# Retire the split-half reliability gate machinery

**Status:** planned, not started.
**Type:** ad-hoc plan (`.planning/quick/`), cleanup-only.
**Origin:** post-hoc audit of `.planning/quick/001-morning-cognitive-index/PLAN.md`
requested by the developer to close that plan out. All slices below are
**Structure** — no user-facing behavior changes. Do not execute until the
developer approves.

## Goal

Delete the diagnostic code cluster that plan 001's slices 21.1–21.9 built
solely to answer one question: *is the composite morning index's split-half
reliability ≥ ~0.6?* That gate was run against complete production data and
recorded a decisive **fail** (`pairCount: 91`, `rawCorrelation: 0.076`,
`spearmanBrownCorrelation: 0.141`), so the composite index itself (slices
22–25) was permanently dropped. The verdict is already committed to plan
001's text. The diagnostic apparatus that produced that verdict has no
remaining consumer — nothing in the shipped product, and nothing in the
extracted follow-on plans `007-daily-cognitive-probe` or
`008-probe-convergent-analyses`, which only cite the gate's numeric *result*
as history, not the code. Per `general.mdc`/`planning.mdc`, code with no
side effect on externally observable behavior and no remaining purpose must
not be kept.

## Audit trail (why each deletion is safe)

A fork-based audit of plan 001's full scoped diff, independently re-verified
by grep against the current tree, found the following zero-consumer graph
(each class's only references are to each other and their own tests):

- `RecallCognitiveIndex` ← only `RecallMorningHalfIndex` + its own test
- `RecallMorningHalfIndex` ← only `RecallSplitHalfReliability`,
  `RecallStatsTestFixtures`, + its own test
- `RecallSplitHalfReliability` ← only `RecallStatsService
  .computeSplitHalfReliability`, `UserController
  .getRecallSplitHalfReliability`, `RecallStatsTestFixtures`, + its own test
- The `GET /api/user/recall-split-half-reliability` endpoint is on the wire
  (`open_api_docs.yaml`, `packages/generated/donut-backend-api/*`) with
  **zero** frontend call sites — plan 001 built it "diagnostic-only," never
  wired into `RecallStatsDTO` or any page.
- `RecallPaceAggregator.PaceResult.paceDayBaseline`/`lapseDayBaseline` are
  consumed only by `RecallMorningHalfIndex.scoreHalf` — but are computed on
  **every** real `RecallPaceAggregator.compute(...)` call, i.e. every actual
  Recall Stats page load, for a consumer that's being deleted. Removing them
  is not just dead-code cleanup; it removes wasted per-request computation
  from the shipped Pace tile's code path.
- `RecallStatsTestFixtures`'s `variedBaselinesThrough`, `addScorableMorning`,
  `warmedUpBaselines` (lines 65, 97, 112) are called only from
  `RecallSplitHalfReliabilityTest` and `RecallMorningHalfIndexTest`.

**Confirmed NOT part of this cluster — do not touch:**
- `RecallNewtonRaphson`, `RecallProbabilityMath` — back the shipped Accuracy
  tile via `RecallCalibrationFitter`/`RecallGuessingFloorFitter`.
- `RecallDayBaseline`, `RecallWeightedResidualStats` — back the shipped
  Consistency badge (`consistencyZScore`/`residualsByDate`) and the shipped
  Pace tile's own weighted median/MAD. `RecallMorningHalfIndex` also imports
  `RecallDayBaseline`, but that import dies with `RecallMorningHalfIndex`
  itself — the class stays for its real callers.
- `RecallStatsService`'s `findAllTimeAnsweredRows`/`reviewsOnly`/
  `localToday` — shared with the still-live `compute`/`aggregateRows` paths
  (confirmed by reading the file); only `computeSplitHalfReliability` itself
  goes, not its shared helpers.
- `RecallAnswerRow.memoryTrackerId` — genuine per-item EWMA key, unrelated
  to this cluster.

No bugs, digressions, or other refactor/redundant-test issues surfaced
outside this cluster — the rest of plan 001's surviving code read as tight
and already refactored. This plan's scope is exactly the deletions below.

## Slices

Status legend: `[ ]` planned · `[~]` in progress · `[x]` done

Each slice is verified by "existing tests still pass, no observable
difference" plus a full `Recall*` backend test run to confirm nothing else
silently depended on the removed surface (expected: nothing, per the
zero-consumer graph above). Ordered leaves-first so every slice leaves a
compiling, green tree if the developer stops partway.

### 1. Delete the composite-index arithmetic — Structure `[x]`

Delete `RecallCognitiveIndex.java` and `RecallCognitiveIndexTest.java`.

- Verify: `grep -rl RecallCognitiveIndex backend/src` returns nothing after
  deletion; targeted backend test run
  (`./gradlew test --tests "*RecallMorningHalfIndex*"`) still compiles and
  passes (its only remaining caller, `RecallMorningHalfIndex`, is deleted in
  slice 2 — sequencing this first keeps each slice independently buildable
  only if slice 2 follows immediately; if these must land as separate
  commits, delete 1 and 2 together in one slice instead of splitting further,
  since `RecallMorningHalfIndex` won't compile without `RecallCognitiveIndex`
  removed from its usage first — check actual compile order when executing
  and merge into one slice if splitting proves artificial).

**Done:** landed together with slices 2–4 in one commit, per the compile-order
note above — intermediate states didn't compile independently.

### 2. Delete the odd/even half-index scorer — Structure `[x]`

Delete `RecallMorningHalfIndex.java` and `RecallMorningHalfIndexTest.java`.

- Verify: `grep -rl RecallMorningHalfIndex backend/src` returns nothing;
  `RecallSplitHalfReliability` (deleted next) is this class's only remaining
  production caller, so slices 1–2 and slice 3 are tightly coupled the same
  way — evaluate at execution time whether 1–3 are better landed as one
  Structure slice ("delete the whole half-index/composite/reliability
  cluster") rather than three, since intermediate states won't compile
  independently. Prefer one slice over three broken intermediate commits.

**Done:** landed together with slices 1, 3, 4 in one commit.

### 3. Delete the split-half reliability computation, DTO, and endpoint — Structure `[x]`

Delete:
- `RecallSplitHalfReliability.java` + `RecallSplitHalfReliabilityTest.java`
- `RecallSplitHalfReliabilityDTO.java`
- `UserController.getRecallSplitHalfReliability` (the
  `GET /api/user/recall-split-half-reliability` endpoint method and its
  Javadoc) + `UserRecallSplitHalfReliabilityControllerTest.java`
- `RecallStatsService.computeSplitHalfReliability(...)` — leave
  `findAllTimeAnsweredRows`/`reviewsOnly`/`localToday` untouched, they're
  shared with `compute`/`aggregateRows`.

Remove the now-unused `RecallSplitHalfReliabilityDTO` import from
`UserController.java`.

- **If slices 1–3 turned out coupled at execution time, land them as one
  slice here instead — that's fine, still Structure, still one coherent
  "remove the diagnostic cluster" change.**
- Verify: `./gradlew build` compiles clean; full `Recall*` backend test
  package run is green; `grep -rn "recall-split-half-reliability\|SplitHalfReliability" backend/src`
  returns nothing.

### 4. Regenerate the OpenAPI doc and TS client — Structure `[x]`

Run the `generate-api-client` skill so `open_api_docs.yaml` and
`packages/generated/donut-backend-api/{sdk.gen.ts,types.gen.ts,index.ts,
api-summary.md}` drop the orphaned endpoint and DTO.

- Verify: `RobotsTests.openApiDocsMatchCommittedYaml` passes (this is the
  same CI check plan 001 slice 15 found fires whenever a wire shape changes);
  `grep -rn "SplitHalfReliability" packages/generated frontend/src` returns
  nothing.

**Done:** landed together with slices 1–3 in one commit — deleting the
endpoint alone would have left `RobotsTests.openApiDocsMatchCommittedYaml`
red until regeneration ran, so regeneration was pulled into the same
wrap-up rather than committing a known-red intermediate state. `pnpm
frontend:test` (328 files, 1732 tests) also confirmed no frontend call site
depended on the removed wire shape.

### 5. Stop computing day-level pace/lapse baselines on every request — Structure `[ ]`

In `RecallPaceAggregator.java`, remove:
- `PaceResult.paceDayBaseline` / `PaceResult.lapseDayBaseline` record fields
- the private `paceDayBaseline(...)` / `lapseDayBaseline(...)` builder
  methods
- the `lapseCountByDate` accumulation map (exists only to feed
  `lapseDayBaseline`)

Leave `residualsByDate` and every other field/method untouched — those still
feed `consistencyZScore` and the shipped Pace tile.

Delete `RecallPaceAggregatorDayBaselineTest.java` (tests exactly the removed
fields).

- Verify: `./gradlew test --tests "*RecallPaceAggregator*" --tests "*RecallStatsService*"`
  green; targeted E2E for `recall_stats.feature` still passes (Pace/
  Consistency tiles unaffected — this removes internal fields never on the
  DTO/wire).

### 6. Remove the now-orphaned test fixture helpers — Structure `[ ]`

In `RecallStatsTestFixtures.java`, remove `variedBaselinesThrough`,
`addScorableMorning`, `warmedUpBaselines` (lines ~65, ~97, ~112 as of this
writing — re-locate at execution time since line numbers shift after slices
1–5).

- Verify each has zero remaining callers first
  (`grep -rn "variedBaselinesThrough\|addScorableMorning\|warmedUpBaselines" backend/src`)
  — expected empty after slices 1–3 removed their only callers. Full
  backend build green.

### 7. Full backend test run and close plan 001's text — Structure `[ ]`

- Run the full backend test suite once (not per-slice) as a final gate for
  this plan, per `planning.mdc`'s "keep known-green suites green" — targeted
  runs already covered each slice; this is the one full-suite check before
  declaring the cleanup done.
- Update `.planning/quick/001-morning-cognitive-index/PLAN.md`:
  - In the 21.1–21.9 section, add a short closing note that the diagnostic
    cluster (day-level pace/lapse baselines, half-index scoring, composite
    arithmetic, split-half reliability + its endpoint) was deliberately
    deleted once the gate verdict was recorded, per this plan
    (`.planning/quick/009-retire-split-half-reliability-gate/`), so a future
    reader doesn't wonder where the classes went.
  - Update the "Permanent artifacts" table: none of the deleted cluster was
    listed there (it was correctly never treated as a permanent artifact —
    `RecallSplitHalfReliability`/`RecallMorningHalfIndex`/
    `RecallCognitiveIndex` are absent from that table already), so no
    row-removal needed; just confirm and note this was checked.
  - Apply `planning.mdc`'s "plan is fully executed → clean up spent history"
    rule to plan 001 itself now that this closing cleanup lands: prune
    slice-by-slice implementation blow-by-blow that no longer helps a future
    reader of the *product* (the code, ADRs, and this plan's closing note now
    carry the permanent record), keeping only what a future reader of Recall
    Stats' pace/accuracy/consistency/lapse-count design would actually want
    (key design decisions, the Discoveries section's durable lessons, the
    reliability-gate verdict and why the composite was dropped). Mark plan
    001 **Status: closed**.
  - Delete this plan (009)'s own directory once its work is merged and
    plan 001's closing note references it — no permanent diary, per
    `planning.mdc`'s guardrail against leaving completed plans as a diary.
    (Judgment call at execution time: if the developer wants a permanent
    pointer instead of full deletion, a one-line mention in 001's closing
    note suffices and 009's directory can still go.)

## Permanent artifacts (capability-named)

None — this is a pure deletion plan. No new capability-named test or feature
file is created. Existing capability-named artifacts
(`recall_stats.feature`, `RecallStatsServicePaceAggregationTest`, etc.) are
unaffected except where a slice above explicitly deletes a file.

## Per-slice wrap-up

Per `.cursor/rules/planning.mdc`: for each slice, delete → run targeted
tests (`./gradlew test --tests "..."` for the touched classes, plus
`RobotsTests` for slice 4) → confirm green → `post-change-refactor` on the
diff (expect little to nothing left to refactor, since this is subtractive)
→ update this plan's slice status → commit and push before the next slice.
No `@wip` E2E expected — no E2E scenario touches the deleted diagnostic
endpoint (it was never wired to a page). Never commit on a red build.
