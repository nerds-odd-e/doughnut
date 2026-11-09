# Second CI runner series after MySQL isolation

Identity `SEED-111#run-uncovered-script-tests-in-ci`.
Observed on 2026-11-09 from the completed `donut CI` attempt and saved probe artifacts.
Revision `b691bba3009d776a9c295931bf452fb65e1d4dec`.
Run [42579425249](https://github.com/nerds-odd-e/doughnut/actions/runs/42579425249), attempt 1.

## Provenance and selection

Three fresh `ubuntu-latest` matrix jobs ran the shared Lint setup, generated-types
check and `pnpm lint:all`, then ten sequential full-group invocations each.
Every invocation selected the same 18 scoped files in one
`node --test --test-reporter=tap` command. The metadata file lists agree.
The raw TAP summaries and `results.json` agree for every repetition.

| Runner | Job | Artifact name |
| --- | --- | --- |
| 1 | [129034383950](https://github.com/nerds-odd-e/doughnut/actions/runs/42579425249/job/129034383950) | `script-runner-probe-attempt-1-runner-1` |
| 2 | [129034383901](https://github.com/nerds-odd-e/doughnut/actions/runs/42579425249/job/129034383901) | `script-runner-probe-attempt-1-runner-2` |
| 3 | [129034384009](https://github.com/nerds-odd-e/doughnut/actions/runs/42579425249/job/129034384009) | `script-runner-probe-attempt-1-runner-3` |

Authenticated `gh` downloaded the artifacts under
`/tmp/seed111-probe-42579425249/script-runner-probe-attempt-1-runner-{1,2,3}/`.
Each directory holds `metadata.json`, `results.json` and all ten TAP files.
The native diagnostic `/tmp/seed111-second-failure.json` accounts for all three
failed jobs with `inventory.complete` true and an empty gap list.
These local paths are temporary recovery coordinates. Run, attempt, revision,
job IDs and artifact names retain the observation provenance.

## Runner observations

All three jobs record image `ubuntu24`, version `20261101.360.1`, and four
logical CPUs. Their recorded runner names and job IDs differ. All three record
hostname `runnervm7aw5e`. The distinct runner names and job IDs identify the
jobs in this evidence. Memory values are bytes, captured before repetitions.
Load is the recorded 1/5/15-minute average.

| Runner | Recorded runner name | Hostname | Total memory | Free memory | Load | MySQL install (s) |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | `GitHub Actions 1000123750` | `runnervm7aw5e` | 16760344576 | 14804393984 | 1.77 / 1.18 / 0.54 | 8 |
| 2 | `GitHub Actions 1000123735` | `runnervm7aw5e` | 16760340480 | 14780047360 | 1.31 / 0.8 / 0.36 | 5 |
| 3 | `GitHub Actions 1000123737` | `runnervm7aw5e` | 16759304192 | 14588321792 | 1.7 / 1.05 / 0.49 | 6 |

All three metadata files record the same executable paths and server version.

- `mysqld` is `/home/runner/work/_temp/mysql-8.4.3-linux-glibc2.17-x86_64-minimal/bin/mysqld`.
- `mysqld --version` reports `Ver 8.4.3 for Linux on x86_64 (MySQL Community Server - GPL)`.
- `lsof` is `/usr/bin/lsof`.
- `ps` is `/usr/bin/ps`.
- `javac` is `/opt/hostedtoolcache/Java_Zulu_jdk/25.0.5-0.3.1/x64/bin/javac`.

## Every repetition

Counts include raw Node parent tests. Group time is the TAP `duration_ms`
recorded to six decimal places. Every command exited 1 with a null signal.
Every repetition reports zero suites, cancelled, skipped and todo tests.

| Runner | Repetition | Tests | Passed | Failed | Cancelled | Skipped | Todo | Group time (ms) |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | 1 | 78 | 69 | 9 | 0 | 0 | 0 | 38004.197128 |
| 1 | 2 | 78 | 69 | 9 | 0 | 0 | 0 | 35763.041393 |
| 1 | 3 | 78 | 69 | 9 | 0 | 0 | 0 | 35061.038499 |
| 1 | 4 | 78 | 69 | 9 | 0 | 0 | 0 | 35882.904695 |
| 1 | 5 | 78 | 69 | 9 | 0 | 0 | 0 | 36962.983168 |
| 1 | 6 | 78 | 69 | 9 | 0 | 0 | 0 | 34826.583678 |
| 1 | 7 | 78 | 69 | 9 | 0 | 0 | 0 | 35010.482574 |
| 1 | 8 | 78 | 69 | 9 | 0 | 0 | 0 | 35002.697983 |
| 1 | 9 | 78 | 69 | 9 | 0 | 0 | 0 | 35030.028914 |
| 1 | 10 | 78 | 69 | 9 | 0 | 0 | 0 | 37042.139431 |
| 2 | 1 | 78 | 69 | 9 | 0 | 0 | 0 | 31940.614695 |
| 2 | 2 | 78 | 69 | 9 | 0 | 0 | 0 | 32011.116387 |
| 2 | 3 | 78 | 69 | 9 | 0 | 0 | 0 | 31408.931748 |
| 2 | 4 | 78 | 69 | 9 | 0 | 0 | 0 | 31172.639985 |
| 2 | 5 | 78 | 69 | 9 | 0 | 0 | 0 | 30328.759626 |
| 2 | 6 | 78 | 69 | 9 | 0 | 0 | 0 | 29776.132798 |
| 2 | 7 | 78 | 69 | 9 | 0 | 0 | 0 | 31019.687079 |
| 2 | 8 | 78 | 69 | 9 | 0 | 0 | 0 | 31357.015170 |
| 2 | 9 | 78 | 69 | 9 | 0 | 0 | 0 | 30779.074781 |
| 2 | 10 | 78 | 69 | 9 | 0 | 0 | 0 | 30921.923226 |
| 3 | 1 | 78 | 68 | 10 | 0 | 0 | 0 | 35595.640777 |
| 3 | 2 | 78 | 69 | 9 | 0 | 0 | 0 | 33075.668819 |
| 3 | 3 | 78 | 69 | 9 | 0 | 0 | 0 | 32386.857634 |
| 3 | 4 | 78 | 69 | 9 | 0 | 0 | 0 | 32474.074675 |
| 3 | 5 | 78 | 68 | 10 | 0 | 0 | 0 | 33147.168944 |
| 3 | 6 | 78 | 69 | 9 | 0 | 0 | 0 | 34906.815970 |
| 3 | 7 | 78 | 69 | 9 | 0 | 0 | 0 | 32087.349921 |
| 3 | 8 | 78 | 69 | 9 | 0 | 0 | 0 | 32636.033148 |
| 3 | 9 | 78 | 69 | 9 | 0 | 0 | 0 | 32222.323389 |
| 3 | 10 | 78 | 69 | 9 | 0 | 0 | 0 | 32564.833769 |

## Client dependency failures

Every repetition has eight failed MySQL leaf results and one downstream
parent assertion failure. The new ambient-server-defaults regression is among
the affected leaves.

| File | Kind | Exact failed test name |
| --- | --- | --- |
| `scripts/worktree-retirement-e2e-mutation.test.mjs` | leaf | `second DROP failure reports partial retirement; retry completes both targets` |
| `scripts/worktree-retirement-e2e-mutation.test.mjs` | leaf | `idle unit+E2E allocation drops both schemas and preserves peer and claims` |
| `scripts/worktree-retirement-e2e-mutation.test.mjs` | leaf | `unrecorded E2E-shaped database stays untouched when only unit is recorded` |
| `scripts/worktree-retirement-mutation.test.mjs` | leaf | `interruption after marker: starts blocked; retry completes same targets` |
| `scripts/worktree-retirement-mutation.test.mjs` | leaf | `idle unit-only allocation drops target schema and preserves peer sentinel` |
| `scripts/worktree-retirement-mutation.test.mjs` | leaf | `missing unit schema counts as already absent; still marks retired` |
| `scripts/worktree-retirement-mutation.test.mjs` | nested leaf | `use a disposable server` |
| `scripts/worktree-retirement-mutation.test.mjs` | leaf | `disposable MySQL serves queries with ambient server defaults` |
| `scripts/worktree-retirement-mutation.test.mjs` | parent | `disposable MySQL cleanup returns only after its server has exited` |

All 240 leaf errors report `mysql failed (127)` and
`error while loading shared libraries: libncurses.so.5: cannot open shared object file: No such file or directory`.
The stack reaches `mysqlCli` from the version query in `startDisposableMysql`
at `scripts/worktree-retirement-disposable-mysql.mjs:184` at this revision.
The server has passed initialization and the readiness wait before that query.
The runner installation therefore needs the client's shared-library dependency.

The cleanup parent continues after `use a disposable server` fails before
assigning `pid`. Its own assertion expects `ESRCH`, then receives
`ERR_INVALID_ARG_TYPE` with `The "pid" argument must be of type number. Received undefined`.
This downstream assertion accounts for the ninth failed raw result in each repetition.

## Idle fixture failures and remaining gate

A one-time reading of the saved TAP files finds 28 passes and two failures of
`verified idle fixture reports idle snapshot not a reservation`.
Both failures occur on runner 3 in
`scripts/worktree-retirement-checkout-processes.test.mjs:177` at this revision.

| Runner | Repetition | PID | Recorded refusal |
| --- | --- | --- | --- |
| 3 | 1 | 15780 | `uncertain checkout backend JVM evidence (pid 15780; working directory unavailable)` |
| 3 | 5 | 24646 | `uncertain checkout backend JVM evidence (pid 24646; working directory unavailable)` |

Each idle assertion expects exit code 0 and receives 1 with
`Refusing worktree database retirement: recorded evidence prevents cleanup.`
The process observation supplies uncertain peer-JVM evidence to the fixture.
Any failure of this named idle case counts as recurring under the story gate.

**Gate remains unmet.** The client dependency failure recurs in all 30
repetitions, and the idle fixture fails twice. Root assigned separate installer
and fixture-isolation repairs. A fresh full-group series after those repairs
remains required before the retirement tests become required in normal CI.
This record preserves the second series' actual results and leaves repair
acceptance and the subsequent gate observation with root.
