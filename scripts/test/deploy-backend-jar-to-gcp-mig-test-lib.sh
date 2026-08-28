#!/usr/bin/env bash

fail() {
	echo "FAIL: $*" >&2
	exit 1
}

assert_equals() {
	local expected=$1 actual=$2 msg=${3:-}
	if [[ "$expected" != "$actual" ]]; then
		fail "${msg:+$msg — }expected <$expected> got <$actual>"
	fi
}

assert_file_exists() {
	[[ -f "$1" ]] || fail "expected file missing: $1"
}

assert_not_file_exists() {
	[[ ! -f "$1" ]] || fail "expected file absent: $1"
}

init_deploy_test_logs() {
	local work=$1
	export GSUTIL_LOG="$work/gsutil.log" ROLLING_LOG="$work/rolling.log" GCLOUD_LOG="$work/gcloud.log"
	export HEALTHCHECK_LOG="$work/healthcheck.log" ROLLOUT_LOG="$work/rollout.log" STEPS_LOG="$work/steps.log"
	: >"$GSUTIL_LOG"
	: >"$GCLOUD_LOG"
	: >"$HEALTHCHECK_LOG"
	: >"$ROLLOUT_LOG"
	: >"$STEPS_LOG"
	rm -f "$ROLLING_LOG"
}

write_matching_deploy_record() {
	local record_file=$1 jar=$2
	local hash startup_hash
	hash=$(sha256sum "$jar" | awk '{print $1}')
	startup_hash=$(sha256sum "$REPO_ROOT/infra/gcp/scripts/mig-zulu25-openai-app-instance-startup.sh" | awk '{print $1}')
	printf '{"sha256":"%s","startup_script_sha256":"%s","git_sha":"old","recorded_at":"2020-01-01T00:00:00Z"}\n' "$hash" "$startup_hash" >"$record_file"
}

assert_healthcheck_invoked() {
	local log=$1 msg=${2:-healthcheck not invoked}
	grep -q app-instance-healthcheck.sh "$log" || fail "$msg"
}

assert_healthcheck_not_invoked() {
	local log=$1 msg=${2:-healthcheck invoked when deploy skipped}
	! grep -q app-instance-healthcheck.sh "$log" 2>/dev/null || fail "$msg"
}

assert_rollout_invoked() {
	local log=$1 msg=${2:-rollout wait not invoked}
	grep -q check-mig-rollout.sh "$log" || fail "$msg"
}

assert_rollout_not_invoked() {
	local log=$1 msg=${2:-rollout wait invoked when deploy skipped}
	! grep -q check-mig-rollout.sh "$log" 2>/dev/null || fail "$msg"
}

write_fake_bin() {
	local fake_bin=$1
	mkdir -p "$fake_bin"
	{
		printf '#!%s\n' "$REAL_BASH"
		cat <<'EOS'
set -e
LOG="${GSUTIL_LOG:?}"
cmd="${1:-}"
shift || true
case "$cmd" in
cat)
	echo "cat $*" >>"$LOG"
	if [[ -n "${RECORD_JSON_FILE:-}" && -f "$RECORD_JSON_FILE" ]]; then
		cat "$RECORD_JSON_FILE"
		exit 0
	fi
	exit 1
	;;
cp)
	if [[ "${1:-}" == "-" ]]; then
		echo "cp - $2" >>"$LOG"
		cat >/dev/null
		exit 0
	fi
	echo "cp $*" >>"$LOG"
	exit 0
	;;
*)
	echo "unexpected gsutil: $cmd $*" >>"$LOG"
	exit 1
	;;
esac
EOS
	} >"$fake_bin/gsutil"
	chmod +x "$fake_bin/gsutil"

	{
		printf '#!%s\n' "$REAL_BASH"
		cat <<'EOS'
set -e
LOG="${GCLOUD_LOG:?}"
cmd="${1:-}"
shift || true
case "$cmd" in
compute)
	sub="${1:-}"
	shift || true
	if [[ "$sub" == "url-maps" ]]; then
		echo "gcloud compute url-maps $*" >>"$LOG"
		exit 0
	fi
	echo "unexpected gcloud compute: $sub $*" >>"$LOG"
	exit 1
	;;
*)
	echo "unexpected gcloud: $cmd $*" >>"$LOG"
	exit 1
	;;
esac
EOS
	} >"$fake_bin/gcloud"
	chmod +x "$fake_bin/gcloud"

	{
		printf '#!%s\n' "$REAL_BASH"
		cat <<'EOS'
case "$*" in
*update-mig-startup-script.sh*)
	echo "update-mig-startup-script $*" >>"${ROLLING_LOG:?}"
	echo "update-mig-startup-script" >>"${STEPS_LOG:?}"
	exit 0
	;;
*check-mig-rollout.sh*)
	echo "check-mig-rollout $*" >>"${ROLLOUT_LOG:?}"
	echo "check-mig-rollout" >>"${STEPS_LOG:?}"
	if [[ "${ROLLOUT_SHOULD_FAIL:-}" == "1" ]]; then
		exit 1
	fi
	exit 0
	;;
*app-instance-healthcheck.sh*)
	echo "app-instance-healthcheck $*" >>"${HEALTHCHECK_LOG:?}"
	echo "GITHUB_SHA=${GITHUB_SHA:-}" >>"${HEALTHCHECK_LOG:?}"
	echo "app-instance-healthcheck" >>"${STEPS_LOG:?}"
	if [[ "${HEALTHCHECK_SHOULD_FAIL:-}" == "1" ]]; then
		exit 1
	fi
	exit 0
	;;
esac
EOS
		printf 'exec %q "$@"\n' "$REAL_BASH"
	} >"$fake_bin/bash"
	chmod +x "$fake_bin/bash"
}

run_deploy() {
	(
		cd "$1"
		PATH="$2:$PATH"
		export GCS_BUCKET ARTIFACT VERSION GITHUB_SHA
		export GSUTIL_LOG ROLLING_LOG GCLOUD_LOG HEALTHCHECK_LOG ROLLOUT_LOG STEPS_LOG
		export REPO_ROOT="$REPO_ROOT"
		export DEPLOY_JAR_PATH="${DEPLOY_JAR_PATH:-}"
		export RECORD_JSON_FILE="${RECORD_JSON_FILE:-}"
		export FORCE_FULL_DEPLOY="${FORCE_FULL_DEPLOY:-}"
		export HEALTHCHECK_SHOULD_FAIL="${HEALTHCHECK_SHOULD_FAIL:-}"
		export ROLLOUT_SHOULD_FAIL="${ROLLOUT_SHOULD_FAIL:-}"
		bash "$DEPLOY_SCRIPT"
	)
}
