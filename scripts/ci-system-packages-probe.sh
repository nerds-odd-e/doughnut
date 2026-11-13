#!/usr/bin/env bash
# Disposable Linux probe purposes for SEED-129 slice 1 (ADR 0007).
# Sourced by scripts/ci-system-packages.sh after shared policy helpers exist.
# Delete with the temporary probe workflow once slices 2–3 own the proof.

purpose_probe_env() {
	echo "=== ci-system-packages probe environment ==="
	echo "date_utc=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
	echo "uname=$(uname -a)"
	if [[ -r /etc/os-release ]]; then
		# shellcheck disable=SC1091
		. /etc/os-release
		echo "os_release_pretty=${PRETTY_NAME:-}"
		echo "os_release_version=${VERSION_ID:-}"
		echo "os_release_version_codename=${VERSION_CODENAME:-}"
	fi
	if [[ -r /etc/image_version ]]; then
		echo "image_version_file=$(tr '\n' ' ' </etc/image_version)"
	fi
	if command -v apt-get >/dev/null; then
		apt-get --version | head -1
	fi
	if command -v apt >/dev/null; then
		apt --version | head -1
	fi
	timeout --version | head -1
	bash --version | head -1
	echo "budget_seconds=${BUDGET_SECONDS}"
	echo "kill_after_seconds=${KILL_AFTER_SECONDS}"
	echo "acquire_retries=${ACQUIRE_RETRIES}"
	echo "nproc=$(nproc 2>/dev/null || echo unknown)"
	if [[ -r /proc/meminfo ]]; then
		awk '/MemTotal:/ {print "mem_total_kb="$2}' /proc/meminfo
	fi
	echo "literal_timeout_invocation: timeout --kill-after=${KILL_AFTER_SECONDS}s ${BUDGET_SECONDS}s bash -euo pipefail -c 'sudo -n env DEBIAN_FRONTEND=noninteractive bash -euo pipefail -c …'"
	echo "literal_apt_options: $(apt_option_string)"
}

# Replace runner APT sources with a single .invalid mirror so real apt-get must
# consume it. Disposable runner only (ADR 0007).
purpose_probe_bad_mirror() {
	sudo -n bash -euo pipefail -c '
		if [[ -e /etc/apt/sources.list ]]; then
			rm -f /etc/apt/sources.list
		fi
		if [[ -d /etc/apt/sources.list.d ]]; then
			rm -rf /etc/apt/sources.list.d
		fi
		mkdir -p /etc/apt/sources.list.d
		# Empty indexes: wipe lists; only an unresolvable .invalid mirror remains.
		printf "%s\n" "deb http://archive.ubuntu.invalid/ubuntu noble main" >/etc/apt/sources.list
		: >/etc/apt/sources.list.d/empty-probe.list
		rm -rf /var/lib/apt/lists/*
	'

	echo "ci-system-packages: bad-mirror sources.list:"
	sudo -n cat /etc/apt/sources.list

	set +e
	run_bounded_phase bad-mirror "$(sudo_apt_script "$(apt_update_install_body hello)")"
	local status=$?
	set -e
	return "$status"
}

# Short test budget against a hanging privileged child that ignores TERM.
purpose_probe_stalled_transfer() {
	BUDGET_SECONDS="${CI_SYSTEM_PACKAGES_BUDGET_SECONDS:-15}"
	echo "ci-system-packages: stalled-transfer budget=${BUDGET_SECONDS}s kill-after=${KILL_AFTER_SECONDS}s"

	# Timed bash ignores TERM so only --kill-after reaps it (stuck-acquisition stand-in).
	# Privileged APT cleanup is covered by bad-mirror / healthy-package on the runner.
	set +e
	run_bounded_phase stalled-transfer \
		'trap "" TERM; echo "ci-system-packages: stalled child pid=$$ ignoring TERM"; sleep 1000'
	local status=$?
	set -e
	echo "ci-system-packages: stalled-transfer finished status=${status} (expect timeout/kill nonzero)"
	return "$status"
}

# Fail-once HTTP acquisition, then succeed within curl retry allowance.
purpose_probe_transient_recovery() {
	local work port pid attempt_log status
	work=$(mktemp -d)
	attempt_log="$work/attempts"
	: >"$attempt_log"
	port=$(python3 - <<'PY'
import socket
s = socket.socket()
s.bind(("127.0.0.1", 0))
print(s.getsockname()[1])
s.close()
PY
	)

	python3 - "$port" "$work" "$attempt_log" <<'PY' &
import http.server
import sys
from pathlib import Path

port = int(sys.argv[1])
work = Path(sys.argv[2])
attempt_log = Path(sys.argv[3])
payload = b"transient-recovery-ok\n"
(work / "ok.bin").write_bytes(payload)

class Handler(http.server.BaseHTTPRequestHandler):
    def do_GET(self):
        count = int(attempt_log.read_text() or "0") + 1
        attempt_log.write_text(str(count))
        if count == 1:
            self.send_response(503)
            self.end_headers()
            self.wfile.write(b"transient\n")
            return
        self.send_response(200)
        self.send_header("Content-Length", str(len(payload)))
        self.end_headers()
        self.wfile.write(payload)

    def log_message(self, fmt, *args):
        return

http.server.HTTPServer(("127.0.0.1", port), Handler).serve_forever()
PY
	pid=$!
	# Expand paths now: EXIT runs after function locals are gone under set -u.
	trap 'kill "'"$pid"'" 2>/dev/null || true; rm -rf "'"$work"'"' EXIT

	sleep 0.2

	set +e
	run_bounded_phase transient-recovery \
		"curl --fail --location --retry ${ACQUIRE_RETRIES} --retry-all-errors --retry-delay 0 --connect-timeout 5 --max-time 30 \
			http://127.0.0.1:${port}/ok.bin -o ${work}/downloaded.bin \
			&& cmp -s ${work}/ok.bin ${work}/downloaded.bin \
			&& echo ci-system-packages: transient-recovery downloaded after retries"
	status=$?
	set -e

	echo "ci-system-packages: transient-recovery attempt_count=$(cat "$attempt_log")"
	kill "$pid" 2>/dev/null || true
	trap - EXIT
	rm -rf "$work"
	return "$status"
}

purpose_probe_healthy_package() {
	run_bounded_phase healthy-package \
		"$(sudo_apt_script "$(apt_update_install_body hello) && hello")"
}
