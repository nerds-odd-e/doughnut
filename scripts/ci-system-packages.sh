#!/usr/bin/env bash
# Bound CI system-package phases.
# Usage: scripts/ci-system-packages.sh <purpose>
#
# Purposes:
#   canvas — APT update + canvas build packages
#   mysql  — compatibility .deb downloads + MySQL system-library APT install
#
# Env overrides (fixtures):
#   CI_SYSTEM_PACKAGES_BUDGET_SECONDS     default 600
#   CI_SYSTEM_PACKAGES_KILL_AFTER_SECONDS default 10
#   CI_SYSTEM_PACKAGES_ACQUIRE_RETRIES    default 2
set -euo pipefail

BUDGET_SECONDS="${CI_SYSTEM_PACKAGES_BUDGET_SECONDS:-600}"
KILL_AFTER_SECONDS="${CI_SYSTEM_PACKAGES_KILL_AFTER_SECONDS:-10}"
ACQUIRE_RETRIES="${CI_SYSTEM_PACKAGES_ACQUIRE_RETRIES:-2}"

CANVAS_PACKAGES=(
	libcairo2-dev
	libpango1.0-dev
	libjpeg-dev
	libgif-dev
	librsvg2-dev
)

MYSQL_COMPAT_DEB_VERSION=6.3-2ubuntu0.3
MYSQL_COMPAT_DEB_PACKAGES=(libncurses5 libtinfo5)
MYSQL_APT_PACKAGES=(libaio-dev libaio1t64 libnuma1)

apt_option_string() {
	printf '%s ' \
		-o "Acquire::Retries=${ACQUIRE_RETRIES}" \
		-o "Acquire::http::Timeout=30" \
		-o "Acquire::https::Timeout=30" \
		-o "APT::Update::Error-Mode=any"
}

apt_update_install_body() {
	local packages=$1
	local opts
	opts=$(apt_option_string)
	printf 'apt-get %supdate && apt-get %sinstall --yes --no-install-recommends %s' \
		"$opts" "$opts" "$packages"
}

apt_install_body() {
	local packages=$1
	local opts
	opts=$(apt_option_string)
	printf 'apt-get %sinstall --yes --no-install-recommends %s' \
		"$opts" "$packages"
}

curl_acquire_options() {
	printf '%s ' \
		--fail \
		--location \
		--retry "${ACQUIRE_RETRIES}" \
		--retry-all-errors \
		--retry-delay 0 \
		--connect-timeout 5 \
		--max-time 30
}

# Live parent reports phase/budget and retains the child's exit status (ADR 0006).
# GNU timeout owns the strict Bash child; privileged work runs inside that child.
run_bounded_phase() {
	local phase=$1
	local script=$2
	local start end elapsed status

	start=$(date +%s)
	set +e
	timeout --kill-after="${KILL_AFTER_SECONDS}s" "${BUDGET_SECONDS}s" \
		bash -euo pipefail -c "$script"
	status=$?
	set -e
	end=$(date +%s)
	elapsed=$((end - start))

	if ((status != 0)); then
		echo "ci-system-packages: phase=${phase} budget=${BUDGET_SECONDS}s kill-after=${KILL_AFTER_SECONDS}s acquire-retries=${ACQUIRE_RETRIES} elapsed=${elapsed}s status=${status}" >&2
		return "$status"
	fi
	echo "ci-system-packages: phase=${phase} completed elapsed=${elapsed}s budget=${BUDGET_SECONDS}s"
	return 0
}

sudo_apt_script() {
	local body=$1
	printf 'sudo -n env DEBIAN_FRONTEND=noninteractive bash -euo pipefail -c %q' "$body"
}

purpose_canvas() {
	local packages
	packages=$(printf '%s ' "${CANVAS_PACKAGES[@]}")
	run_bounded_phase canvas "$(sudo_apt_script "$(apt_update_install_body "$packages")")"
}

# Compatibility downloads and APT install share one phase deadline/retry policy.
purpose_mysql() {
	local dest=${RUNNER_TEMP:?RUNNER_TEMP is required for mysql package phase}
	local package packages debs downloads curl_opts

	packages=$(printf '%s ' "${MYSQL_APT_PACKAGES[@]}")
	debs=
	for package in "${MYSQL_COMPAT_DEB_PACKAGES[@]}"; do
		debs+="${dest}/${package}.deb "
	done

	curl_opts=$(curl_acquire_options)
	downloads=
	for package in "${MYSQL_COMPAT_DEB_PACKAGES[@]}"; do
		downloads+="curl ${curl_opts}\"https://security.ubuntu.com/ubuntu/pool/universe/n/ncurses/${package}_${MYSQL_COMPAT_DEB_VERSION}_amd64.deb\" -o \"${dest}/${package}.deb\" && "
	done

	run_bounded_phase mysql \
		"${downloads}$(sudo_apt_script "$(apt_install_body "${packages}${debs}")")"
}

main() {
	local purpose=${1:-}
	if [[ -z $purpose ]]; then
		echo "usage: $0 <canvas|mysql>" >&2
		exit 2
	fi
	shift || true

	case $purpose in
	canvas)
		purpose_canvas "$@"
		;;
	mysql)
		purpose_mysql "$@"
		;;
	*)
		echo "ci-system-packages: unknown purpose: $purpose" >&2
		exit 2
		;;
	esac
}

main "$@"
