# First CI runner series: gate not met

**Identity:** SEED-111#run-uncovered-script-tests-in-ci
**Observed:** 2026-11-09, from the completed `donut CI` attempt and its retained probe artifacts.
**Revision:** `2d4d52f06680eb94a8d5f55e4eb1fe256ea14960`.
**Run / attempt:** [42575004767](https://github.com/nerds-odd-e/doughnut/actions/runs/42575004767), attempt 1.

## Provenance and selection

The branch-only probe selected all 18 files listed by the story, in one
`node --test --test-reporter=tap` invocation per repetition. Each of three
fresh `ubuntu-latest` matrix jobs ran the shared Lint setup, generated-types
check and `pnpm lint:all`, then ten sequential full-group invocations.
The three metadata file lists agree exactly. No selected test was skipped.

| Probe runner | Job | Artifact name | Artifact ID |
| --- | --- | --- | --- |
| 1 | [129020625942](https://github.com/nerds-odd-e/doughnut/actions/runs/42575004767/job/129020625942) | `script-runner-probe-attempt-1-runner-1` | 13655505029 |
| 2 | [129020626016](https://github.com/nerds-odd-e/doughnut/actions/runs/42575004767/job/129020626016) | `script-runner-probe-attempt-1-runner-2` | 13655176990 |
| 3 | [129020625940](https://github.com/nerds-odd-e/doughnut/actions/runs/42575004767/job/129020625940) | `script-runner-probe-attempt-1-runner-3` | 13654369840 |

The connected GitHub app downloaded each artifact. Native `gh` initially
returned HTTP 403 for job logs and HTTP 401 for artifact downloads. Its
`/tmp/seed111-probe-failure.json` diagnostic was supplemented by complete
logs fetched through the connected app for all three failed jobs; the bounded
accounting is `/tmp/seed111-probe-diagnostic-supplement.json`.
The local artifact copies are under
`/tmp/seed111-probe-42575004767/runner-{1,2,3}/`: `metadata.json`,
`results.json` and every `repetition-<n>.tap`. These local paths are temporary
recovery coordinates; the run, attempt, revision and artifact IDs above are
retained provenance. No download credentials or signed URLs are recorded.

## Runner observations

All three jobs report image `ubuntu24`, version `20261101.360.1`, and four
logical CPUs. Their recorded runner names and job IDs differ. All three report
hostname `runnervm7aw5e`; that hostname is preserved as observed and is not
used as a unique runner identity. Load is the recorded 1/5/15-minute average.
Memory values are bytes, captured before the repetition loop.

| Runner | Recorded runner name | Hostname | Total memory | Free memory | Load (1/5/15 min) | MySQL install (s) |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | `GitHub Actions 1000123702` | `runnervm7aw5e` | 16760352768 | 14716162048 | 1.51 / 0.96 / 0.43 | 6 |
| 2 | `GitHub Actions 1000123704` | `runnervm7aw5e` | 16760348672 | 14708621312 | 1.25 / 0.9 / 0.42 | 7 |
| 3 | `GitHub Actions 1000123699` | `runnervm7aw5e` | 16760344576 | 14671065088 | 1.35 / 0.77 / 0.34 | 5 |

All three metadata files record the same executable observations:

- `mysqld`: `/home/runner/work/_temp/mysql-8.4.3-linux-glibc2.17-x86_64-minimal/bin/mysqld`.
- `mysqld --version`: `Ver 8.4.3 for Linux on x86_64 (MySQL Community Server - GPL)`.
- `lsof`: `/usr/bin/lsof`.
- `ps`: `/usr/bin/ps`.
- `javac`: `/opt/hostedtoolcache/Java_Zulu_jdk/25.0.5-0.3.1/x64/bin/javac`.

The MySQL binary executes and reports version 8.4.3. These observations do
not establish a successfully initialized disposable server: initialization
fails as recorded below.

## Every repetition

Counts are the raw Node TAP summary, including parent tests. Group time is
its `duration_ms`, retained to six decimal places. Every process exited 1
without a signal. All repetitions report zero suites.

| Runner | Repetition | Tests | Passed | Failed | Cancelled | Skipped | Todo | Group time (ms) |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | 1 | 77 | 69 | 8 | 0 | 0 | 0 | 5839.071474 |
| 1 | 2 | 77 | 69 | 8 | 0 | 0 | 0 | 5742.522894 |
| 1 | 3 | 77 | 69 | 8 | 0 | 0 | 0 | 5904.499864 |
| 1 | 4 | 77 | 69 | 8 | 0 | 0 | 0 | 5886.673700 |
| 1 | 5 | 77 | 69 | 8 | 0 | 0 | 0 | 6015.345282 |
| 1 | 6 | 77 | 69 | 8 | 0 | 0 | 0 | 6177.750400 |
| 1 | 7 | 77 | 69 | 8 | 0 | 0 | 0 | 5953.163176 |
| 1 | 8 | 77 | 69 | 8 | 0 | 0 | 0 | 5970.464230 |
| 1 | 9 | 77 | 69 | 8 | 0 | 0 | 0 | 5960.396394 |
| 1 | 10 | 77 | 69 | 8 | 0 | 0 | 0 | 6116.625158 |
| 2 | 1 | 77 | 69 | 8 | 0 | 0 | 0 | 5544.569755 |
| 2 | 2 | 77 | 69 | 8 | 0 | 0 | 0 | 5533.818840 |
| 2 | 3 | 77 | 69 | 8 | 0 | 0 | 0 | 5643.028797 |
| 2 | 4 | 77 | 69 | 8 | 0 | 0 | 0 | 5591.811814 |
| 2 | 5 | 77 | 69 | 8 | 0 | 0 | 0 | 5689.256144 |
| 2 | 6 | 77 | 69 | 8 | 0 | 0 | 0 | 5588.872533 |
| 2 | 7 | 77 | 69 | 8 | 0 | 0 | 0 | 5750.438915 |
| 2 | 8 | 77 | 69 | 8 | 0 | 0 | 0 | 5670.002579 |
| 2 | 9 | 77 | 69 | 8 | 0 | 0 | 0 | 5524.276840 |
| 2 | 10 | 77 | 69 | 8 | 0 | 0 | 0 | 5498.610871 |
| 3 | 1 | 77 | 69 | 8 | 0 | 0 | 0 | 4823.199981 |
| 3 | 2 | 77 | 69 | 8 | 0 | 0 | 0 | 4817.015161 |
| 3 | 3 | 77 | 69 | 8 | 0 | 0 | 0 | 4670.915010 |
| 3 | 4 | 77 | 69 | 8 | 0 | 0 | 0 | 4787.692794 |
| 3 | 5 | 77 | 69 | 8 | 0 | 0 | 0 | 5088.308864 |
| 3 | 6 | 77 | 69 | 8 | 0 | 0 | 0 | 4691.902091 |
| 3 | 7 | 77 | 69 | 8 | 0 | 0 | 0 | 4913.052219 |
| 3 | 8 | 77 | 69 | 8 | 0 | 0 | 0 | 4652.410547 |
| 3 | 9 | 77 | 69 | 8 | 0 | 0 | 0 | 4859.178961 |
| 3 | 10 | 77 | 69 | 8 | 0 | 0 | 0 | 4748.916142 |

## Failing results and diagnosis

Every repetition has the same eight failed raw results. The first seven
below are leaves whose disposable MySQL initialization fails; the last is
the parent of the seventh leaf and has a downstream assertion failure.

| File | Kind | Exact failed test name |
| --- | --- | --- |
| `scripts/worktree-retirement-e2e-mutation.test.mjs` | leaf | `second DROP failure reports partial retirement; retry completes both targets` |
| `scripts/worktree-retirement-e2e-mutation.test.mjs` | leaf | `idle unit+E2E allocation drops both schemas and preserves peer and claims` |
| `scripts/worktree-retirement-e2e-mutation.test.mjs` | leaf | `unrecorded E2E-shaped database stays untouched when only unit is recorded` |
| `scripts/worktree-retirement-mutation.test.mjs` | leaf | `interruption after marker: starts blocked; retry completes same targets` |
| `scripts/worktree-retirement-mutation.test.mjs` | leaf | `idle unit-only allocation drops target schema and preserves peer sentinel` |
| `scripts/worktree-retirement-mutation.test.mjs` | leaf | `missing unit schema counts as already absent; still marks retired` |
| `scripts/worktree-retirement-mutation.test.mjs` | nested leaf | `use a disposable server` |
| `scripts/worktree-retirement-mutation.test.mjs` | parent | `disposable MySQL cleanup returns only after its server has exited` |

Each of the seven initialization errors contains:
`[MY-010187] [Server] Could not open file '/var/log/mysql/error.log' for error logging: Permission denied`.
They also warn that the user was already set to `mysql` before the explicit
`runner` user. `MY-013236` (unusable data directory) and `MY-010119` (aborting)
follow that error. This is consistent across all 210 initialization failures.
The stack reaches `startDisposableMysql` in
`scripts/worktree-retirement-disposable-mysql.mjs:127` at this revision.

The runner ambient MySQL option files leak system logging/user settings into
the disposable helper. This is a reproducible fixture-isolation/setup defect,
not a proven external infrastructure outage. The server binary itself starts
its initialization path, but no affected scenario acquires its intended
disposable server. The parent's following assertion is described separately.

The parent `disposable MySQL cleanup returns only after its server has exited`
continues after its child fails before assigning `pid`. Its own assertion
expects `ESRCH` but receives `ERR_INVALID_ARG_TYPE` from `process.kill` with
an undefined PID. This eighth raw failure is downstream of the same failed
initialization, rather than evidence of a second independent helper defect.

## Idle observation and remaining gate

A one-time reading of every retained TAP file found exactly one passing
`verified idle fixture reports idle snapshot not a reservation` result in each:
30 passes, zero failures for the named idle case. Every full-group invocation
still failed as above. This reading reuses the existing observations; it is
not an added test or a new execution.

**Gate not met.** The MySQL failures recur in all 30 repetitions across all
three runner jobs. Root owns the fixture-isolation repair and a fresh full
group series after the repair. This record accepts neither the repair nor
a passing gate and does not make the retirement tests required.
