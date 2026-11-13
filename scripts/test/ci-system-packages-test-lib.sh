#!/usr/bin/env bash
# Shared stand-ins and assertions for scripts/test/ci-system-packages.sh.test.
# Not discovered by run_all_script_tests.sh (only *.test).

CANVAS_PACKAGES=(
	libcairo2-dev
	libpango1.0-dev
	libjpeg-dev
	libgif-dev
	librsvg2-dev
)

fail() {
	echo "FAIL: $*" >&2
	exit 1
}

require_timeout() {
	command -v timeout >/dev/null || fail "GNU timeout required (run under nix develop)"
}

write_fake_sudo() {
	local fake_bin=$1
	mkdir -p "$fake_bin"
	{
		printf '#!%s\n' "$(command -v bash)"
		cat <<'EOS'
set -euo pipefail
# Production invokes: sudo -n env DEBIAN_FRONTEND=noninteractive bash … -c '…'
if [[ ${1:-} == -n ]]; then
	shift
fi
exec "$@"
EOS
	} >"$fake_bin/sudo"
	chmod +x "$fake_bin/sudo"
}

# Stand-in apt-get: honors Acquire::Retries for one simulated transient install
# failure, optional delay, and optional past-budget hang. Logs every call.
write_fake_apt_get() {
	local fake_bin=$1
	{
		printf '#!%s\n' "$(command -v bash)"
		cat <<'EOS'
set -euo pipefail
: "${APT_GET_LOG:?}"
: "${APT_STATE_DIR:?}"

retries=0
delay_seconds="${APT_GET_DELAY_SECONDS:-0}"
hang="${APT_GET_HANG:-0}"
fail_once="${APT_GET_FAIL_ONCE:-0}"

args=("$@")
echo "apt-get $*" >>"$APT_GET_LOG"

subcommand=
for arg in "${args[@]}"; do
	case $arg in
	Acquire::Retries=*)
		retries=${arg#Acquire::Retries=}
		;;
	update | install | upgrade | dist-upgrade)
		subcommand=$arg
		;;
	esac
done

if [[ $hang == 1 ]]; then
	# Sleep past the fixture budget; responds to TERM so the real timeout
	# reaps cleanly. TERM-ignoring cleanup remains slice-1 Linux probe proof.
	echo "apt-get: hanging past budget pid=$$" >&2
	sleep 1000
	exit 1
fi

if ((delay_seconds > 0)); then
	sleep "$delay_seconds"
fi

case $subcommand in
update)
	echo "apt-get: update ok" >>"$APT_GET_LOG"
	exit 0
	;;
install)
	attempt_file="$APT_STATE_DIR/install_attempts"
	attempt=$(($(cat "$attempt_file" 2>/dev/null || echo 0) + 1))
	echo "$attempt" >"$attempt_file"
	if [[ $fail_once == 1 && $attempt -eq 1 ]]; then
		# Simulate APT acquisition retry inside one install invocation.
		if ((retries >= 1)); then
			echo "apt-get: transient acquire failure; retrying (retries=$retries)" >>"$APT_GET_LOG"
			attempt=$((attempt + 1))
			echo "$attempt" >"$attempt_file"
			echo "apt-get: install ok after retry" >>"$APT_GET_LOG"
			exit 0
		fi
		echo "apt-get: install failed (no retries)" >&2
		exit 1
	fi
	echo "apt-get: install ok" >>"$APT_GET_LOG"
	exit 0
	;;
*)
	echo "apt-get: unexpected subcommand in: $*" >&2
	exit 2
	;;
esac
EOS
	} >"$fake_bin/apt-get"
	chmod +x "$fake_bin/apt-get"
}

assert_canvas_packages_in_install() {
	local log=$1
	local pkg
	grep -q 'apt-get .*update' "$log" || fail "expected apt-get update in log"
	grep -q 'apt-get .*install' "$log" || fail "expected apt-get install in log"
	for pkg in "${CANVAS_PACKAGES[@]}"; do
		grep -q "install .*${pkg}" "$log" || fail "expected canvas package ${pkg} on install line"
	done
}

# Populate fake_bin under $1; export APT_GET_LOG / APT_STATE_DIR for stand-ins.
# Callers must not capture this via command substitution (exports would be lost).
setup_work() {
	local work=$1
	local fake_bin="$work/bin"
	mkdir -p "$work/state" "$fake_bin"
	write_fake_sudo "$fake_bin"
	write_fake_apt_get "$fake_bin"
	export APT_GET_LOG="$work/apt-get.log"
	export APT_STATE_DIR="$work/state"
	: >"$APT_GET_LOG"
}

# Shared deadline claims: phase/budget diagnostic and wall time near the real budget
# (not an outer harness). Tight upper bound makes a bypassed timeout fail this check.
deadline_assertions_hold() {
	local status=$1 out=$2 elapsed=$3
	((status != 0)) || return 1
	[[ $out == *"phase=canvas"* ]] || return 1
	[[ $out == *"budget=3s"* ]] || return 1
	((elapsed >= 3)) || return 1
	# budget + kill-after + 1s slack; outer harness of 8s must miss this.
	((elapsed <= 6)) || return 1
	return 0
}

run_canvas_under_fakes() {
	local work=$1
	PATH="$work/bin:$PATH" bash "$SCRIPT" canvas >"$work/out" 2>&1
}
