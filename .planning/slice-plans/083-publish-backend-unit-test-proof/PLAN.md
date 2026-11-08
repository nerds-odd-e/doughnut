# Publish backend unit-test proof in Intent

Work item: **SEED-107#publish-backend-unit-test-proof**.
Source: [refined story](../../seeds/SEED-107-publish-backend-unit-test-proof.md#publish-backend-unit-test-proof).

## Goal and scope

For a judged `donut CI` attempt, Intent's CI status shows backend unit-test
passed, failed, and other counts for `backend/src/test/java` alongside the
existing frontend and end-to-end entries, agreeing with that attempt's JUnit
report and the downloaded `ci-proof-summary-attempt-<n>` artifact. A job that
reports failed tests still contributes those results; a job with no report
shows the backend part as missing.

**Included:** register `backend/src/test/java` in `.planning/open-dough.json`
`intent.locations`; convert Gradle JUnit XML from `Backend-unit-tests` into
one proof fragment (one part); include it in the existing collector summary;
extend focused CI-proof tests and `docs/ci-proof-summary.md`. Follow the
[CI proof recipe](../../../.agents/skills/dough-test-optimization/references/ci-proof-summary.md).

**Excluded:** `scripts` / `infra` proof; backend test sharding beyond a
single-part matrix needed for the existing fragment action; Intent/dashboard
product changes; CI-adapter publishing; changing how frontend or E2E proof is
judged beyond extending `expect` and collector `needs`.

## Architecture

- **PFE:** Reuse the project's existing producer → fragment → `ci-proof`
  collector path (Vitest/Cucumber already on it). Extend
  `scripts/ci/ci-proof-outcomes.mjs` with a `junit` format the same way those
  runners were added; wire `Backend-unit-tests` through
  `.github/ci_proof_fragments` and extend `.github/ci_proof_summary` `expect`.
  Do not fork the skill-owned collector copies.
- **Boundary:** Runner-specific conversion and workflow wiring stay
  project-owned; collector math stays skill-owned copies in `scripts/ci/`.
- **ADRs:** No Accepted ADR constrains this path. Loud conversion failure
  continues to match [ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md).
- **North Star:** No new topic; CI publishing plumbing, not notebook domain
  structure.

## Key examples → proof

| Promise (story key example) | Slice | Proof |
| --- | --- | --- |
| Passing attempt: backend counts agree with JUnit; frontend/E2E remain | 1, 2 | Converter tests on fixture XML; local `fragment`/`summarize`; slice 2 CI artifact + Intent |
| Failed tests still publish counts (not missing) | 1, 2 | Converter: `failure`/`error` → `failed`; workflow fragment step runs when the test step was not `skipped`; slice 2 when available |
| No report → backend part missing; other entries unaffected | 1 | Fragment step skipped when the test step was skipped; `summarize` with only frontend/E2E fragments leaves backend missing |
| Same artifact carries all three | 1, 2 | Collector `expect` lists all three; slice 2 downloads one `ci-proof-summary-attempt-<n>` |

## Decisive premises

| Premise | Consuming operation | Observation | Result |
| --- | --- | --- | --- |
| Gradle `test` writes JUnit XML under `backend/build/test-results/test/` by default (one file per class) | Slice 1 conversion + workflow report path | Gradle userguide (Context7 `/websites/gradle_current_userguide`, 2026-11-08): Test task XML at `layout.buildDirectory.dir("test-results/$testTaskName")`; project uses `useJUnitPlatform()` with no custom `testResultsDir` (`backend/build.gradle`) | Settled true for default location |
| `ci-proof-outcomes.mjs` has only `vitest` and `cucumber` today | Slice 1 adds `junit` | Read `scripts/ci/ci-proof-outcomes.mjs` `formats` (2026-11-08) | Settled true |
| `.github/ci_proof_fragments` requires `strategy.job-index` / `job-total` (matrix jobs) | Slice 1 gives `Backend-unit-tests` a one-value matrix | Action env uses those fields; GitHub docs: strategy context is for matrix jobs (2026-11-08). `scripts/ci/ci-workflow.test.mjs` asserts each producer's matrix length and `fail-fast: false` | Settled: use `strategy.matrix` with one part and `fail-fast: false` (no sharding) |
| Collector currently expects only E2E + frontend and does not wait on backend | Slice 1 extends `needs` / `expect` | `.github/workflows/ci.yml` `ci-proof` (2026-11-08) | Settled true |
| `backend/src/test/java` is absent from `intent.locations` | Slice 1 registers it | `.planning/open-dough.json` (2026-11-08) | Settled true |
| JUnit XML `testcase` carries optional `time` (seconds) and failure via `failure`/`error`/`skipped` | Slice 1 converter status/times mapping | JUnit XML pseudo-standard used by Gradle; converter tests use fixtures of that shape; live Gradle XML confirmed on slice 2 | Settled for fixture shape; live file confirmed in probe |
| Intent already shows proof for configured locations from the summary artifact | Story excludes Intent code changes | Existing frontend/E2E proof in Intent; recipe contract | Settled as publishing contract; live Intent agreement is slice 2 |

## Slices

### 1. A CI attempt summary includes backend unit-test proof from JUnit
Type: Behavior
Status: planned
Proof: Extend `scripts/ci/ci-proof-outcomes.test.mjs` for JUnit → outcomes/times
(passed, failed, skipped→other, classname→`backend/src/test/java/.../*.java`,
multi-file directory); extend `scripts/ci/ci-workflow.test.mjs` so expect lists
`e2e_test/features=6 frontend/tests=2 backend/src/test/java=1` with matching
producer matrix length and `fail-fast: false`; run `pnpm test:ci-proof`.
Optional local `fragment`+`summarize` on fixtures shows the backend entry with
frontend/E2E fixtures still present.

Behavior: Pre — frontend and E2E producers already publish fragments.
Trigger — `Backend-unit-tests` finishes (pass or fail with a report), converts
`backend/build/test-results/test` JUnit XML, uploads one fragment; `ci-proof`
collects after Backend, E2E, and Frontend.
Result — `ci-proof-summary-attempt-<n>` expects and carries
`backend/src/test/java` as one part with counts from the report; a skipped
backend test step leaves that part missing; frontend/E2E expectations unchanged
in meaning. Register the location in `open-dough.json` (after `frontend/tests`,
before `scripts`). Update `docs/ci-proof-summary.md`.

Work decisions baked in:
- Add `junit` to `ci-proof-outcomes.mjs`; `report` may be a directory of
  `TEST-*.xml` (or equivalent) because Gradle emits one file per class.
- Map classname to repository path under `backend/src/test/java` (nested
  `$` classes → outer class file).
- Give `Backend-unit-tests` `strategy: { fail-fast: false, matrix: { part: [1] } }`
  so the existing fragment action's part numbering works without sharding.
- Id the Gradle test step; run fragments when `!cancelled() && outcome != 'skipped'`.
- Collector `needs: [E2E-tests, Frontend-unit-tests, Backend-unit-tests]` and
  expect in `intent.locations` order among proved entries.

### 2. Judged CI attempt shows backend proof in the summary and Intent
Type: Behavior
Status: planned
Proof: Early probe after slice 1 is on the judged workflow: download
`ci-proof-summary-attempt-<n>` for one completed attempt; confirm
`backend/src/test/java` counts agree with that run's JUnit totals (or the part
is missing if the job produced no report), and that Intent's CI proof for that
attempt agrees. Failure stops here and revises the plan.

Behavior: Pre — slice 1 published on the revision the attempt builds.
Trigger — the attempt finishes and the developer opens that attempt in Intent.
Result — backend entry present with agreeing counts (examples 1–2) or missing
when appropriate (example 3); frontend and end-to-end remain on the same
artifact (example 4).

## Current decisions

- One backend part only; the one-value matrix exists solely so
  `strategy.job-index` / `job-total` populate for `.github/ci_proof_fragments`.
- Location path is exactly `backend/src/test/java`; place it after
  `frontend/tests` in `intent.locations`. Intent `kind` / include / runner
  fields follow whatever shape Intent already uses for a Java test tree; if a
  supported kind string is unclear at implementation, match the closest
  existing location metadata pattern without inventing Intent UI behavior.
- JUnit times use the XML `time` attribute (seconds) for `--times`, same as
  other producers; absent time → untimed.
- No change to skill-owned collector module copies unless a recipe gap blocks
  this entry (none known).

## Concern evidence

- Slice-plan refinement: not needed — one commit-sized delivery Behavior plus
  one early-probe Behavior; boundaries match the story's single outcome and
  reuse the established CI proof path.
- Remaining slice-specific concerns: none identified. Slice 2 is an authorized
  early probe for live artifact/Intent agreement; its failure revises the plan
  and does not block recording `ready`.
