---
id: SEED-106
status: active
planted: 2026-11-07
scope: story
---

# Publish Doughnut test execution times in Intent

<a id="publish-ci-test-execution-times"></a>

### Publish Doughnut test execution times in Intent

**Identity:** SEED-106#publish-ci-test-execution-times
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/082-publish-ci-test-execution-times/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"b177ba02197b8f179593530cbb2623dc5e6f28402777c566d3896d1a830c99e7","plan":"9df960aa425efb5b2ff0da2e9eb38adfe6c975f47443093aa71f1bfc6165ae16"}}
```

**Plan:** [082-publish-ci-test-execution-times](../slice-plans/082-publish-ci-test-execution-times/PLAN.md)

**Beneficiary:** A Doughnut developer investigating which Vitest and Cucumber
checks consume recorded test execution time on a CI attempt.

**Goal:** Intent shows the cumulative recorded test execution time
(`durationMs`) and per-file breakdown for the Vitest and Cucumber proof from a
judged CI attempt, matching the runner reports and the downloaded
`ci-proof-summary-attempt-<n>` artifact.

**Scope — required behavior:**

- Keep the existing CI proof path: Vitest JSON and Cucumber messages → project
  conversion → timed fragments → collector summary for entries
  `e2e_test/features` (6 parts) and `frontend/tests` (2 parts), in
  `intent.locations` order.
- Refresh this project's copied CI proof collector modules from the installed
  [CI proof recipe](../../.agents/skills/dough-test-optimization/references/ci-proof-summary.md)
  so `fragment` accepts `--times` and `--by-file` and `summarize` retains
  `durationMs` and file breakdown (schema version stays 1). Copy them
  unchanged; do not fork collector behavior in this story.
- Extend `scripts/ci/ci-proof-outcomes.mjs` (and its tests) so each converted
  check can also emit a times line `<seconds><TAB><repo-relative path>` for
  the same labels as today's outcomes:
  - Vitest: one times line per assertion that records `duration` (milliseconds
    in Vitest's JSON report → seconds for `--times`); a load-failed file with
    no assertions stays a failed outcome with no times line (untimed).
  - Cucumber: one times line per scenario on its final attempt, summing that
    attempt's step `testStepResult.duration` values; status rules stay as
    today.
- Wire `.github/ci_proof_fragments` to pass `--times` and `--by-file` when
  writing each part's fragment.
- Preserve today's gap rules: a missing shard stays visible as missing; a
  shard that reported failed tests still contributes its available timings; a
  broken conversion fails its producer step.
- Update `docs/ci-proof-summary.md` so the published summary's timing and
  file-breakdown behavior match what CI writes.

**Scope — deferred promises** (not built or verified here):

- Proof entries or timings for `scripts`, `infra`, or backend tests.
- Wall-clock job, setup, browser-boot, or shard-queue time (Intent's
  `durationMs` is summed recorded test execution time, not CI wall time).
- Changes to Intent/dashboard product code (it already reads optional
  `durationMs` and `--by-file` breakdowns when present).
- Publishing timings through a CI adapter path (this project uses the judged
  GitHub Actions workflow).

**Scope — rejection constraints:**

- Do not invent durations the runner did not record; those checks stay
  untimed and still count.
- Do not publish machine-absolute paths; labels stay repository-relative
  inside their entry, as the recipe requires.

**Key examples:**

1. **Timed Vitest attempt.** Pre: both Frontend-unit-tests shards write Vitest
   JSON where assertions record durations, including at least one failed test
   and one file under `frontend/tests`. Trigger: the judged workflow attempt
   finishes and the collector uploads `ci-proof-summary-attempt-<n>`. Result:
   entry `frontend/tests` carries `durationMs` equal to the sum of those
   shards' recorded assertion durations; its file breakdown lists that file's
   counts and `durationMs`; the downloaded summary and Intent agree with the
   runner report for the entry totals and that file.
2. **Timed Cucumber attempt.** Pre: all six E2E-tests shards write Cucumber
   messages with step durations on the final attempt of each scenario,
   including a failed scenario and one feature under `e2e_test/features`.
   Trigger: the same collector upload. Result: entry `e2e_test/features`
   carries `durationMs` from those final-attempt step sums; that feature's
   file figures match the messages; Intent agrees with the summary.
3. **Missing shard.** Pre: five of six Cucumber fragment artifacts are present
   for the attempt. Trigger: summarize. Result: the missing part is reported
   missing and not counted; the five present parts still contribute their
   counts and available `durationMs` and file breakdown.
4. **Failed tests still timed.** Pre: a Vitest or Cucumber part has failed
   checks that recorded durations. Trigger: fragment then summarize. Result:
   those failures count as failed and their times are included in entry and
   file `durationMs`.
5. **Untimed load failure.** Pre: a Vitest file fails to load (failed status,
   no assertion results). Trigger: conversion and fragment with `--by-file`.
   Result: one failed check for that path with no times line; the part's
   breakdown marks it untimed rather than inventing a duration.

**Architecture:**

- Refresh the five skill-owned collector modules into `scripts/ci/`
  (including `ci-proof-figures.mjs`) as unchanged copies; keep
  runner-specific conversion and workflow wiring in project-owned files
  (`.github/ci_proof_fragments`, `ci-proof-outcomes.mjs`).
- No Accepted ADR constrains this CI publishing path. [ADR 0006](../../docs/adrs/0006-failure-handling-accepted.md)
  already matches the loud conversion/collector failure rule. [ADR 0007](../../docs/adrs/0007-environments-and-isolation-accepted.md)
  treats CI as an execution location selecting existing environments; this
  story does not change environment isolation.
- No Intent/dashboard change in this repository: the promise is that CI
  publishes the recipe fields Intent already displays.

**Investigation (refinement):**

| Kind | Claim | Source |
| --- | --- | --- |
| Fact | Counts-only CI proof already publishes for `frontend/tests` (2 parts) and `e2e_test/features` (6 parts) | `docs/ci-proof-summary.md`, `.github/workflows/ci.yml`, `.github/ci_proof_fragments` |
| Fact | Outcomes already use repository-relative paths | `scripts/ci/ci-proof-outcomes.mjs` |
| Fact | Project collector copies lack `--times`, `--by-file`, and `durationMs`; installed skill has them; `ci-proof-figures.mjs` is absent from `scripts/ci/` | `diff` of skill scripts vs `scripts/ci/` (2026-11-07) |
| Fact | Vitest JSON assertions expose optional `duration` in milliseconds | Vitest JSON reporter docs / `JsonAssertionResult` |
| Fact | Cucumber `TestStepResult.duration` is `{seconds, nanos}` | `@cucumber/messages` schema |
| Fact | Collector `--times` lines are seconds, converted ×1000 to `durationMs` | installed `ci-proof-fragments.mjs` |
| Hypothesis | Some Vitest assertions or Cucumber steps may omit duration on CI | treat as untimed; no invented values |

**Challenge (refinement):** Strongest doubt — a developer reading “cost” as CI
wall time could misread Intent's `durationMs`. The stated Goal already names
cumulative recorded test execution time per the recipe; Scope defers wall-clock
signals so that misreading is not part of this promise.
