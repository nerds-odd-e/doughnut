# Publish Doughnut test execution times in Intent

Work item: **SEED-106#publish-ci-test-execution-times**.
Source: [refined story](../../seeds/SEED-106-publish-ci-test-execution-times.md#publish-ci-test-execution-times).

## Goal and scope

For a judged `donut CI` attempt, Intent's CI proof for `e2e_test/features` and
`frontend/tests` shows cumulative recorded test execution time (`durationMs`)
and a per-file breakdown that agree with the runner reports and the downloaded
`ci-proof-summary-attempt-<n>` artifact. Missing shards stay visible; failed
checks still contribute available timings; unrecorded durations stay untimed.

**Included:** refresh skill-owned collector copies into `scripts/ci/`; extend
`ci-proof-outcomes.mjs` to emit `--times` lines; wire
`.github/ci_proof_fragments` with `--times` and `--by-file`; update
`docs/ci-proof-summary.md`.

**Excluded:** `scripts` / `infra` / backend proof entries; wall-clock job or
setup time; Intent/dashboard product changes; CI-adapter publishing.

## Architecture

- **PFE:** Reuse the installed Open Dough CI proof recipe. Change the existing
  project conversion (`scripts/ci/ci-proof-outcomes.mjs`) and the fragment
  composite action. Refresh collector modules as **unchanged copies** of
  `.agents/skills/dough-test-optimization/scripts/ci-proof-{summary,contract,fragments,collect,figures}.mjs`
  (figures is currently missing; project copies lack `--times` / `--by-file`).
  Do not fork collector behavior.
- **Boundary:** Runner-specific conversion and workflow wiring stay
  project-owned; collector math stays skill-owned copies.
- **ADRs:** No Accepted ADR constrains this path. Loud conversion failure
  continues to match [ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md).
  [ADR 0007](../../../docs/adrs/0007-environments-and-isolation-accepted.md)
  is untouched (CI remains an execution location).
- **North Star:** No new topic; this is CI publishing plumbing, not notebook
  domain structure.

## Key examples → proof

| Promise (story key example) | Slice | Proof |
| --- | --- | --- |
| Timed Vitest entry + file breakdown | 2, 3 | `ci-proof-outcomes.test.mjs` (Vitest durations → seconds); local `fragment`/`summarize` on fixture outcomes+times with `--by-file`; slice 3 CI artifact / Intent |
| Timed Cucumber entry + file breakdown | 2, 3 | same tests for Cucumber final-attempt step duration sums; slice 3 |
| Missing shard stays missing; present parts keep times | 2 | collector already encodes missing parts; fixture summarize with one part absent still carries other parts' `durationMs` |
| Failed tests still timed | 2 | outcomes test: failed Vitest/Cucumber checks with durations appear in times |
| Untimed load-failed Vitest file | 2 | outcomes test: failed empty `assertionResults` → outcome, no times line; fragment marks untimed |

## Decisive premises

| Premise | Consuming operation | Observation | Result |
| --- | --- | --- | --- |
| Installed collector supports `--times` and `--by-file` and emits `durationMs` / files | Structure slice copies those modules | Read `.agents/skills/dough-test-optimization/scripts/ci-proof-{summary,fragments,figures,collect,contract}.mjs` (2026-11-07); `fragment` option list includes `times` and flag `by-file` | Settled true |
| Project collector copies are stale (no times/by-file; no figures module) | Structure refresh | `diff` skill scripts vs `scripts/ci/`; `scripts/ci/ci-proof-figures.mjs` absent (2026-11-07) | Settled true |
| Vitest JSON assertions expose optional `duration` in milliseconds | Conversion writes `--times` seconds as `duration/1000` | Vitest JSON reporter docs / `JsonAssertionResult.duration?: Milliseconds` (Context7 `/vitest-dev/vitest`, 2026-11-07). Local Vitest run in this workspace failed on tooling; converter tests use the documented shape | Settled for field shape; absent duration → untimed |
| Cucumber `testStepResult.duration` is `{seconds, nanos}` | Conversion sums final-attempt step durations | `@cucumber/messages` Duration schema (Context7 `/cucumber/messages`, 2026-11-07) | Settled for field shape |
| `--times` lines are seconds (×1000 → `durationMs`) | Conversion unit choice | installed `ci-proof-fragments.mjs` `checkTimes` | Settled true |
| Intent already displays optional `durationMs` and `--by-file` breakdowns | Story excludes Intent code changes | Recipe [ci-proof-summary.md](../../../.agents/skills/dough-test-optimization/references/ci-proof-summary.md) | Settled as publishing contract; live Intent agreement is slice 3 |

## Slices

### 1. Collector copies accept timed by-file fragments
Type: Structure
Status: planned
Proof: After copying the five skill modules into `scripts/ci/`,
`node scripts/ci/ci-proof-summary.mjs fragment --outcomes <fixture> --times <fixture> --by-file --entry frontend/tests --part 1 --of 2 --attempt 1 --revision deadbeef --out <tmp>`
writes a fragment whose JSON includes `durationMs` and `files` (and existing
`pnpm test:ci-proof` / collector consumers still load).

Structure: Replace `scripts/ci/ci-proof-{summary,contract,fragments,collect}.mjs`
with the installed skill copies and add `ci-proof-figures.mjs` beside them,
unchanged. No workflow or outcomes change yet. Enables slice 2's `--times` /
`--by-file` wiring.

### 2. Vitest and Cucumber proof fragments carry durations and file breakdown
Type: Behavior
Status: planned
Proof: Extend `scripts/ci/ci-proof-outcomes.test.mjs` for the five key-example
conversions; extend `scripts/ci/ci-workflow.test.mjs` (or the fragment action
fixture) so `.github/ci_proof_fragments` invokes `fragment` with `--times` and
`--by-file`; run `pnpm test:ci-proof`. Optional local
`fragment`+`summarize` on the new fixtures shows entry `durationMs` and file
paths.

Behavior: Pre — producer jobs still write Vitest JSON / Cucumber messages as
today. Trigger — convert report → outcomes + times, then
`fragment --outcomes … --times … --by-file`. Result — each present part's
fragment carries summed `durationMs` when times exist, repository-relative
file breakdown, failed checks' available times, and untimed load-failed Vitest
files without invented durations. Update `docs/ci-proof-summary.md` to describe
timing and breakdown. Missing-shard visibility remains the collector's existing
rule.

### 3. Judged CI attempt shows timed proof in the summary and Intent
Type: Behavior
Status: planned
Proof: Early probe after slices 1–2 are on the judged workflow: download
`ci-proof-summary-attempt-<n>` for one completed attempt; confirm both entries
carry `durationMs` and file breakdown for present parts, and that Intent's CI
proof for that attempt agrees on entry totals and one file from each (story
acceptance). Failure stops here and revises the plan; no further product scope
is assumed.

Behavior: Pre — slices 1–2 published on the revision the attempt builds.
Trigger — the attempt finishes and the developer opens that attempt in Intent.
Result — cumulative times and file breakdown match the artifact and runner
reports for the checked files; a missing part remains visible if one exists on
that attempt.

## Current decisions

- Cucumber scenario time is the sum of `testStepResult.duration` on the **final**
  attempt only (same attempt the status rule already uses).
- Vitest times convert milliseconds → seconds for `--times`; do not invent
  values when `duration` is absent.
- Wall-clock CI cost stays deferred; published figures are recipe `durationMs`.

## Concern evidence

- Slice-plan refinement: not needed — two commit-sized delivery slices plus one
  authorized early probe, one proof loop per Behavior, Structure immediately
  before its Behavior, no competing outcomes or special-case accumulation.
- Remaining slice-specific concerns: none identified. Slice 3 is an authorized
  early probe for live CI/Intent agreement; its failure revises the plan rather
  than blocking preparation readiness.
