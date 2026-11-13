# Slice 1 probe — evidence handoff

**Identity:** SEED-129#bound-ci-system-package-installation
**Slice:** 1. Validate bounded package failure on the actual Linux runner
**Checkout:** `/Users/terryyin/git/doughnut/.worktrees/bound-ci-system-package-installation`
**Branch:** `cursor/bound-ci-system-package-installation`

## Authored artifacts (uncommitted until coordinator push)

| Path | Role |
| --- | --- |
| `scripts/ci-system-packages.sh` | Shared 600s / 10s kill-after / 2 acquire-retries policy; canvas purpose; entry for probes |
| `scripts/ci-system-packages-probe.sh` | Disposable probe purposes sourced by the entry (delete with the probe workflow) |
| `.github/workflows/ci-system-packages-probe.yml` | Branch-scoped disposable probe on `${{ vars.RUNNER \|\| 'ubuntu-latest' }}` |

## Literal commands (also logged by `probe-env` on the runner)

```sh
timeout --kill-after=10s 600s bash -euo pipefail -c 'sudo -n env DEBIAN_FRONTEND=noninteractive bash -euo pipefail -c …'
apt-get -o Acquire::Retries=2 -o Acquire::http::Timeout=30 \
  -o Acquire::https::Timeout=30 -o APT::Update::Error-Mode=any …
```

Stalled case: `CI_SYSTEM_PACKAGES_BUDGET_SECONDS=15` (kill-after remains 10s); child `trap "" TERM` then `sudo -n sleep 1000`.

Bad mirror: wipe runner sources/lists; sole source `deb http://archive.ubuntu.invalid/ubuntu noble main` (real APT; do not stub apt-get).

## Jobs to read after push

Workflow name: `CI system packages probe`

| Job | Expectation |
| --- | --- |
| `probe bad-mirror` | `Bad-mirror package phase` → `failure` within ≤610s; `Downstream checks` → `skipped` |
| `probe stalled-transfer` | Step log shows nonzero timeout/kill status; step itself passes after asserting failure; wall time ≈ budget + kill-after |
| `probe transient-recovery` | `success`; log `attempt_count=2` (or ≥2 with curl retries) |
| `probe healthy-package` | `success`; `elapsed=` well under 600s (headroom check) |

## Observation commands

```sh
# Competing runs before launch
CURSOR_DEV=true nix develop -c gh run list --repo nerds-odd-e/doughnut --limit 10

# After push: find the probe run
CURSOR_DEV=true nix develop -c gh run list --repo nerds-odd-e/doughnut \
  --workflow ci-system-packages-probe.yml --branch cursor/bound-ci-system-package-installation --limit 5

# Terminal job/step outcomes
CURSOR_DEV=true nix develop -c gh api \
  repos/nerds-odd-e/doughnut/actions/runs/<RUN_ID>/jobs --paginate \
  --jq '.jobs[] | {name, conclusion, started_at, completed_at, steps: [.steps[] | {name, conclusion, number, started_at, completed_at}]}'
```

Record from `probe-env` step logs: `os_release_*`, `apt-get --version`, `timeout --version`, `nproc`, `mem_total_kb`.

## Expensive-proof decision (retained)

- Host: GitHub-hosted `ubuntu-latest` (repo `RUNNER`; confirmed `ubuntu-latest`)
- Workload: one serial disposable probe workflow
- Pre-launch note (2026-11-13T10:33Z): one unrelated `queued` donut CI run on
  `codex/keep-answered-questions-with-their-tracker` (`43409981854`). GitHub-hosted
  pool; no self-hosted inventory. Coordinator may wait for that queue to clear if
  runner contention appears, then push this probe alone.
- Choice: **run** after coordinator commit/push of the probe files
- Reconsider when: unsupported APT options, incomplete cleanup, unavailable credentials/workflow, healthy setup outside budget, or clear host contention

## Local checks already run (not Linux APT / job-skip proof)

- `CURSOR_DEV=true nix develop -c bash -n scripts/ci-system-packages.sh scripts/ci-system-packages-probe.sh` ok
- `CI_SYSTEM_PACKAGES_BUDGET_SECONDS=5 CI_SYSTEM_PACKAGES_KILL_AFTER_SECONDS=2` `probe-stalled-transfer` → exit 137 in ~7s (Darwin/nix)
- `probe-transient-recovery` → success, `attempt_count=2` (Darwin/nix)

## Hosted probe receipts (run 43410926122 on SHA 510f94debc5f44bb6f072ed70f6049451bf413e8)

Runner: Ubuntu 24.04.5 LTS (noble), apt 2.8.3, GNU timeout 9.4, nproc=4, mem_total_kb=16367524.
Literal APT options and timeout invocation matched the production defaults in `probe-env`.

| Job | Result |
| --- | --- |
| probe bad-mirror | Package step `failure` in 3s (`status=100`); retained `Could not resolve 'archive.ubuntu.invalid'`; phase/budget line emitted; Downstream checks `skipped`. Job conclusion `failure` is expected. |
| probe stalled-transfer | Child ignored TERM; `elapsed=25s` `status=137` under 15s budget + 10s kill-after; step asserted nonzero and passed. |
| probe transient-recovery | `success`; `attempt_count=2`; phase elapsed 1s. |
| probe healthy-package | `success`; phase `elapsed=11s` under 600s budget (headroom holds). |

Approach usable: APT `Error-Mode=any`, timeout/kill cleanup, real package install, and job skipping worked together on ubuntu-latest. Dependent slices 2–3 may proceed.

Main `ci.yml` for the same SHA remains under the attached observer (`await-revision`); mark slice 1 `done` only after that progress observation settles.
