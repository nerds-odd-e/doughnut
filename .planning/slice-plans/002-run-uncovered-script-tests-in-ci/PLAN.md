# See CI results and timings for the remaining script tests in Intent

**Identity:** SEED-111#run-uncovered-script-tests-in-ci
**Source:** [Refined story](../../seeds/SEED-111-run-uncovered-script-tests-in-ci.md#run-uncovered-script-tests-in-ci)
**Source review:** [Source-only commitments](source-review.md)

## Goal and scope

Every `donut CI` attempt runs the story's 18 Node test files on
`ubuntu-latest`, fails when one of their tests fails, and publishes each
file's test outcomes and cumulative recorded time under the `scripts` entry
of the attempt's CI proof summary. Intent's Scripts and Infrastructure group
shows the same figures for that attempt. Before the retirement tests become
required, the full group runs repeatedly on the CI runner, and recurring
failures are resolved first.

The existing CI proof counting rules apply. Scope excludes the 13
`scripts/ci/application-release-*.test.mjs` files, Node tests outside `scripts/`,
CLI CI proof, changes to the Cloud VM setup, and removing or merging any of the
18 files. No test is skipped or left out on CI to make an attempt pass. A file
that cannot run on the runner without changing what it checks goes to the story
owner.

## Current decisions

- **Story Branch Mode.** Increments publish to the story branch, and
  `donut CI` runs on every branch push. The files become required on `main`
  only when the story lands, after the slice 1 gate holds. Temporary probe
  changes are committed and reverted on the story branch.
- **Placement.** Run the files from `pnpm lint:all` in the Lint job. That job
  has a JDK (the JVM fixtures compile with `javac`) and already publishes one
  `scripts` part per Node test command in `lint:all` order, which
  `scripts/ci/ci-workflow.test.mjs` enforces. Add each file to the existing
  command whose files share its concern, for example the
  `isolated-cypress*` and `worktree-reset-isolation-harness*` files to
  `test:browser-worktree-isolation` and `dev-healthcheck` to
  `test:development-stack`. Add a capability-named command only where no
  command shares the concern, as for the five `worktree-retirement*` files and
  the profiling receiver check. The command names appear in
  `package.json`, the Lint CI action and `docs/ci-proof-summary.md`.
- **MySQL 8.4 binaries on CI.** `startDisposableMysql` needs local MySQL 8.4
  server binaries (`MYSQL_BASEDIR`, or the directory above `mysqld` on
  `PATH`), and the e2e mutation test checks the version. CI's MySQL
  (`.github/mysql_db`) is a shared container, not local binaries. Add one
  small CI install for the Lint job that unpacks the official 8.4 minimal
  Linux tarball (61 MB, against 908 MB for the full one the Cloud VM uses)
  with the `libaio` packages and symlink the Cloud VM setup already needs on
  Ubuntu 24.04, and sets `MYSQL_BASEDIR`. PFE: the Cloud VM's install is part
  of a sourced, idempotent bootstrap that also starts and configures the
  development and test server on a persistent machine; CI needs only binaries
  on a throwaway runner, and the Cloud VM path cannot be run here. Leave the
  Cloud VM setup unchanged.
- **CI duration.** The slowest E2E shard sets the attempt's duration (about
  7.5 minutes on 2026-11-08, run 42498952903, with the Lint job at about 5.3).
  Record the Lint job's duration after slice 2 and keep it under the slowest
  E2E shard where the measured cost allows.
- **Gate rule (from the story).** A series is at least 30 full-group
  repetitions (all 18 files in one `node --test` invocation, as in the
  story's evidence), spread over at least 3 fresh runners, each after the Lint
  job's setup and `pnpm lint:all`. Any failure of `verified idle fixture
  reports idle snapshot not a reservation` counts as recurring. Another
  test's failure counts as recurring when it fails in two repetitions or its
  diagnosed cause can repeat, as a race can. A recurring failure gets a
  resolution slice before slice 2, then a new series. Thirty clean
  repetitions bound the failure rate near 10 percent at 95 percent
  confidence; a rarer failure still fails a `main` attempt and is handled as
  an ordinary CI failure.
- **Retirement check safety.** The retirement check refuses on ambiguous
  process or database evidence to protect databases. If a resolution would
  change what it refuses, stop for the story owner's decision. A fix in test
  isolation or a test fixture needs no decision.

## Outside-in proof

| Promise | Slice | Observation |
| --- | --- | --- |
| The full group runs repeatedly on the CI runner, recorded per attempt, and recurring failures are resolved before the tests are required | 1 | The plan records the series' run, attempts, runners and every repetition's outcome; no recurring failure remains |
| The runner provides what the files need | 1, 2 | The probe and the slice 2 attempt pass the MySQL 8.4 mutation tests and the JVM process tests on `ubuntu-latest` |
| Every attempt runs all 18 files, and a failing test fails the attempt | 2 | The story-branch attempt's Lint job runs them inside `pnpm lint:all`, whose `&&` chain fails the job on a failing command |
| Each file's outcomes and cumulative recorded time are published, with no test skipped | 2 | The attempt's `ci-proof-summary-attempt-<n>` lists all 18 files under `scripts` with every part present, per-file check counts equal to the local report below, and no `other` outcome |
| Failed tests count with their recorded durations | 2 | Reused: SEED-109's accepted `scripts/ci/ci-proof-outcomes-node.test.mjs`, since the new files pass through the same wrapper and reporter |
| Intent shows the same figures | 3 | The dashboard's figures for the selected attempt equal the artifact for the group and each of the 18 files |

## Decisive premises

| Premise and consumer | Observation and result |
| --- | --- |
| The 18 files pass together through the CI wrapper, and its Node report attributes checks to all 18 (slice 2) | 2026-11-09 at `3dce9d850f`, this macOS host in the Nix shell: `CURSOR_DEV=true nix --extra-experimental-features 'nix-command flakes' develop -c env npm_lifecycle_event=probe-18 CI_PROOF_NODE_REPORT_DIR=<dir> node scripts/ci/node-test.mjs <18 files>` exited 0 with 75 tests passed. `probe-18.ndjson` held 73 passed checks: `dev-healthcheck` 4, `isolated-cypress-openai-mock-cancel` 2, `-failure` 1, `-ownership` 2, `isolated-cypress-openai-mock` 7, `isolated-cypress-owning-health` 2, `isolated-openai-mock` 2, `isolated-service-mocks` 4, `local-runtime-target` 3, `profiling/verify-publication-receiver` 4, `sut-owner-application-group` 2, `worktree-reset-isolation-harness-openai-mock` 1, `-wikidata-mock` 2, `worktree-retirement-checkout-processes` 9, `-e2e-mutation` 3, `-evidence` 11, `-mutation` 6, `worktree-retirement` 8. Run duration 35.6 s on a busy host. This is a proxy for the Linux runner; `ps`, `lsof`, `javac` and MySQL binaries there stay unverified until slice 1. |
| The idle retirement test can fail in the full group (slice 1) | Retained in the story's evidence: on 2026-11-09 at `afe8472ca0`, one of two local full-group runs failed it when the check could not determine a JVM's working directory. Its behavior on the runner is unobserved; slice 1 observes it. |
| `ubuntu-latest` lacks MySQL 8.4 binaries where the helper looks (slices 1 and 2) | `gh variable list` on 2026-11-09: `RUNNER` is `ubuntu-latest`. The helper resolves `MYSQL_BASEDIR` or `$(dirname $(dirname $(which mysqld)))` and runs `bin/mysqld`; the only MySQL in CI is the `.github/mysql_db` container. The runner image's contents are unobserved; slice 1 installs and records them. |
| The 8.4 minimal tarball runs the disposable server on the runner (slices 1 and 2) | Download sizes from `curl -sIL` on 2026-11-09: `mysql-8.4.3-linux-glibc2.17-x86_64-minimal.tar.xz` 61,378,984 bytes; `mysql-8.4.3-linux-glibc2.28-x86_64.tar.xz` 908,299,408 bytes. Whether the minimal build initializes and serves the helper on `ubuntu-latest` is unobserved; slice 1 is its early probe, and its failure changes the install before slice 2. |
| A new `lint:all` Node test command without its fragment step fails the workflow check (slice 2 proof) | 2026-11-09 at `3dce9d850f`: a scratch `test:worktree-retirement` command added to `package.json` and `lint:all`, then `CURSOR_DEV=true nix --extra-experimental-features 'nix-command flakes' develop -c node --test scripts/ci/ci-workflow.test.mjs` failed "each Node test command in lint:all reports its own scripts part after the Bash part" (4 passed, 1 failed). Reverted. `pnpm test:ci-proof` runs the same file through `node-test.mjs`, which adds reporters only when asked. |
| Intent reads a selected attempt's summary (slice 3) | 2026-11-09, the local dashboard (Open Dough 0.3.92, deployment `e8bbd6b9fdf8`): `curl -H 'Origin: http://127.0.0.1:4173' -H 'Sec-Fetch-Site: same-origin' 'http://127.0.0.1:4173/__authenticated-read?source=doughnut&intent=ci-proof&run=42498952903&attempt=1&revision=afe8472ca00f815ac73cc01bc203b6a245293435'` returned 200 with that attempt's summary; its `scripts` entry had 390 passed, 0 failed, 90,795.72 ms, its parts and a per-file breakdown. Without the two headers the route answers 403; without `attempt` it answers 400. Plan 085 slice 8 compared the same route with the artifact on 2026-11-08. |

## Slices

### 1. The full group passes repeatedly on the CI runner

Type: Behavior
Status: done
Proof: The plan records the series: run IDs, attempts, runners, each
repetition's passed and failed counts with failing tests and errors, the
group's time per repetition, the MySQL install time, and the runner's
`mysqld --version`, `lsof`, `ps` and `javac` paths. The series meets the gate
rule above.

Behavior: With SEED-114's cleanup in place, a temporary probe job on the story branch
runs on `ubuntu-latest` with the Lint job's setup, `pnpm lint:all` and the
new MySQL 8.4 install, then repeats the full group until the series is
complete. This is the early probe for the runner: if the install or a file
cannot work there, slices 2 and 3 stop and the plan changes. A recurring
failure under the gate rule adds a resolution slice before slice 2 and a new
series. Once the series is recorded, revert the probe job on the story
branch and keep the MySQL install for slice 2.

### 2. Every CI attempt runs the 18 files and publishes their proof

Type: Behavior
Status: planned
Proof: `pnpm test:ci-proof` passes with the new parts and `expect`. Run each
changed `lint:all` command locally with `CI_PROOF_NODE_REPORT_DIR` set and
check its report covers the files it gained. On the story-branch attempt,
read `ci-proof-summary-attempt-<n>` as in
[check a run](../../../docs/ci-proof-summary.md#check-a-run): all 18 files
appear under `scripts` with the check counts from the premise table and no
`other` outcome, every `scripts` part is present, and the MySQL 8.4 mutation
tests pass. Record the Lint job's duration beside the slowest E2E shard.

Behavior: A push runs `donut CI`. The Lint job installs the MySQL 8.4
binaries, `pnpm lint:all` runs each of the 18 files from the command of its
concern, and each command publishes its own `scripts` part, so the
attempt's summary gives every file's outcomes and cumulative recorded time.
A failing test fails `pnpm lint:all` and the attempt. Update the fragment
steps, `expect` and `docs/ci-proof-summary.md` (producer parts and counts)
to match.

### 3. Intent agrees with the summary for a selected attempt

Type: Behavior
Status: planned
Proof: For the final candidate's completed `donut CI` attempt, compare the
published artifact with the dashboard's figures for the same attempt and
revision, as plan 085 slice 8 did: the Scripts and Infrastructure group and
each of the 18 files have the same passed, failed, other and untimed counts
and cumulative recorded time. A mismatch holds completion. Failure and
missing-part behavior reuse the proof named in the outside-in table.

Behavior: A contributor selects that attempt in Intent's CI status and sees,
for the group and each of the 18 files, the outcomes and recorded time the
summary publishes.

## Concern evidence

- Slice-plan refinement was not needed: each slice has one outcome and one
  proof loop, and slice 2 keeps the wiring, parts and documentation together
  because a lint:all command without its part fails `pnpm test:ci-proof`.
- Slice 1 is the early probe for the runner premises above (MySQL 8.4
  minimal build, `ps`, `lsof`, `javac` on `ubuntu-latest`) and for the idle
  test's behavior there. Its result can add resolution slices before slice 2;
  the gate rule bounds that. Slices 1 and 2 wait on CI, an external-wait
  exception to the slice time target.
- This review found no other slice-specific concern.


## Story obligations

### G1. Hosted runner gate awaits the published probe
Reported: slice 1 — "Hosted MySQL execution, Java/process inspection, and all 30 repetitions remain uncovered; root owns those observations after progress publication."
Story clause: "Record the CI attempts and outcomes, and resolve any recurring failure in the group before making its tests required."
Disposition: proved by slice 1: runner-series-clean.md. Exact CI success at a84e1ee292, run42583951600 attempt1, with all30 full-group repetitions passed.


### G2. Hosted command and summary proof awaits publication
Reported: slice 2 — "The five changed commands and all 18 gained files passed locally. Hosted 13-part summary contents, Linux command results and Lint/E2E duration remain unobserved and owned by root after progress publication."
Story clause: "each file’s test outcomes and cumulative recorded test time under the `scripts` entry"
Disposition: return

## Current execution context

- Established story-branch identity, claim, checkout preparation and prior proof are retained in [execution-history.md](execution-history.md). First failed series is [runner-series.md](runner-series.md).
- Current published revision before this delivery is `a84e1ee2928e2db1d2418ec8b2023bdf1bb09bbf` on origin/refs/heads/codex/see-ci-results-and-timings-for-the-remaining-scr, with suffix base `2d4d52f06680eb94a8d5f55e4eb1fe256ea14960`. Work remains in the established checkout and branch.
- The developer authenticated gh as terryyin. Workflow selector ci.yml (5208694) resolves to donut CI. The checkout readiness command ./scripts/run.sh pnpm test:ci-proof passed 25/25 again.
- Old observer /tmp/dough-ci-501/watch-Th0z5b is stopped with no unread records and its PID gone. Current observer is cell g48:4, coordinator seed111-ai-chan, directory /tmp/dough-ci-501/watch-VZyjTJ, PID78934, session31456, same story-branch target. Managed resume recovered observation of the already-published repair without pushing.
- Second series run42579425249 attempt1 failed. All30 repetitions hit the missing MySQL client libncurses.so.5 dependency. Runner3 repetitions1 and5 also hit the named idle fixture's unavailable peer JVM cwd. These are owned recurring failures, with installer and fixture-isolation repairs locally accepted for progress publication. G1 stays open and a fresh series follows the repairs.
- CI preservation record /var/folders/65/16p4k5qj42qg7l46k2j0nhj40000gn/T/dough-ci-repair-stash-O3WvE5/record.json returned clean with oid null. Restore it after repair publication. Prior repair record was restored successfully with no bytes to apply.

## Second series repair proof

- Complete dated results and both recurring causes are recorded in [runner-series-after-mysql-isolation.md](runner-series-after-mysql-isolation.md). Gate remains open.
- Installer resolution supplies genuine Ubuntu security libncurses5/libtinfo5 6.3-2ubuntu0.3 packages (206,870 bytes). Published checksums and package dependency metadata matched. Both MySQL server and client --version are checked during acquisition. Official glibc2.28 minimal archive was unavailable (404), while the full archive was 908 MB. Retaining the 61 MB minimal build plus ABI5 packages keeps the measured setup small. ./scripts/run.sh node /tmp/seed111-validate-mysql-action.mjs passed action/Bash/input validation and ./scripts/run.sh pnpm test:ci-proof passed25/25. Actual Ubuntu acquisition remains owned by root in the fresh series.
- Idle fixture resolution reuses runCheck default clearEvidenceDeps, which supplies empty process/session evidence. The existing idle assertions observe snapshot and non-reservation output. With controlled external ps/lsof evidence via /tmp/seed111-idle-host-preload.mjs, the exact idle case failed0/1 before and passed1/1 after using ./scripts/run.sh node --import /tmp/seed111-idle-host-preload.mjs --test --test-name-pattern='verified idle fixture' scripts/worktree-retirement-checkout-processes.test.mjs. ./scripts/run.sh node --test scripts/worktree-retirement-checkout-processes.test.mjs scripts/worktree-retirement.test.mjs scripts/worktree-retirement-evidence.test.mjs passed28/28 in4.2s, including real JVM and unavailable-cwd refusals.
- Consumer traces /tmp/seed111-client-consumers.md and /tmp/seed111-idle-repair-consumers.md were inspected. Independent runner_repairs_refactor completed before publication. Accepted local proof remains valid. The fresh three-runner, ten-repetitions-each series is required before acceptance.

## Slice 1 accepted

- [Clean series](runner-series-clean.md) records run42583951600 attempt1 at a84e1ee2928e2db1d2418ec8b2023bdf1bb09bbf. Root independently reconciled18 files and30 repetitions with2340 raw passes, zero failures/cancellations/skips/todos. Native await-revision returned exact success. G1 is proved and all slice1 source commitments were observed.
- Removed the temporary probe and its exclusive timing outputs transitively. Retained Lint environment and MySQL installation/client/server checks passed ./scripts/run.sh pnpm test:ci-proof25/25 and ./scripts/run.sh node /tmp/seed111-validate-probe-removal.mjs. Consumer trace /tmp/seed111-clean-series-consumers.md covers every affected live caller. The one-time sweep is recorded separately as a reading with dated planning evidence classified.
- Refactor disposition is already-traced removal within the accepted probe_refactor and runner_repairs_refactor paths/consumer traces, plus records-only source/review/evidence changes. No new helper, dependency or representation was added in this increment.
- Human source amendment at trunk9a33d32751 was absorbed. It retains existing CI counting rules and removes a redundant scope sentence referring to the sibling counts story. A fresh source-only review was retained before gate acceptance. The repair publication began before absorption, an execution-order defect corrected here before any slice acceptance. Source freshness now matches trunk.
- Raw passing Node tests are78 per repetition. The CI reporter counts75 leaf checks across the18 files. The dated73-check baseline gains one cleanup leaf and one ambient-defaults leaf in worktree-retirement-mutation.test.mjs. Slice2 must observe its current report counts rather than copy the dated baseline.

## Slice 2 local proof

- All18 files are wired through five concern commands and13 scripts producer parts. Local report evidence is /tmp/seed111-slice2-local-proof.json, with actual75 passed leaf checks for the gained files and zero failed/other/untimed checks. Literal commands set CI_PROOF_NODE_REPORT_DIR=/tmp/seed111-slice2-node-reports through ./scripts/run.sh and pnpm. Changed command totals are sut-start85, browser-worktree-isolation140, development-stack23, worktree-retirement39 and publication-receiver4. All passed.
- ./scripts/run.sh pnpm test:ci-proof passed25/25, covering producer ordering/part totals and reused failed outcomes, duration, load-failure and missing-part semantics. Root inspected package command selection, all13 fragment counts/conditions, Bash total, summary expect and documentation. Consumer dispositions /tmp/seed111-slice2-consumers.md cover unchanged collectors and affected fixture callers.
- The newly combined SUT command exposed inherited npm_execpath selecting pnpm as the stand-in run-p executor. Controlled ./scripts/run.sh env npm_execpath=pnpm node --test --test-name-pattern='live owner control exposes' scripts/sut-owner-application-group.test.mjs failed0/1 before and passed1/1 after explicit --npm-path npm. The original fixture passed with npm_execpath removed. The normal pnpm command then passed85/85, including owned restart cases. Other helper consumers import unchanged waitUntil.
- This executor correction preserves the direct-Node gate conditions, where run-p already selects npm by default. The accepted30-run gate remains applicable, and the five-command proof covers the added pnpm execution context.
- Independent script_wiring_refactor completed before guarded progress delivery. It polished the changed documentation table only, preserving code/proof boundaries. Hosted artifact and timing observations remain open as G2.
