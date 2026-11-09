# CI proof from the normal script commands

Identity `SEED-111#run-uncovered-script-tests-in-ci`.
Observed on 2026-11-09 from the completed normal `donut CI` attempt.
Revision `e63aa60bb81f93549c747cc52c913171542cd3c3`.
Run [42588658999](https://github.com/nerds-odd-e/doughnut/actions/runs/42588658999), attempt 1.
Artifact `ci-proof-summary-attempt-1`, ID `13659874120`, schema version 1.

## Acquisition and identity

The downloaded summary names the expected repository, run, attempt and full revision.
All 13 jobs completed successfully. The root coordinator retained the
exact native `await-revision` success for this candidate.

```sh
gh api 'repos/nerds-odd-e/doughnut/actions/runs/42588658999/artifacts?per_page=100' > /tmp/seed111-final-ci/artifacts.json
gh api repos/nerds-odd-e/doughnut/actions/artifacts/13659874120/zip > /tmp/seed111-final-ci/summary.zip
unzip -q -o /tmp/seed111-final-ci/summary.zip -d /tmp/seed111-final-ci/summary
gh api 'repos/nerds-odd-e/doughnut/actions/runs/42588658999/attempts/1/jobs?per_page=100' > /tmp/seed111-final-ci/jobs.json
```

The archive contains `ci-proof-summary.json`. The retained local source files are
`/tmp/seed111-final-ci/summary/ci-proof-summary.json` and
`/tmp/seed111-final-ci/jobs.json`.
The local report baseline is `/tmp/seed111-slice2-local-proof.json`.

## Scripts aggregate

| Passed | Failed | Other | Untimed | Recorded duration (ms) | Files | Present parts |
| ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 465 | 0 | 0 | 0 | 145707.03825999997 | 91 | 13 |

Every part is present and has a file breakdown. All parts have zero failed, other
and untimed checks. The Bash producer is `Other-unit-tests` in the main workflow.
Parts 2–13 are published by `.github/lint_and_generated_types/action.yml`.

| Part | Producer command | Passed | Recorded duration (ms) |
| ---: | --- | ---: | ---: |
| 1 | `bash scripts/test/run_all_script_tests.sh` | 22 | 36263.25799999999 |
| 2 | `test:path-routing` | 17 | 90.555071 |
| 3 | `test:api-summary` | 4 | 14.291407 |
| 4 | `test:workspace-syncpack-lifecycle` | 2 | 9.791633 |
| 5 | `test:log-utils` | 6 | 22.355246 |
| 6 | `test:sut-healthcheck` | 16 | 1249.268699 |
| 7 | `test:sut-start` | 85 | 19243.285351 |
| 8 | `test:browser-worktree-isolation` | 137 | 17243.353786999996 |
| 9 | `test:ci-proof` | 25 | 1696.9747599999998 |
| 10 | `test:backend-test-worktree` | 85 | 12661.300706 |
| 11 | `test:development-stack` | 23 | 1621.867288 |
| 12 | `test:worktree-retirement` | 39 | 55583.200818000005 |
| 13 | `test:publication-receiver` | 4 | 7.535494000000001 |

## All 18 added files

Each file appears exactly once. Its passed count equals the local report baseline.
The table records zero for an omitted file-level `untimed` field, following
`scripts/ci/ci-proof-figures.mjs`, whose `publishedFigures` omits zero untimed counts.
The entry and all 13 parts explicitly report `untimed=0`. Every file has a finite
recorded duration. These are the durations from this CI attempt.

| File | Passed | Failed | Other | Untimed | Recorded duration (ms) |
| --- | ---: | ---: | ---: | ---: | ---: |
| `scripts/dev-healthcheck.test.mjs` | 4 | 0 | 0 | 0 | 39.394799 |
| `scripts/isolated-cypress-openai-mock-cancel.test.mjs` | 2 | 0 | 0 | 0 | 607.330521 |
| `scripts/isolated-cypress-openai-mock-failure.test.mjs` | 1 | 0 | 0 | 0 | 294.374989 |
| `scripts/isolated-cypress-openai-mock-ownership.test.mjs` | 2 | 0 | 0 | 0 | 450.918496 |
| `scripts/isolated-cypress-openai-mock.test.mjs` | 7 | 0 | 0 | 0 | 171.69168500000004 |
| `scripts/isolated-cypress-owning-health.test.mjs` | 2 | 0 | 0 | 0 | 619.019134 |
| `scripts/isolated-openai-mock.test.mjs` | 2 | 0 | 0 | 0 | 607.141104 |
| `scripts/isolated-service-mocks.test.mjs` | 4 | 0 | 0 | 0 | 1817.277881 |
| `scripts/local-runtime-target.test.mjs` | 3 | 0 | 0 | 0 | 2.753347 |
| `scripts/profiling/verify-publication-receiver.test.mjs` | 4 | 0 | 0 | 0 | 7.535494000000001 |
| `scripts/sut-owner-application-group.test.mjs` | 2 | 0 | 0 | 0 | 540.001528 |
| `scripts/worktree-reset-isolation-harness-openai-mock.test.mjs` | 1 | 0 | 0 | 0 | 15.077094 |
| `scripts/worktree-reset-isolation-harness-wikidata-mock.test.mjs` | 2 | 0 | 0 | 0 | 29.290397999999996 |
| `scripts/worktree-retirement-checkout-processes.test.mjs` | 9 | 0 | 0 | 0 | 2527.949729 |
| `scripts/worktree-retirement-e2e-mutation.test.mjs` | 3 | 0 | 0 | 0 | 20173.738398 |
| `scripts/worktree-retirement-evidence.test.mjs` | 11 | 0 | 0 | 0 | 2352.043342 |
| `scripts/worktree-retirement-mutation.test.mjs` | 8 | 0 | 0 | 0 | 29829.031579000002 |
| `scripts/worktree-retirement.test.mjs` | 8 | 0 | 0 | 0 | 700.4377699999999 |

The 18 files contribute 75 passed leaf checks. Their file durations sum to
approximately 60,785.007288 ms.
The MySQL E2E mutation file contributes 3 passed checks and the mutation file
contributes 8. Their real-server setup and version assertion passed in the normal
Linux command. The configured installer targets MySQL 8.4.3, also observed in the
accepted [30-repetition runner series](runner-series-clean.md).

## Retained boundaries and rules

`scripts/test/application-release.test` appears exactly once with one passed check
and 9720.091999999999 ms. Its 13 delegated Node files retain the Bash counting boundary.
All 91 paths under the scripts entry start with `scripts/`. The summary entries
contain no `cli/tests` entry. The browser command reported 140 local checks, while
its scripts part contains 137 because the three checks in the existing
`e2e_test/config/composeCypressPluginEvents.test.mjs` file are outside this entry.

The existing 25 checks in `test:ci-proof` passed locally and in CI part 9.
Failure propagation, failed recorded durations, load-failure counting and missing
parts reuse the accepted wrapper/reporter observations in
`scripts/ci/ci-proof-outcomes-node.test.mjs` and the shared outcome fixtures.
This attempt supplies the passing-file and complete-part observations.

## Measured job durations

All timestamps below are the UTC job start and completion values from GitHub.
Elapsed job execution excludes queue wait. Each E2E row identifies its exact job.

| Job | Started | Completed | Duration (s) |
| --- | --- | --- | ---: |
| [Lint 129064552631](https://github.com/nerds-odd-e/doughnut/actions/runs/42588658999/job/129064552631) | 2026-11-09T08:48:48Z | 2026-11-09T08:54:57Z | 369 |
| [E2E 129064552673](https://github.com/nerds-odd-e/doughnut/actions/runs/42588658999/job/129064552673) | 2026-11-09T08:48:48Z | 2026-11-09T08:55:41Z | 413 |
| [E2E 129064552675](https://github.com/nerds-odd-e/doughnut/actions/runs/42588658999/job/129064552675) | 2026-11-09T08:48:49Z | 2026-11-09T08:54:39Z | 350 |
| [E2E 129064552706](https://github.com/nerds-odd-e/doughnut/actions/runs/42588658999/job/129064552706) | 2026-11-09T08:48:48Z | 2026-11-09T08:56:16Z | 448 |
| [E2E 129064552749](https://github.com/nerds-odd-e/doughnut/actions/runs/42588658999/job/129064552749) | 2026-11-09T08:48:48Z | 2026-11-09T08:53:36Z | 288 |
| [E2E 129064552768](https://github.com/nerds-odd-e/doughnut/actions/runs/42588658999/job/129064552768) | 2026-11-09T08:48:48Z | 2026-11-09T08:53:11Z | 263 |
| [E2E 129064552844](https://github.com/nerds-odd-e/doughnut/actions/runs/42588658999/job/129064552844) | 2026-11-09T08:48:48Z | 2026-11-09T08:55:10Z | 382 |

Lint took 369 seconds (6 minutes 9 seconds). The slowest E2E shard, job
`129064552706`, took 448 seconds (7 minutes 28 seconds). Lint was 79 seconds
shorter in this attempt. The measured duration objective holds for this attempt.

## Acceptance scope

The artifact proves the normal CI commands publish the 18 files with matching
counts, recorded durations and all 13 parts. Root owns the source-commitment
records, G2 disposition and slice completion. Slice 3 still owns comparison with
Intent for the same attempt and revision.
