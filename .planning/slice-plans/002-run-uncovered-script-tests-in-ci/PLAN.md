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

Not included: the 13 `scripts/ci/application-release-*.test.mjs` files, Node
tests outside `scripts/`, CLI CI proof, SEED-112's test counts, changes to
the Cloud VM setup, and removing or merging any of the 18 files. No test is
skipped or left out on CI to make an attempt pass. A file that cannot run on
the runner without changing what it checks goes to the story owner.

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
Status: planned
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


## Execution context

On 2026-11-09 the developer declined an exception to the mandatory 250-line
refactor check. Continue the same refactor boundary with a cohesive split of
CI configuration; retain all jobs, producer observations and failure semantics.
The initial independent pass stopped on the 351-line existing workflow (460
with the temporary probe). No hosted gate or slice completion is claimed.

The initial observer below stopped cleanly at that decision boundary, with no
unread records and its PID gone. After the developer's answer, root verified
that terminal identity and rearmed: cell `g151:3`, coordinator
`seed111-ai-chan`, directory `/tmp/dough-ci-501/watch-Th0z5b`, PID 5120,
session 39826, same branch target. GitHub's directed API wait ends at
2026-11-09T07:11:00.142Z; the live observer retains it. The execution branch
fast-forwarded to supplier revision 03fc16245a15ef12ced590dfea751cfc25187f1b;
that revision adds the cleanup regression and leaves its helper unchanged.
The probe publication accepted 2d4d52f06680eb94a8d5f55e4eb1fe256ea14960
on the story branch, with suffix base dc1372e73effdcc1d646bf5733675ea99828813c.

- Established execution: `SEED-111#run-uncovered-script-tests-in-ci`, publisher
  `dashboard-territory.local-doughnut`, agent Ai-chan. The accepted claim is
  `dc1372e73effdcc1d646bf5733675ea99828813c`; it is the first increment's
  previously published base. Starting revision:
  `6a25ee9b5dd69f1bacc7cec41ea931c62813623f`. No start command is repeated.
- Mode: story-branch. Owned execution checkout:
  `/Users/terryyin/git/doughnut/.worktrees/see-ci-results-and-timings-for-the-remaining-scr`;
  branch `codex/see-ci-results-and-timings-for-the-remaining-scr`. Increments
  target that branch on `origin`; closure targets `origin/main`. Default
  checkout: `/Users/terryyin/git/doughnut`.
- Checkout preparation: `./scripts/run.sh bash scripts/worktree_setup.sh`
  succeeded with the workspace installation fingerprint current;
  `./scripts/run.sh pnpm test:ci-proof` passed 25/25 tests. Existing plan
  authority allows replanning within the understood story scope.
- CI source: GitHub Actions `ci.yml`, locally named `donut CI`. Codex observer
  cell `g141:11`, coordinator `seed111-ai-chan`, directory
  `/tmp/dough-ci-501/watch-ywFEsC`, PID 7673, session 31911, target
  `origin/refs/heads/codex/see-ci-results-and-timings-for-the-remaining-scr`.
  Shared request owner directed a wait until
  `2026-11-09T07:11:00.334Z`; the armed observer retains that wait. The claim's
  trunk CI is unobserved.


## Story obligations

### G1. Hosted runner gate awaits the published probe
Reported: slice 1 — "Hosted MySQL execution, Java/process inspection, and all 30 repetitions remain uncovered; root owns those observations after progress publication."
Story clause: "Record the CI attempts and outcomes, and resolve any recurring failure in the group before making its tests required."
Disposition: return

## Slice 1 progress evidence

- Temporary probe infrastructure is locally validated; the hosted gate remains open as G1. Three fresh ubuntu-latest matrix jobs each run the same Lint setup, generated-types check and pnpm lint:all, then ten sequential full-group invocations. Every TAP log, status/count/duration result and runner/tool/install metadata is uploaded. The required test commands are unchanged pending the gate.
- Local proof: ./scripts/run.sh pnpm test:ci-proof passed all 25 tests; the workflow tests observe unchanged producer parts, command order and push trigger. The implementation parsed YAML and checked exact seed file selection, matching setup steps, matrix size 3, loop size 10 and embedded Node/Bash syntax. scripts/check_diff_whitespace.sh passed. Official minimal archive returned HTTP 200, 61,378,984 bytes. Linux runtime capabilities are unobserved until G1.
- Consumer trace: /tmp/seed111-probe-consumers.md. The workflow proof suite is reused unchanged. The new action feeds MYSQL_BASEDIR to the disposable helper; its two file consumers are included in every hosted repetition. Root inspected the helper stop ordering, named idle test, probe selection and result retention. Explicit ciWorkflow selects the already-used ci.yml for the retained observer.
- Source freshness at pinned trunk 03fc16245a15ef12ced590dfea751cfc25187f1b was unchanged. The sole incoming supplier change adds a cleanup-waits-for-exit regression to worktree-retirement-mutation.test.mjs; the execution branch fast-forwarded to that revision before this probe, preserving published history. The supplier helper is unchanged. This adds one reported leaf check; later report acceptance uses the current file contents alongside the dated baseline. SEED-112 remains unimplemented on pinned trunk.
- Expensive-proof decision: actual target is three fresh independent GitHub runners, ten sequential Node invocations each. Local 35.6 s was a busy-host proxy, not runner headroom evidence. The probe captures runner CPU/memory/load before repetitions and has a 20-minute job bound. Root owns hosted observation; reconsider on timeout, contention or failures before another series. No local repeated suite was launched.
- Refactor: independent probe_refactor return completed after the developer declined the size exception; main workflow is 249 lines. Cohesive local actions own lint_environment, lint_and_generated_types, package_application_artifacts and the temporary script_runner_probe. ./scripts/run.sh pnpm test:ci-proof passed 25/25; ./scripts/run.sh node /tmp/seed111-validate-ci-refactor.mjs preserved all eight job graphs, matrices, commands, environments, failure conditions, proof/artifact contracts and step order against the pre-extraction probe YAML. scripts/check_diff_whitespace.sh passed. All changed files are at most 250 lines. Root inspected action inputs, Bash shells, fragment conditions, shared setup and package upload paths; no behavior beyond the temporary probe and MySQL binaries installation is asserted complete.
- The installed hook is check-only. Fresh delivery inherits pnpm_config_verify_deps_before_run=false to preserve that contract with the installed hook version.

## MySQL runner repair progress

- First series: [runner-series.md](runner-series.md), run 42575004767 attempt 1 at 2d4d52f06680eb94a8d5f55e4eb1fe256ea14960. All 30 repetitions failed (69 passed, 8 failed / 77 raw tests each). The named idle case passed all 30. Seven initializations per repetition read ambient system MySQL defaults and failed opening /var/log/mysql/error.log; the eighth raw failure is the cleanup parent downstream of its child initialization. These are recurring fixture/setup failures, so the gate remains open.
- Bounded resolution through the owned CI repair: startDisposableMysql now passes --no-defaults first to initialization and server startup. The existing in-scope mutation file supplies an ambient MYSQL_HOME/my.cnf with an unavailable log path and observes a real SELECT 42 result. No retirement refusal or assertion is weakened, and client/persistent-server defaults are unchanged. A fresh full-group series remains required before slice 2.
- Red: ./scripts/run.sh node --test --test-name-pattern='ambient server defaults' scripts/worktree-retirement-mutation.test.mjs selected exactly the new test and failed 0/1 with MY-010187. Green: ./scripts/run.sh node --test scripts/worktree-retirement-mutation.test.mjs scripts/worktree-retirement-e2e-mutation.test.mjs passed 12/12 in 16.6 s. scripts/check_diff_whitespace.sh passed. Consumer trace: /tmp/seed111-mysql-repair-consumers.md; both helper consumers were exercised, persistent servers and production admin adapter are independent.
- Independent mysql_repair_refactor accepted without edits or repeated tests; helper 201 lines, mutation tests 246, series evidence 146. Local focused proof followed load/memory inspection; no repeated local full group was launched. Root owns the new Linux series.
- The prior observer /tmp/dough-ci-501/watch-Th0z5b stopped cleanly (no unread events; PID exited). Its failure at 2d4d52f066 is accounted above. Native gh has no token or authenticated profile; the developer has been asked to authenticate it. The connected GitHub app recovered all three complete logs and artifacts. Native CI coverage is unavailable until authentication and rearming; no anonymous observation is claimed.
- Sanctioned CI preservation returned clean with oid null: /var/folders/65/16p4k5qj42qg7l46k2j0nhj40000gn/T/dough-ci-repair-stash-ohHycd/record.json. Restore that record after repair publication before resuming normal slice work.
