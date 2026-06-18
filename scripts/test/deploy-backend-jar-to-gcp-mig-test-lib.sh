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
		export GSUTIL_LOG ROLLING_LOG GCLOUD_LOG
		export REPO_ROOT="$REPO_ROOT"
		export DEPLOY_JAR_PATH="${DEPLOY_JAR_PATH:-}"
		export RECORD_JSON_FILE="${RECORD_JSON_FILE:-}"
		export FORCE_FULL_DEPLOY="${FORCE_FULL_DEPLOY:-}"
		bash "$DEPLOY_SCRIPT"
	)
}
