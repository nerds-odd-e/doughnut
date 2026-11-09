# Clean CI runner series after dependency and fixture repairs

Identity `SEED-111#run-uncovered-script-tests-in-ci`.
Observed on 2026-11-09 from the completed `donut CI` attempt and saved artifacts.
Revision `a84e1ee2928e2db1d2418ec8b2023bdf1bb09bbf`.
Run [42583951600](https://github.com/nerds-odd-e/doughnut/actions/runs/42583951600), attempt 1.

## Provenance and selection

Three fresh `ubuntu-latest` matrix jobs each ran the shared Lint environment,
generated-types check and `pnpm lint:all`, followed by ten sequential full-group
invocations. At the observed revision, `.github/script_runner_probe/action.yml`
uses `.github/lint_environment`, as the Lint action does, and invokes all 18
files together through one `node --test --test-reporter=tap` process per repetition.
The metadata file lists match the scoped seed list exactly on all three runners.

| Runner | Job | Runner name | Artifact name |
| --- | --- | --- | --- |
| 1 | [129049138435](https://github.com/nerds-odd-e/doughnut/actions/runs/42583951600/job/129049138435) | `GitHub Actions 1000123835` | `script-runner-probe-attempt-1-runner-1` |
| 2 | [129049138354](https://github.com/nerds-odd-e/doughnut/actions/runs/42583951600/job/129049138354) | `GitHub Actions 1000123831` | `script-runner-probe-attempt-1-runner-2` |
| 3 | [129049138401](https://github.com/nerds-odd-e/doughnut/actions/runs/42583951600/job/129049138401) | `GitHub Actions 1000123836` | `script-runner-probe-attempt-1-runner-3` |

Authenticated `gh` downloaded all three artifacts with one command.
The saved directories under `/tmp/seed111-probe-42583951600/` each contain
`metadata.json`, `results.json` and ten `repetition-<n>.tap` files.
`jobs.json` records the three successful jobs and their distinct runner names.
These paths are temporary recovery coordinates. The run, attempt, revision,
job IDs and artifact names retain the observation provenance.

## Runner observations

All three runners record image `ubuntu24`, image version `20261101.360.1`,
hostname `runnervm7aw5e` and four logical CPUs. The distinct runner names and
job IDs identify the separate fresh jobs. Memory is recorded in bytes before
the repetitions. Load is the recorded 1/5/15-minute average.

| Runner | Job start (UTC) | Job end (UTC) | Total memory | Free memory | Load | MySQL install (s) |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | 2026-11-09T08:09:52Z | 2026-11-09T08:22:24Z | 16760344576 | 14737584128 | 2.28 / 1.25 / 0.56 | 9 |
| 2 | 2026-11-09T08:09:52Z | 2026-11-09T08:21:06Z | 16759308288 | 14643933184 | 1.55 / 0.87 / 0.39 | 7 |
| 3 | 2026-11-09T08:09:52Z | 2026-11-09T08:21:09Z | 16760344576 | 14736764928 | 2.2 / 1.26 / 0.58 | 8 |

The metadata files agree on executable paths and the MySQL server version.

- `mysqld` is `/home/runner/work/_temp/mysql-8.4.3-linux-glibc2.17-x86_64-minimal/bin/mysqld`.
- `mysqld --version` reports `/home/runner/work/_temp/mysql-8.4.3-linux-glibc2.17-x86_64-minimal/bin/mysqld  Ver 8.4.3 for Linux on x86_64 (MySQL Community Server - GPL)`.
- `lsof` is `/usr/bin/lsof`.
- `ps` is `/usr/bin/ps`.
- `javac` is `/opt/hostedtoolcache/Java_Zulu_jdk/25.0.5-0.3.1/x64/bin/javac`.

## Every repetition

The TAP summaries and `results.json` agree for all 30 repetitions.
Each command exited 0 with a null signal and no spawn error. Counts include
Node parent tests. Group time is TAP `duration_ms`. Wall time is the recorded
invocation elapsed time. Every row has zero failed, cancelled, skipped and todo
tests, and every TAP has the named idle case passing.

| Runner | Repetition | Tests | Passed | Failed | Cancelled | Skipped | Todo | Group time (ms) | Wall time (ms) |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | 1 | 78 | 78 | 0 | 0 | 0 | 0 | 36659.740159 | 36696.585815 |
| 1 | 2 | 78 | 78 | 0 | 0 | 0 | 0 | 35957.285871 | 35996.968882 |
| 1 | 3 | 78 | 78 | 0 | 0 | 0 | 0 | 37080.726128 | 37123.224579 |
| 1 | 4 | 78 | 78 | 0 | 0 | 0 | 0 | 33790.500188 | 33833.045141 |
| 1 | 5 | 78 | 78 | 0 | 0 | 0 | 0 | 36121.784113 | 36158.755025 |
| 1 | 6 | 78 | 78 | 0 | 0 | 0 | 0 | 35028.990959 | 35071.652038 |
| 1 | 7 | 78 | 78 | 0 | 0 | 0 | 0 | 36009.589050 | 36048.136498 |
| 1 | 8 | 78 | 78 | 0 | 0 | 0 | 0 | 35695.887576 | 35733.210286 |
| 1 | 9 | 78 | 78 | 0 | 0 | 0 | 0 | 34733.247613 | 34771.647131 |
| 1 | 10 | 78 | 78 | 0 | 0 | 0 | 0 | 34908.093457 | 34948.960839 |
| 2 | 1 | 78 | 78 | 0 | 0 | 0 | 0 | 32196.294200 | 32226.091851 |
| 2 | 2 | 78 | 78 | 0 | 0 | 0 | 0 | 32271.767758 | 32301.731719 |
| 2 | 3 | 78 | 78 | 0 | 0 | 0 | 0 | 32342.305623 | 32373.688438 |
| 2 | 4 | 78 | 78 | 0 | 0 | 0 | 0 | 32719.838691 | 32753.569238 |
| 2 | 5 | 78 | 78 | 0 | 0 | 0 | 0 | 31989.623937 | 32020.537159 |
| 2 | 6 | 78 | 78 | 0 | 0 | 0 | 0 | 31945.527969 | 31974.997428 |
| 2 | 7 | 78 | 78 | 0 | 0 | 0 | 0 | 33366.170328 | 33396.140247 |
| 2 | 8 | 78 | 78 | 0 | 0 | 0 | 0 | 32266.438624 | 32294.850154 |
| 2 | 9 | 78 | 78 | 0 | 0 | 0 | 0 | 32052.647232 | 32083.939907 |
| 2 | 10 | 78 | 78 | 0 | 0 | 0 | 0 | 31889.239859 | 31918.950990 |
| 3 | 1 | 78 | 78 | 0 | 0 | 0 | 0 | 32503.408394 | 32531.152942 |
| 3 | 2 | 78 | 78 | 0 | 0 | 0 | 0 | 33184.119190 | 33213.636922 |
| 3 | 3 | 78 | 78 | 0 | 0 | 0 | 0 | 32973.606120 | 33001.704297 |
| 3 | 4 | 78 | 78 | 0 | 0 | 0 | 0 | 30987.144242 | 31016.770239 |
| 3 | 5 | 78 | 78 | 0 | 0 | 0 | 0 | 31538.623410 | 31564.354722 |
| 3 | 6 | 78 | 78 | 0 | 0 | 0 | 0 | 31733.181152 | 31758.090064 |
| 3 | 7 | 78 | 78 | 0 | 0 | 0 | 0 | 32021.571997 | 32057.935012 |
| 3 | 8 | 78 | 78 | 0 | 0 | 0 | 0 | 32615.128738 | 32651.961010 |
| 3 | 9 | 78 | 78 | 0 | 0 | 0 | 0 | 31580.537315 | 31605.969458 |
| 3 | 10 | 78 | 78 | 0 | 0 | 0 | 0 | 31770.934007 | 31796.101352 |

## Gate result and limits

All 30 repetitions pass all 78 raw Node tests. This is 2,340 passed test
observations with zero failures, cancelled, skipped or todo outcomes.
The exact idle case `verified idle fixture reports idle snapshot not a reservation`
passes 30/30. Every TAP also records the three E2E MySQL mutation cases,
`disposable MySQL cleanup returns only after its server has exited` and
`disposable MySQL serves queries with ambient server defaults` passing.
The E2E mutation fixture checks its own disposable server version against 8.4.

The first and second series remain recorded in [runner-series.md](runner-series.md)
and [runner-series-after-mysql-isolation.md](runner-series-after-mysql-isolation.md).
The current candidate includes the disposable-server isolation, MySQL client
library acquisition and idle-fixture evidence-isolation repairs recorded there
and in the plan. This fresh series follows those repairs and has no remaining
recurring failure. It satisfies the 30-repetition, three-fresh-runner gate.

This observation covers the recorded runner image, candidate, setup and file
selection. It supplies the runner gate before the files become required.
Slice 2 owns required-command wiring and published per-file CI proof.

## Acceptance reading

The one-time whole-checkout search covered `Script-runner-probe`,
`script_runner_probe`, `mysql-install-seconds`, `elapsed-seconds`, `PROBE_RUNNER`,
`PROBE_MYSQL_INSTALL_SECONDS` and `script-runner-probe`, including hidden paths
and excluding Git internals, installed dependencies and nested worktrees.
It found only the dated series records and execution history under this plan.
Those matches retain the actual observed action and artifact names for provenance.
The live CI workflow and actions have zero remaining references to the retired
probe and its exclusive timing outputs. This is a reading captured during
acceptance. The retained Lint setup and MySQL acquisition/version-check commands
match the accepted hosted candidate under the action validation command.

The consumer trace at `/tmp/seed111-clean-series-consumers.md` records each
located consumer's disposition. The retained process/MySQL behavior reuses
this series. Workflow proof and action syntax were checked after the edits.

## Observation commands

- `gh run download 42583951600 --repo nerds-odd-e/doughnut --pattern 'script-runner-probe-attempt-1-runner-*' --dir /tmp/seed111-probe-42583951600`
- `gh api 'repos/nerds-odd-e/doughnut/actions/runs/42583951600/attempts/1/jobs?per_page=100' --jq '[.jobs[] | select(.name | startswith("Script runner probe")) | {id,name,conclusion,started_at,completed_at,runner_name}]'`
- `./scripts/run.sh python3 /tmp/seed111-record-clean-series.py` validated the three metadata files, all 30 results and all 30 TAPs before generating this record.
- `./scripts/run.sh pnpm test:ci-proof` passed 25/25 after removal edits.
- `./scripts/run.sh node /tmp/seed111-validate-probe-removal.mjs` parsed YAML and Bash and compared the retained Lint setup and MySQL acquisition/version checks with the accepted candidate.
- `scripts/check_diff_whitespace.sh` passed.
