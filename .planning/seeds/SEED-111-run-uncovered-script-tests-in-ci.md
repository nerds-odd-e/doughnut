---
id: SEED-111
status: active
planted: 2026-11-09
scope: story
---

# See CI results and timings for the remaining script tests in Intent

<a id="run-uncovered-script-tests-in-ci"></a>

### See CI results and timings for the remaining script tests in Intent

**Identity:** SEED-111#run-uncovered-script-tests-in-ci
```json dough-story-dependencies
{"schemaVersion":1,"identity":"SEED-111#run-uncovered-script-tests-in-ci","dependencies":[{"supplier":{"identity":"SEED-114#disposable-mysql-stops-before-cleanup","href":"seeds/SEED-114-disposable-mysql-cleanup.md#disposable-mysql-stops-before-cleanup"},"implementation":"SEED-111's first slice repeats the full 18-file group at least 30 times on the CI runner and treats any recurring failure as blocking.","rationale":"The disposable MySQL helper currently fails the retirement mutation tests with ENOTEMPTY on a busy host and can leave mysqld running; a series run before that is fixed would fail on cleanup rather than measure the runner. Reordering alone does not stop SEED-111 from starting first.","condition":"The disposable MySQL helper removes its temporary folder only after its mysqld has exited, so repeated full-group runs show no ENOTEMPTY and leave no helper mysqld or doughnut-retire-mysql-* folder.","state":"satisfied","resolution":{"revision":"37253a42ccbf19ab0026b67c1168da2933f6e9fe","path":".planning/seeds/SEED-114-disposable-mysql-cleanup.md#disposable-mysql-stops-before-cleanup","summary":"Directly satisfied. scripts/worktree-retirement-disposable-mysql.mjs `stop` now awaits terminateOwnedProcessTree (SIGTERM, 5 s wait, SIGKILL) and only then removes the doughnut-retire-mysql-* folder, from both the t.after hook and the not-ready path. Proof in plan 003 slice 1 at 37253a42cc: a probe saw the server gone (ESRCH) and the folder absent right after the cleanup resolved, including a SIGSTOP-paused server (5,061 ms); ten sequential runs of the 18-file group on 2026-11-09 had no ENOTEMPTY, both helper users passed every test, and no helper mysqld or folder remained. SEED-111's plan already assumes this cleanup; no consumer assumption changes. Limit: the helper's test users do not fail when the await is dropped (intent coverage at 37253a42cc), so CI observes the current ordering rather than guarding it.\nCondition satisfied: The disposable MySQL helper removes its temporary folder only after its mysqld has exited, so repeated full-group runs show no ENOTEMPTY and leave no helper mysqld or doughnut-retire-mysql-* folder.\nAccepted integration: 5d70136ac83337f9258ee2e61e618be85b900482 on origin/refs/heads/main."}}]}
```
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/002-run-uncovered-script-tests-in-ci/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"69a29bc4c8261f2b89ecd4c2cc1140faecb9ff270a041b78eeffd15479b38a42","plan":"c079bdadeb5ebfb4f03395b10cdf36cda22d9aca35dbe21ecf2c233aac024ed8"}}
```

**Goal** Doughnut contributors can see, in Intent’s Scripts and
Infrastructure group for a selected `donut CI` attempt, whether each of the
18 Node test files listed below passed and how much recorded test time each
contributed. No CI command runs these files today, so a regression in the
worktree retirement, isolated mock, and local runtime scripts they check is
found only when someone runs them by hand.

**Scope**

Required behavior:

- Every `donut CI` attempt runs all 18 files on the CI runner, and a failing
  test in them fails the attempt, as other script tests do. That is what
  "required" means here. The retirement tests become required only after the
  gate below.
- The attempt’s [CI proof summary](../../docs/ci-proof-summary.md) publishes
  each file’s test outcomes and cumulative recorded test time under the
  `scripts` entry, and Intent shows the same figures for that attempt. Failed
  tests contribute their outcomes and whatever durations the runner recorded.
  The summary’s existing rules apply unchanged: a file that fails to load is
  one failed check, and a part that did not run shows as missing.
- The CI runner provides what the files need. For example,
  `scripts/worktree-retirement-e2e-mutation.test.mjs` starts its own MySQL 8.4
  server from local binaries (`mysqld` on `PATH`, or `MYSQL_BASEDIR`) and
  checks its version, and `scripts/worktree-retirement-checkout-processes.test.mjs`
  starts a Java process and inspects processes with `ps` and `lsof`.

Required restriction: no test in these files is skipped or left out on CI to
make an attempt pass, because that would hide whether it passes, which is
what this story makes visible. If a file cannot run on the CI runner without
changing what it checks, the story owner decides what happens to it.

Scope limits:

- The 13 `scripts/ci/application-release-*.test.mjs` files keep running
  inside the Bash `scripts/test/application-release.test` check, which counts
  them as one check.
- Node test files outside `scripts/` and CLI CI proof are outside scope.
- Removing or merging any of the 18 files is not part of this story.

Boundary assumption: the intended CI runner is GitHub’s `ubuntu-latest`, which
the repository variable `RUNNER` and the lint job both named on 2026-11-09.

**Before making the retirement tests required** Run `verified idle fixture
reports idle snapshot not a reservation` from
[`scripts/worktree-retirement-checkout-processes.test.mjs`](../../scripts/worktree-retirement-checkout-processes.test.mjs#L177)
repeatedly with the full test group (all 18 files together) on the intended
CI runner, after the same setup as the job that will run it. Record the CI
attempts and outcomes, and resolve any recurring failure in the group before
making its tests required. The local evidence below already shows one
failure of the idle test, so any failure of it in the CI repetitions counts
as recurring. The disposable MySQL cleanup failure in the same evidence
already recurs, so it is resolved before the CI repetitions.

**Key examples**

1. **All pass.** A candidate revision’s `donut CI` attempt runs the 18 files
   and they all pass. The attempt’s `ci-proof-summary-attempt-<n>` lists each
   file under `scripts` with its passed count and recorded time, including
   `scripts/worktree-retirement-e2e-mutation.test.mjs` with its tests passed
   against a MySQL 8.4 server started on the runner. Intent’s CI status for
   that attempt shows the same count and time for each file and for the
   group.
2. **One test fails.** One test in `scripts/isolated-service-mocks.test.mjs`
   fails on the runner. The attempt fails. The summary and Intent count that
   test as failed for its file, with its recorded duration when the runner
   reported one, and the file’s other tests keep their own results.
3. **Retirement gate.** Before the retirement tests are required, the full
   group runs repeatedly on the CI runner and the plan records each CI
   attempt with its outcomes. If the idle test fails in any repetition, or
   another test fails recurrently, its cause is resolved and the repetitions
   run again before the tests become required.

**Evidence** On 2026-11-09, a local Nix run at `afe8472ca0` passed 74 of 75 tests.
The idle retirement case failed when the check could not determine a JVM’s
working directory. Its file passed on its own, and the full rerun passed all
75 tests in 6.6 seconds. The original coverage gap was recorded during
[SEED-109 completion](https://github.com/nerds-odd-e/doughnut/commit/10ac6ef354e15643409df4e7e9e9da3524e7a2a1).

Later on 2026-11-09, at `3dce9d850f`, five more local Nix runs of the full
group on a busy machine (load average 21 to 25 on 16 cores) passed all 75
tests once and failed one test in each of the other four. Every failure was
in `scripts/worktree-retirement-mutation.test.mjs`, in three different tests,
with `ENOTEMPTY` when the disposable MySQL helper
(`scripts/worktree-retirement-disposable-mysql.mjs`) removed its temporary
directory right after signaling `mysqld` to stop, without waiting for it to
exit. The idle retirement test passed in all five runs.

#### Files in scope

- `scripts/dev-healthcheck.test.mjs`
- `scripts/isolated-cypress-openai-mock-cancel.test.mjs`
- `scripts/isolated-cypress-openai-mock-failure.test.mjs`
- `scripts/isolated-cypress-openai-mock-ownership.test.mjs`
- `scripts/isolated-cypress-openai-mock.test.mjs`
- `scripts/isolated-cypress-owning-health.test.mjs`
- `scripts/isolated-openai-mock.test.mjs`
- `scripts/isolated-service-mocks.test.mjs`
- `scripts/local-runtime-target.test.mjs`
- `scripts/profiling/verify-publication-receiver.test.mjs`
- `scripts/sut-owner-application-group.test.mjs`
- `scripts/worktree-reset-isolation-harness-openai-mock.test.mjs`
- `scripts/worktree-reset-isolation-harness-wikidata-mock.test.mjs`
- `scripts/worktree-retirement-checkout-processes.test.mjs`
- `scripts/worktree-retirement-e2e-mutation.test.mjs`
- `scripts/worktree-retirement-evidence.test.mjs`
- `scripts/worktree-retirement-mutation.test.mjs`
- `scripts/worktree-retirement.test.mjs`
