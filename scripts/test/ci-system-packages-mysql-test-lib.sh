#!/usr/bin/env bash
# MySQL-purpose stand-ins and assertions for ci-system-packages mysql boundary tests.
# Source after ci-system-packages-test-lib.sh. Not discovered by run_all_script_tests.sh.

MYSQL_COMPAT_DEB_PACKAGES=(libncurses5 libtinfo5)
MYSQL_APT_PACKAGES=(libaio-dev libaio1t64 libnuma1)
MYSQL_COMPAT_DEB_VERSION=6.3-2ubuntu0.3

# Stand-in curl: logs downloads; optional hard failure or one transient failure
# that succeeds under --retry (native acquire retries).
write_fake_curl() {
	local fake_bin=$1
	{
		printf '#!%s\n' "$(command -v bash)"
		cat <<'EOS'
set -euo pipefail
: "${CURL_LOG:?}"
: "${CURL_STATE_DIR:?}"

fail_hard="${CURL_FAIL:-0}"
fail_once="${CURL_FAIL_ONCE:-0}"

echo "curl $*" >>"$CURL_LOG"

out=
retries=0
args=("$@")
i=0
while ((i < ${#args[@]})); do
	case ${args[$i]} in
	--retry)
		i=$((i + 1))
		retries=${args[$i]:-0}
		;;
	-o | --output)
		i=$((i + 1))
		out=${args[$i]:-}
		;;
	esac
	i=$((i + 1))
done

[[ -n $out ]] || {
	echo "curl: missing -o path" >&2
	exit 2
}

attempt_file="$CURL_STATE_DIR/download_attempts"
attempt=$(($(cat "$attempt_file" 2>/dev/null || echo 0) + 1))
echo "$attempt" >"$attempt_file"

if [[ $fail_hard == 1 ]]; then
	echo "curl: download failed (forced)" >&2
	exit 22
fi

if [[ $fail_once == 1 && $attempt -eq 1 ]]; then
	if ((retries >= 1)); then
		echo "curl: transient acquire failure; retrying (retries=$retries)" >>"$CURL_LOG"
		attempt=$((attempt + 1))
		echo "$attempt" >"$attempt_file"
		: >"$out"
		echo "curl: download ok after retry" >>"$CURL_LOG"
		exit 0
	fi
	echo "curl: download failed (no retries)" >&2
	exit 22
fi

: >"$out"
echo "curl: download ok" >>"$CURL_LOG"
exit 0
EOS
	} >"$fake_bin/curl"
	chmod +x "$fake_bin/curl"
}

assert_mysql_packages_in_install() {
	local log=$1
	local pkg deb
	grep -q 'apt-get .*install' "$log" || fail "expected apt-get install in log"
	for pkg in "${MYSQL_APT_PACKAGES[@]}"; do
		grep -q "install .*${pkg}" "$log" || fail "expected MySQL apt package ${pkg} on install line"
	done
	for deb in "${MYSQL_COMPAT_DEB_PACKAGES[@]}"; do
		grep -q "install .*/${deb}.deb" "$log" || fail "expected compatibility deb ${deb}.deb on install line"
	done
}

assert_mysql_compat_downloads() {
	local log=$1
	local package
	for package in "${MYSQL_COMPAT_DEB_PACKAGES[@]}"; do
		grep -q "curl .*${package}_${MYSQL_COMPAT_DEB_VERSION}_amd64.deb" "$log" ||
			fail "expected curl download of ${package} ${MYSQL_COMPAT_DEB_VERSION}"
		grep -q -- "--retry 2" "$log" || fail "expected curl --retry 2 (ACQUIRE_RETRIES)"
	done
}

# Extends setup_work with curl stand-in and RUNNER_TEMP for the mysql purpose.
setup_mysql_work() {
	local work=$1
	setup_work "$work"
	mkdir -p "$work/curl-state" "$work/runner-temp"
	write_fake_curl "$work/bin"
	export CURL_LOG="$work/curl.log"
	export CURL_STATE_DIR="$work/curl-state"
	export RUNNER_TEMP="$work/runner-temp"
	: >"$CURL_LOG"
}

run_mysql_under_fakes() {
	local work=$1
	PATH="$work/bin:$PATH" bash "$SCRIPT" mysql >"$work/out" 2>&1
}
