# Initial shard balance assessment

Invoked the delivered `donut-rebalance-e2e-shards` skill on 2026-11-16 from
`5cf220d2734a5b30ff0788c2b63985ba94346754`, repository
`nerds-odd-e/doughnut`, branch
`codex/keep-all-e2e-specs-covered-and-rebalance-the-six`.

## Baseline before timing edits

Fresh dashboard status at 02:24 UTC still selected failed main attempt
`43726030546/1`; it supplied no comparable success baseline. Connected GitHub
run discovery identified these completed normal delivery attempts:

- [43789649141/1](https://github.com/nerds-odd-e/doughnut/actions/runs/43789649141),
  `c594b3d5a135462865708ce7d3aedddaf9a074aa`, overall frontend failure but
  all six E2E jobs and proof successful (A).
- [43791042394/1](https://github.com/nerds-odd-e/doughnut/actions/runs/43791042394),
  `82efd34b6e0e348dbbb62ceb27454b854966277a`, overall success (B).

Their run records confirm final attempt 1 and the exact revision. Fresh
dashboard proof reads for each identity both contain six present case parts,
94 timed files, 383 passed cases and zero failed/other/untimed. Per-file sums
and cases reconcile to every part using each revision's actual selection.
Only frontend fixture/planning records differ between A and B; application,
E2E scenarios, dependencies, discovery, tags and owned runner are unchanged.
Current HEAD adds only the private skill and records after B.

All E2E jobs name `ubuntu-latest`, built-target Chrome and the same run-mode
retry policy. Actual shard-six logs show Ubuntu 24.04.5, Cypress 16.3.0 and
headless Chrome 157 in both; runner images are `20261111.370.1` (A) and
`20261111.127.1` (B). These are sampled image observations, not a claim that
all runners have identical images or performance. G2's missing condition is
now observed and its variability remains part of the assessment.

The historical 2026-11-14 attempts in `skill-validation.md` are not fitted as
current full-workload observations: they lack the newly covered file and
precede dependency changes. Its cost was unknown historically, not zero.
Both current observations include learning-session: 8.559 / 5.872 seconds.
The four selected CLI files without cases carry `@ignore`; no eligible current
file is assigned unknown or zero cost.

## Supported candidate

CLI is the heaviest folder at 155.746 / 108.372 recorded seconds. Moving it
wholesale to an occupied lighter shard alone makes the maximum worse. Isolate
CLI in explicit shard 3, replacing `note_creation_and_update`; that folder
then belongs to the exact remainder (6). Keep the other four explicit groups.

Every table entry uses the same observed per-file costs before and after.
File cells give selected/timed; case counts are unchanged overall.

| Shard | Current files; cases | Candidate files; cases | A current → candidate seconds | B current → candidate seconds |
| --- | --- | --- | --- | --- |
| 1 | 11/11; 40 | 11/11; 40 | 98.495 → 98.495 | 52.769 → 52.769 |
| 2 | 17/17; 97 | 17/17; 97 | 123.909 → 123.909 | 118.875 → 118.875 |
| 3 | 6/6; 42 | 20/16; 45 | 57.012 → 155.746 | 60.019 → 108.372 |
| 4 | 15/15; 87 | 15/15; 87 | 105.223 → 105.223 | 108.933 → 108.933 |
| 5 | 16/16; 43 | 16/16; 43 | 105.065 → 105.065 | 108.469 → 108.469 |
| 6 | 33/29; 74 | 19/19; 71 | 201.399 → 102.665 | 140.997 → 92.644 |

Maximum recorded-time estimates: 201.399 → 155.746 seconds (A), and
140.997 → 118.875 seconds (B). This is a forecast, not measured speedup.

| Shard | A wrapper / whole-job seconds | B wrapper / whole-job seconds |
| --- | --- | --- |
| 1 | 205 / 386 | 127 / 245 |
| 2 | 212 / 350 | 201 / 349 |
| 3 | 122 / 280 | 129 / 246 |
| 4 | 193 / 362 | 181 / 287 |
| 5 | 190 / 356 | 207 / 314 |
| 6 | 341 / 476 | 257 / 372 |

Shard 6 finished last at 02:09:34Z (A) and 02:21:39Z (B). The E2E interval
from first job start to last finish was 476 / 373 seconds. Setup, browser,
external-call and runner variation remain outside per-file estimates.

## Candidate proof and remaining observation

`./scripts/run.sh pnpm test:ci-proof` passed all 34 cases after the assignment
edit. Actual CLI tests prove the current checkout partition and new-directory
coverage; fixture explicit representatives now follow configured groups, so
the assertion checks ownership without freezing a timing assignment.
No E2E scenario or file eligibility changed. Candidate selected files total
98, with the same 94 timed files and 383 observed cases.

Coordinator normal delivery CI must observe the candidate before retention.
Compare all six exact-attempt case/file totals, recorded times, wrapper/job
times and last finish with A/B. Retain only a supported elapsed result; an
ineffective or regressing assignment is reverted while keeping complete
coverage and the skill. No candidate elapsed saving is established yet.

Raw dashboard/job evidence, throwaway analysis, six-row calculations and the
dispositioned consumer trace remain in `/tmp/seed130-initial-balance/`.
No profiling, local E2E, additional job or publication ran in this assessment.
