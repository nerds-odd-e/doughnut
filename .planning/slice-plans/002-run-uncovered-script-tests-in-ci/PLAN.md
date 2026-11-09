# See CI results and timings for the remaining script tests in Intent

**Identity:** SEED-111#run-uncovered-script-tests-in-ci
**Source:** [Refined story](../../seeds/SEED-111-run-uncovered-script-tests-in-ci.md#run-uncovered-script-tests-in-ci)

## Goal and scope

Every `donut CI` attempt runs the story's 18 Node test files on
`ubuntu-latest`, fails when one of their tests fails, and publishes each
file's test outcomes and cumulative recorded time under the `scripts` entry
of the attempt's CI proof summary. Intent's Scripts and Infrastructure group
shows the same figures for that attempt. Before the retirement tests become
required, the full group runs repeatedly on the CI runner, and recurring
failures are resolved first.

Not included: the 13 `scripts/ci/application-release-*.test.mjs` files, Node
tests outside `scripts/`, CLI CI proof, SEED-112's test counts, changes to
the Cloud VM setup, and removing or merging any of the 18 files. No test is
skipped or left out on CI to make an attempt pass. A file that cannot run on
the runner without changing what it checks goes to the story owner.

## Current decisions

- **Story Branch Mode.** Increments publish to the story branch, and
  `donut CI` runs on every branch push. The files become required on `main`
  only when the story lands, after the slice 2 gate holds. Temporary probe
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
  the profiling receiver check. The command names appear only in
  `package.json`, `.github/workflows/ci.yml` and `docs/ci-proof-summary.md`.
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
  Record the Lint job's duration after slice 3 and keep it under the slowest
  E2E shard where the measured cost allows.
- **Gate rule (from the story).** A series is at least 30 full-group
  repetitions (all 18 files in one `node --test` invocation, as in the
  story's evidence), spread over at least 3 fresh runners, each after the Lint
  job's setup and `pnpm lint:all`. Any failure of `verified idle fixture
  reports idle snapshot not a reservation` counts as recurring. Another
  test's failure counts as recurring when it fails in two repetitions or its
  diagnosed cause can repeat, as a race can. A recurring failure gets a
  resolution slice before slice 3, then a new series. Thirty clean
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
| The retirement tests clean up their disposable MySQL without failing | 1 | Repeated full-group runs show no `ENOTEMPTY`, no leftover `doughnut-retire-mysql-*` directory and no leftover helper `mysqld` |
| The full group runs repeatedly on the CI runner, recorded per attempt, and recurring failures are resolved before the tests are required | 1, 2 | The plan records the series' run, attempts, runners and every repetition's outcome; no recurring failure remains |
| The runner provides what the files need | 2, 3 | The probe and the slice 3 attempt pass the MySQL 8.4 mutation tests and the JVM process tests on `ubuntu-latest` |
| Every attempt runs all 18 files, and a failing test fails the attempt | 3 | The story-branch attempt's Lint job runs them inside `pnpm lint:all`, whose `&&` chain fails the job on a failing command |
| Each file's outcomes and cumulative recorded time are published, with no test skipped | 3 | The attempt's `ci-proof-summary-attempt-<n>` lists all 18 files under `scripts` with every part present, per-file check counts equal to the local report below, and no `other` outcome |
| Failed tests count with their recorded durations | 3 | Reused: SEED-109's accepted `scripts/ci/ci-proof-outcomes-node.test.mjs`, since the new files pass through the same wrapper and reporter |
| Intent shows the same figures | 4 | The dashboard's figures for the selected attempt equal the artifact for the group and each of the 18 files |

## Decisive premises

| Premise and consumer | Observation and result |
| --- | --- |
| The 18 files pass together through the CI wrapper, and its Node report attributes checks to all 18 (slice 3) | 2026-11-09 at `3dce9d850f`, this macOS host in the Nix shell: `CURSOR_DEV=true nix --extra-experimental-features 'nix-command flakes' develop -c env npm_lifecycle_event=probe-18 CI_PROOF_NODE_REPORT_DIR=<dir> node scripts/ci/node-test.mjs <18 files>` exited 0 with 75 tests passed. `probe-18.ndjson` held 73 passed checks: `dev-healthcheck` 4, `isolated-cypress-openai-mock-cancel` 2, `-failure` 1, `-ownership` 2, `isolated-cypress-openai-mock` 7, `isolated-cypress-owning-health` 2, `isolated-openai-mock` 2, `isolated-service-mocks` 4, `local-runtime-target` 3, `profiling/verify-publication-receiver` 4, `sut-owner-application-group` 2, `worktree-reset-isolation-harness-openai-mock` 1, `-wikidata-mock` 2, `worktree-retirement-checkout-processes` 9, `-e2e-mutation` 3, `-evidence` 11, `-mutation` 6, `worktree-retirement` 8. Run duration 35.6 s on a busy host. This is a proxy for the Linux runner; `ps`, `lsof`, `javac` and MySQL binaries there stay unverified until slice 2. |
| The full group fails from the disposable MySQL cleanup (slice 1) | Same host and revision, `CURSOR_DEV=true nix --extra-experimental-features 'nix-command flakes' develop -c node --test <18 files>` four times at load average 21 to 25 on 16 cores: each run failed one test in `scripts/worktree-retirement-mutation.test.mjs` (lines 149, 149, 99, 45) with `ENOTEMPTY` on `doughnut-retire-mysql-*`, raised by `stop` in `scripts/worktree-retirement-disposable-mysql.mjs`, which calls `rmSync` right after sending `SIGTERM` to `mysqld`. The idle retirement test passed in all five runs. |
| The idle retirement test can fail in the full group (slice 2) | Retained in the story's evidence: on 2026-11-09 at `afe8472ca0`, one of two local full-group runs failed it when the check could not determine a JVM's working directory. Its behavior on the runner is unobserved; slice 2 observes it. |
| `ubuntu-latest` lacks MySQL 8.4 binaries where the helper looks (slices 2 and 3) | `gh variable list` on 2026-11-09: `RUNNER` is `ubuntu-latest`. The helper resolves `MYSQL_BASEDIR` or `$(dirname $(dirname $(which mysqld)))` and runs `bin/mysqld`; the only MySQL in CI is the `.github/mysql_db` container. The runner image's contents are unobserved; slice 2 installs and records them. |
| The 8.4 minimal tarball runs the disposable server on the runner (slices 2 and 3) | Download sizes from `curl -sIL` on 2026-11-09: `mysql-8.4.3-linux-glibc2.17-x86_64-minimal.tar.xz` 61,378,984 bytes; `mysql-8.4.3-linux-glibc2.28-x86_64.tar.xz` 908,299,408 bytes. Whether the minimal build initializes and serves the helper on `ubuntu-latest` is unobserved; slice 2 is its early probe, and its failure changes the install before slice 3. |
| A new `lint:all` Node test command without its fragment step fails the workflow check (slice 3 proof) | 2026-11-09 at `3dce9d850f`: a scratch `test:worktree-retirement` command added to `package.json` and `lint:all`, then `CURSOR_DEV=true nix --extra-experimental-features 'nix-command flakes' develop -c node --test scripts/ci/ci-workflow.test.mjs` failed "each Node test command in lint:all reports its own scripts part after the Bash part" (4 passed, 1 failed). Reverted. `pnpm test:ci-proof` runs the same file through `node-test.mjs`, which adds reporters only when asked. |
| Intent reads a selected attempt's summary (slice 4) | 2026-11-09, the local dashboard (Open Dough 0.3.92, deployment `e8bbd6b9fdf8`): `curl -H 'Origin: http://127.0.0.1:4173' -H 'Sec-Fetch-Site: same-origin' 'http://127.0.0.1:4173/__authenticated-read?source=doughnut&intent=ci-proof&run=42498952903&attempt=1&revision=afe8472ca00f815ac73cc01bc203b6a245293435'` returned 200 with that attempt's summary; its `scripts` entry had 390 passed, 0 failed, 90,795.72 ms, its parts and a per-file breakdown. Without the two headers the route answers 403; without `attempt` it answers 400. Plan 085 slice 8 compared the same route with the artifact on 2026-11-08. |

## Slices

### 1. Disposable MySQL stops before its directory is removed

Type: Structure
Status: planned
Proof: Before the change, reuse the retained failing runs above when the host
load is comparable; otherwise reproduce `ENOTEMPTY` first. After the change,
run the full group ten times in sequence with
`CURSOR_DEV=true nix --extra-experimental-features 'nix-command flakes' develop -c node --test <18 files>`:
no `ENOTEMPTY`, the retirement files pass, and afterwards no
`doughnut-retire-mysql-*` directory or helper `mysqld` remains. Its only
users are `scripts/worktree-retirement-mutation.test.mjs` and
`scripts/worktree-retirement-e2e-mutation.test.mjs`. The host is shared
with other sessions; run the repetitions one after another.

Structure: `startDisposableMysql`'s cleanup removes its temporary directory
only after its `mysqld` has exited. The retirement scripts' behavior is
unchanged. Enables slice 2, whose repetitions must not fail on cleanup.

### 2. The full group passes repeatedly on the CI runner

Type: Behavior
Status: planned
Proof: The plan records the series: run IDs, attempts, runners, each
repetition's passed and failed counts with failing tests and errors, the
group's time per repetition, the MySQL install time, and the runner's
`mysqld --version`, `lsof`, `ps` and `javac` paths. The series meets the gate
rule above.

Behavior: With slice 1 in place, a temporary probe job on the story branch
runs on `ubuntu-latest` with the Lint job's setup, `pnpm lint:all` and the
new MySQL 8.4 install, then repeats the full group until the series is
complete. This is the early probe for the runner: if the install or a file
cannot work there, slices 3 and 4 stop and the plan changes. A recurring
failure under the gate rule adds a resolution slice before slice 3 and a new
series. Once the series is recorded, revert the probe job on the story
branch and keep the MySQL install for slice 3.

### 3. Every CI attempt runs the 18 files and publishes their proof

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

### 4. Intent agrees with the summary for a selected attempt

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
  proof loop, and slice 3 keeps the wiring, parts and documentation together
  because a lint:all command without its part fails `pnpm test:ci-proof`.
- Slice 2 is the early probe for the runner premises above (MySQL 8.4
  minimal build, `ps`, `lsof`, `javac` on `ubuntu-latest`) and for the idle
  test's behavior there. Its result can add resolution slices before slice 3;
  the gate rule bounds that. Slices 2 and 3 wait on CI, an external-wait
  exception to the slice time target. Slice 1's ten repetitions are a
  focused-test exception: a race needs repeated runs to show it is gone.
- This review found no other slice-specific concern.
