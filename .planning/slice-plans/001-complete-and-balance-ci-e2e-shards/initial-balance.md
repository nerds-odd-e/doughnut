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

## Candidate proof and hosted assessment

`./scripts/run.sh pnpm test:ci-proof` passed all 34 cases after the assignment
edit. Actual CLI tests prove the current checkout partition and new-directory
coverage; fixture explicit representatives now follow configured groups, so
the assertion checks ownership without freezing a timing assignment.
No E2E scenario or file eligibility changed. Candidate selected files total
98, with the same 94 timed files and 383 observed cases.

Normal delivery candidate
[43792611347/1](https://github.com/nerds-odd-e/doughnut/actions/runs/43792611347)
at `754ce93f987df343ad7f0375741426af6fdc50a3` completed successfully on
2026-11-16 (D). Connected GitHub run/attempt-job reads confirm final attempt 1,
exact revision and all six E2E jobs successful. Fresh dashboard proof has all
six case parts, the same 94 timed files and 383 passed cases, and zero
failed/other/untimed cases. Per-file case/time sums reconcile with every part
under the published candidate selector. Shards 3/6 have 45/71 cases as proposed.

The preceding normal skill-delivery run is an additional unchanged-assignment
observation:
[43791850516/1](https://github.com/nerds-odd-e/doughnut/actions/runs/43791850516)
at `5cf220d2734a5b30ff0788c2b63985ba94346754`, overall success (C).
It also reconciles six present parts, the identical 94 timed-file set and
383 cases. From B through C to D, product/dependency/scenario sources are
unchanged; only skill/records and the selector/test-fixture assignment differ.
This third baseline was read after it completed, without triggering any run.

| Shard | C recorded / wrapper / job seconds | D recorded / wrapper / job seconds |
| --- | --- | --- |
| 1 | 52.591 / 128 / 248 | 79.735 / 153 / 295 |
| 2 | 125.527 / 211 / 320 | 120.576 / 210 / 349 |
| 3 | 59.055 / 127 / 278 | 117.891 / 215 / 363 |
| 4 | 127.177 / 220 / 343 | 101.839 / 167 / 272 |
| 5 | 107.834 / 213 / 337 | 79.554 / 144 / 259 |
| 6 | 197.978 / 314 / 442 | 91.019 / 170 / 278 |

C's shard 6 finished last at 02:30:41Z, 442 seconds after the first E2E
job began. D's shard 3 finished last at 02:36:48Z, 363 seconds after its
02:30:45Z start. Candidate longest wrapper time was 215 seconds versus
341/257/314 for A/B/C; longest whole job was 363 versus 476/372/442.
The first-start-to-last-finish intervals were 363 versus 476/373/442.
Candidate maximum recorded part time is 120.576 seconds, versus
201.399/140.997/197.978. Total recorded time is 590.614 seconds, close to
B's 590.062, while A/C totals are 691.103/670.162: file costs vary materially.

Candidate jobs retain `ubuntu-latest`, built-target Chrome, the same discovery,
CI tags and retry policy. Its shard-three log reports Ubuntu 24.04.5 image
`20261111.127.1`, Cypress 16.3.0 and headless Chrome 157, matching B's sampled
image/browser conditions. As with the baseline samples, this does not assert
identical conditions across all runners.

**Decision: retain the supported assignment.** The largest folder now runs
alone; both pre-edit per-file forecasts lower the six-way maximum, complete
coverage survives, and the normal candidate run's wrapper and last-finish
observations support the change without regression. The candidate is only
nine seconds below the fastest baseline whole job. One candidate sample,
variable file costs, setup and runner conditions do not establish a causal or
repeatable numerical saving. The observations support this initial grouping;
the skill remains the means to reassess it as the suite changes. No timing
revert or further experiment is warranted by this bounded assessment.

No product edits followed the passing candidate run. The unchanged actual-CLI
partition proof is reused; a records-only assessment update needs no test rerun.

Raw dashboard/job evidence, throwaway analysis, six-row calculations and the
dispositioned consumer trace remain in `/tmp/seed130-initial-balance/`.
No profiling, local E2E or additional job ran in this assessment. Candidate
publication and its normal CI were coordinator-owned. Initial analysis used
about five active minutes; post-CI assessment used about three more.
