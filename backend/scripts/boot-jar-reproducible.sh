#!/usr/bin/env bash
#
# Phase 1: two clean bootJar runs from the same tree must yield the same SHA-256.
# Real Gradle + pnpm (not Bach). See ongoing/conditional-deploy-gcs-frontend.md.
#
# Local (full cost):  CURSOR_DEV=true nix develop -c bash backend/scripts/boot-jar-reproducible.sh
# CI (after bundle+build): BOOT_JAR_REPRO_SKIP_BUNDLE=1 bash backend/scripts/boot-jar-reproducible.sh
#
# CLI bundle inlines env; pin these so two runs match (see plan doc).

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$REPO_ROOT"

JAR="backend/build/libs/doughnut-0.0.1-SNAPSHOT.jar"

export GOOGLE_CLIENT_ID="${GOOGLE_CLIENT_ID:-repro-test-google-client-id}"
export GOOGLE_CLIENT_SECRET="${GOOGLE_CLIENT_SECRET:-repro-test-google-client-secret}"
export CLI_VERSION="${CLI_VERSION:-repro-test-cli-version}"

fail() {
	echo "boot-jar-reproducible.sh: $*" >&2
	echo "  Phase 1 context: ongoing/conditional-deploy-gcs-frontend.md" >&2
	exit 1
}

hash_jar() {
	[[ -f "$JAR" ]] || fail "expected fat jar at $JAR after bootJar (check Gradle errors above)"
	sha256sum "$JAR" | awk '{print $1}'
}

run_boot_jar() {
	# --no-build-cache: each run must recompile so ZIP entry times reflect real mtimes; with
	# cache, FROM-CACHE outputs can hide non-reproducible bootJar packaging (Phase 1).
	backend/gradlew -p backend clean bootJar -x test \
		-Dspring.profiles.active=prod \
		--no-build-cache \
		--parallel
}

# Without preserveFileTimestamps=false, ZIP entry times often differ if builds are seconds apart.
sleep_between_rebuilds() {
	sleep 2
}

if ! command -v sha256sum >/dev/null; then
	fail "sha256sum not in PATH (install GNU coreutils, e.g. CURSOR_DEV=true nix develop)"
fi

# Phase 1 Gradle settings (commit 4a75bd169169965fc81933cf0a28c96ecdddad67). A plain double
# bootJar can still match byte-for-byte on some JDK/Gradle pairs without these lines, so we
# assert the configuration stays in the tree.
assert_phase1_gradle_reproducibility_config() {
	local f="backend/build.gradle"
	[[ -f "$f" ]] || fail "missing $f (run from repo root)"
	grep -qF "import org.gradle.api.tasks.bundling.AbstractArchiveTask" "$f" \
		|| fail "$f must import AbstractArchiveTask — needed to apply archive settings to bootJar"
	grep -qF "options.encoding = 'UTF-8'" "$f" \
		|| fail "$f must set JavaCompile options.encoding = 'UTF-8' — avoids locale-dependent class files"
	grep -qF "preserveFileTimestamps = false" "$f" \
		|| fail "$f must set tasks.withType(AbstractArchiveTask) { preserveFileTimestamps = false } — otherwise ZIP entry times leak into the jar and hashes can differ run-to-run"
}

assert_phase1_gradle_reproducibility_config

if [[ "${BOOT_JAR_REPRO_SKIP_BUNDLE:-}" == "1" ]]; then
	[[ -f "$JAR" ]] || fail "BOOT_JAR_REPRO_SKIP_BUNDLE=1 but no jar yet — run pnpm bundle:all then backend/gradlew -p backend build -x test -Dspring.profiles.active=prod (same as CI Backend-unit-tests)"
	run_boot_jar
	h1=$(hash_jar)
	sleep_between_rebuilds
	run_boot_jar
	h2=$(hash_jar)
else
	command -v pnpm >/dev/null || fail "pnpm not in PATH — full mode runs bundle:all twice; use nix develop or install Node tooling"
	[[ -x backend/gradlew ]] || fail "backend/gradlew missing or not executable"

	pnpm bundle:all
	run_boot_jar
	h1=$(hash_jar)

	sleep_between_rebuilds

	pnpm bundle:all
	run_boot_jar
	h2=$(hash_jar)
fi

if [[ "$h1" != "$h2" ]]; then
	fail "two clean bootJar runs produced different SHA-256 ($h1 vs $h2). Same sources and same embedded frontend/CLI bytes should yield identical jars. Check backend/build.gradle: preserveFileTimestamps=false on AbstractArchiveTask, JavaCompile UTF-8. If this script ran pnpm bundle:all (full mode), keep GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET, and CLI_VERSION fixed across both runs — they are baked into the CLI bundle."
fi

echo "ok boot jar reproducible sha256=$h1"
