# Morning cognitive index from recall history

**Status:** the 001 plan's active scope is complete. Slices 1–14, 14.1–14.8,
15, 16, 17, 18, 19, 20, 21.1–21.8 done. Slice 17.1 (pace-expectation R/D
correction, split from 17) is **dropped**. Slice 21.9 (why retrievability
reads null for ~95% of reviews) found a likely root cause — `RecallLog` is
new and current data may be incompletely backfilled, or, more seriously,
`memory_tracker.stability` updates from `applyGrade` may not be persisting —
but confirming it needs a production read-only query only the developer can
run; it is **closed in this plan and escalated** to
`.planning/notes/memory-tracker-stability-not-persisting.md` rather than
chased further here. Because of that, the slice 22–25 reliability gate is
treated as **failed**: the composite morning index is **dropped**, and the
component readouts (pace, accuracy, consistency, lapse count) ship as the
plan's final deliverable. `recall_stats.feature`'s pace scenario stays `@wip`
— a second, unrelated E2E race condition was found (see Discoveries).
Slices 26–33 (daily probe, optional tail) were not started; 32–33 reference
the now-dropped composite index and need re-scoping if picked up later.
**Type:** ad-hoc plan (`.planning/quick/`)
**Research memo:** https://claude.ai/code/artifact/9e13f954-fc5e-48e5-868f-f75d03f811c1

## Goal

Give the learner a daily readout of how their morning recall compared with what
the scheduler predicted for exactly the items that came due — a cognitive-state
signal rather than a restatement of what FSRS scheduled.

Every readout is a **residual**: observed outcome minus the expectation derived
from retrievability, difficulty and per-item time intensity, standardized
against that learner's own recent history.

**Size warning:** ~40 slices is milestone-sized for `quick/`. It is here because
the roadmap has no active milestone. Promote to `.planning/phases/` via
`/gsd-new-milestone` if this should be tracked as a GSD capability.

## Value ordering (why this sequence)

1. **Timer accuracy first.** Slices 1–3 fix a live defect in the response-time
   statistic that already ships. Worth doing even if nothing else here is built.
2. **The pace channel needs no migration.** `thinking_time_ms` already exists and
   per-item time intensity is computable from history.
3. **Repair the shipped readouts (14.1–14.8) before Accuracy.** Inspection of
   slices 1–14 found live defects (idle counts device sleep; pace inherits the
   old chart's 1s-drop / 2min-cap so mistaps still inflate retention), dead
   projection fields, redundant tests, a spelling gap, and two clock mismatches
   that leave the new E2E `@wip`. Stopping after 14.8 leaves trustworthy timer
   and pace readouts. **Do not start slice 15 until 14.1–14.8 are done.**
4. **Accuracy needs schema plus backfill**, so it comes after the repairs.
5. **The composite waits for the reliability gate.** Component readouts are plain
   statistics that defend themselves; only the number labelled as a cognitive
   index makes a claim about the person.

## Key design decisions

- **Residual, not raw.** Daily accuracy and mean response time mostly measure
  which cards came due. Every metric here is scored against a per-attempt
  expectation.
- **Counts over means for the slow tail.** Implausibly fast attempts are dropped
  whole (they invalidate the accuracy observation too, not just the timing);
  slow attempts are winsorized rather than deleted, because genuine effortful
  retrieval is the signal, not the noise. The lapse count is robust by
  construction and carries the most weight.
- **Pause types are derived, never asked.** No pause button: compliance with a
  manual pause would fail exactly when the learner is tired or distracted,
  putting measurement error in step with the measurement target. "View last
  answered question" is not reused as a pause signal either — it already fires
  automatically on every wrong answer.
- **Wall-clock reconciliation is the guarantee; lifecycle events are an
  optimization.** `freeze`/`resume` listeners are deliberately *not* added — the
  gap detector already makes the total correct, and two mechanisms for one
  guarantee is defensive programming.
- **Choice count and stem length are not collected.** The guessing floor is
  fitted (3PL γ) instead of assumed as 1/k — a better model, since distractors
  are never equally plausible. Reading time lives inside per-item time intensity
  after a few exposures; the cold-start cost is handled by a confidence weight.
- **No self-report.** This removes the known-groups validation check and bounds
  the product claim: the index may say *unusual for you*, never *because you
  slept badly*. Copy must respect that.
- **Extend the existing projection.** `RecallStatsService` documents that its
  single projection query is what avoids the N+1 that once timed the endpoint
  out. Add fields to `RecallAnswerRow`; do not add a second query or hydrate
  entities.

## Discoveries

- **`useThinkingTimeTracker` double-counts device sleep.** `resume()` opens with
  `if (hasStopped.value || isRunning.value) return`. When a device suspends
  without firing a pause event, `isRunning` is still true, so both wake-up
  handlers return early and the entire sleep is counted. A phone locked
  overnight mid-question contributes one attempt of several hours.
- **Viewing a previous answer does not stop the clock.** `RecallPage.vue` hides
  `Quiz` with `v-show`, so the unanswered prompt stays mounted and
  `onDeactivated` never fires.
- **`viewLastAnsweredQuestion` fires automatically on every wrong answer**
  (`useRecallAnswerHandling.ts`), which is what sets `isRecallPaused`. That flag
  cannot be used as an interruption signal.
- **Recall Stats E2E exists but is `@wip`.** Slice 9 created
  `recall_stats.feature`; slice 14.5 unblocks it.
- **Slice 6 has no "open the note mid-question" affordance to hook.** The
  active/unanswered `RecallPrompt` DTO (`RecallPrompt.java`) exposes only
  `notebook`, `mcq`, `spellingQuestion` — no note reference. `Mcq.java` has a
  `@JsonIgnore` note field, stripped by `Mcq.withoutSolution()` before the
  frontend ever sees it. `Quiz.vue`'s `NotebookLink` goes to the whole
  notebook (not "the" note) via a full `router-link` navigation. **Resolved:**
  `DonutApp.vue` now KeepAlive-includes `RecallPage`; opening a note is a full
  navigation away; the learner returns via Resume. See slice 6.
- **Idle still counts a silent device suspend (post slice 3+7).**
  `reconcileGap()` resets `runningStart` when the jump exceeds 5s, but leaves
  `lastActivityAt` in the past. The next watchdog tick then runs `checkIdle()`
  and adds the whole sleep to `idleMs`. Slice 3's exact scenario (no
  `visibilitychange`, `isRunning` still true) is fixed for thinking time and
  broken for idle. `stop()` also never flushes idle, so the last stretch can
  be short by up to one watchdog interval.
- **Pace inherits the trend-chart response-time policy.**
  `RecallPaceAggregator` calls `RecallStatsAggregator.responseTimeMs`, which
  drops `<1000ms` and caps thinking time at 120s. A 200ms correct mistap
  therefore never enters `implausiblyFastRows` (the empty Optional is
  `continue`d) and **still counts toward retention** — contradicting slice 10.
  A 3-minute instrumented think is scored as 2 minutes; the 5-minute hard-drop
  only fires on the null-`thinkingTimeMs` diff-fallback path used in tests, not
  on real `thinking_time_ms` rows. Trend AM/PM averages should keep the old
  caps; pace must not.
- **`noteId` on `RecallAnswerRow` has no consumer.** Slice 8 added
  `mt.note.id` to the JPQL constructor; only `memoryTrackerId` is read.
  Remaining slices do not need it. Dead projection field.
- **Spelling drops interruption fields.** Slice 5's justification ("spelling
  Recall History does not render thinking/away") is wrong: those spans live in
  the shared header of `RecallHistory.vue`. `SpellingQuestionDisplay` already
  uses `useQuestionThinkingTime` (clock pauses correctly) but submits only
  `thinkingTimeMs`. `AnswerSpellingDTO` has no away/detour/idle. ADR 0003
  thinking time applies to every measured prompt.
- **Two clock mismatches leave E2E `@wip`.** (1) `Answer.createdAt` is
  `System.currentTimeMillis()` while stats windows and recall logs use
  `testabilitySettings.getCurrentUTCTimestamp()` — time-travelled answers fall
  outside the query; `recall_stats.feature` is `@wip`. (2)
  `useRecallPageLoading` onActivated compared `new Date()` to
  `currentRecallWindowEndAt`, so a simulated day-2 window was always "stale"
  vs 2026; KeepAlive reactivation refetched and remounted Quiz, discarding the
  detour accumulator. **Slice 14.6 replaced that with string identity**; the
  replacement is itself unstable (`alignByHalfADay` leftover nanos), so every
  activation remounts in production. Repair is
  [004-recall-same-window-queue](../004-recall-same-window-queue/PLAN.md), not
  more 001 slices.
- **Redundant tests.** `useThinkingTimeTracker.keepAlive.spec.ts` drives
  `pause()`/`resume()` on KeepAlive, which production no longer uses (detour
  pair). `QuestionDisplay.thinking.spec.ts`'s "pauses timer when deactivated"
  is a weaker duplicate of the detour case. PaceTile "badge absent when field
  absent" repeats the canonical render test. MemoryTracker "does not display
  away" and "does not display detour" share one fixture.
- **`useThinkingTimeTracker.ts` is 281 lines** (over the 250-line split
  threshold). The injected `clock` option from slice 2 is unused: tests spy
  `performance.now()` instead. `useQuestionThinkingTime` calls `start()` from
  both an immediate watch and `onMounted`.
- **Package rename has landed** (`com.odde.donut`). The in-flight rename
  discovery above is historical.
- **`recall_stats.feature`'s pace scenario has a second, independent blocker
  beyond the clock mismatch (post slice 14.5).** With `Answer.createdAt` now
  correctly testability-clock-stamped, the scenario still fails: its
  `answerSlowlyOnDay` step fires `backendTimeTravelTo` + generated-SDK answer
  calls back-to-back across a loop, but the SDK's `fetch` dispatches eagerly
  at call time rather than deferred to Cypress's command queue, so requests
  for different simulated days race against the backend's shared
  `@ApplicationScope` testability clock. DB inspection confirmed two of three
  expected answers landed on the same simulated day. Needs the E2E step
  helpers to sequence requests (await each before firing the next) — not
  fixed by this plan; left `@wip`.

## Jidoka checkpoints — stop for developer judgement

**Before slice 15 — ADR 0003 tension.** **Resolved** (commit `5a8b19c085`):
developer decided to persist `stability_before` / `difficulty_before` /
`retrievability` on `recall_log` rather than replay history per query. ADR
0003 amended — the Retrievability glossary entry and "Recall history and
current state" now state these columns are a materialized cache of a value
the frozen FSRS profile always reproduces by replay, not a new source of
truth; RecallLog's Grades and Confusion remain the record.

**Before slice 9 — new vocabulary.** **Done** (commits `97cc69940f`,
`f67d894175`): *pace*, *retrieval lapse*, *detour*, *away*, *idle*, *daily
probe*, and *cognitive index* are in ADR 0001 / ADR 0003.

**Before slice 22 — the reliability gate.** **Resolved: gate failed, slices
22–25 dropped.** The gate said: if slice 21.4 reports split-half reliability
below ~0.6, slices 22–25 do not ship, and do not tune weights to rescue the
number. 21.9 found the input feeding both the reliability calculation and the
accuracy component is null for ~95% of reviews (`memory_tracker.stability`
reads as New almost everywhere) — the gate cannot be meaningfully evaluated,
let alone pass, on that basis. Per the gate's own stated fallback, the
composite is abandoned rather than reworked; the component readouts (pace,
accuracy, consistency, lapse count) stand on their own and already ship.
21.9's underlying data-integrity finding is escalated separately — see that
slice's section and
`.planning/notes/memory-tracker-stability-not-persisting.md` — rather than
resolved inside this plan. (Slice 21 was split into 21.1–21.4 — see that
section for the composite formula and why.)

**Before slice 17.1 — pace-expectation correction formula.** **Resolved:
dropped.** Slice 17's original text asserted R/D would correct the pace
time-expectation but gave no model. Rather than specify one, the developer
chose to drop 17.1 outright: 21.9 found retrievability null for ~95% of
reviews (`memory_tracker.stability` reads as New almost everywhere), and even
where populated it comes from recently-backfilled data of unproven accuracy —
correcting a time-expectation baseline against a signal that's mostly absent
or noisy adds complexity ("keep it simple") for a correction that mostly
wouldn't fire. The per-item EWMA baseline from slice 9 ships as the permanent
pace expectation, uncorrected. Cost: an item with genuinely low retrievability
that takes longer to retrieve will register as "slower than usual" even though
slowness was expected — accepted as a minor false positive. Confirmed no dead
code results (slice 17 built nothing speculative for pace — see 17.1 below).

---

## Slices

Status legend: `[ ]` planned · `[~]` in progress · `[x]` done

### Recall timer accuracy

#### 1. Viewing a previous answer stops the question clock — Behavior `[x]`

Open an unanswered prompt, view the last answered question, return: that
interval is excluded from the answer's thinking time and from Recall History.

- E2E: extend `recall/browse_answer_and_notes_while_recalling.feature`
- Unit: mounted `RecallPage` with fake timers

Done: added a sibling shared-state flag `isViewingAnsweredQuestion` on
`useRecallData` (deliberately separate from `isRecallPaused`, which fires
automatically on wrong answers and has different consumers). `RecallPage.vue`
sets it from the existing `previousAnsweredQuestionCursor` watcher;
`useQuestionThinkingTime.ts` watches it and calls the tracker's existing
`pause()`/`resume()`. E2E finished in slice 14.7.

#### 2. Inject a clock into the thinking-time tracker — Structure `[x]`

Replace direct `performance.now()` / `Date.now()` reads with an injected clock.
No observable change; existing tests pass unchanged.

- **Enables slice 3 only** — a wall-clock jump cannot be simulated otherwise.

Done: `useThinkingTimeTracker.ts` takes `ThinkingTimeTrackerOptions { clock?: Clock }`,
defaulting to a `realClock` backed by `performance.now()`; all three internal
reads route through `clock.now()`, following the existing options-object
precedent in `useDebouncedTextAutosave.ts`. No `Date.now()` calls existed.
Consumer (`useQuestionThinkingTime.ts`) unaffected — still calls with no args.

#### 3. A suspended device no longer inflates thinking time — Behavior `[x]`

Lock the phone mid-question, unlock, answer: the suspended interval is excluded.
Covers the `resume()` early-return and reconciles on watchdog tick and in
`stop()`.

- Unit: `useThinkingTimeTracker` with the injected clock — the deliberate
  isolated contract, replaying event sequences no real browser produces on demand
- Deliberately **no** `freeze`/`resume` listeners

Done: added `reconcileGap()`, a wall-clock gap detector wired into three
places — `resume()`'s existing early-return branch (the exact stale state a
silent device suspend leaves), the existing 250ms watchdog interval
(renamed `watchdogIntervalId`, previously used only to detect
`document.hidden`), and `stop()`. Threshold `SUSPEND_GAP_THRESHOLD_MS = 5000`
(no prior precedent in the codebase). No `freeze`/`resume`/`visibilitychange`
listeners added, per the plan's design decision.

### Interruption record

Each slice writes one kind of pause **and** renders it, so nothing is write-only
data waiting for a consumer.

#### 4. Pause columns on `answer` — Structure `[x]`

`V300000302__add_pause_tracking_to_answer.sql`: `away_ms`, `away_count`,
`detour_ms`, `detour_count`, `idle_ms`, all nullable with no default, plus entity
fields. NULL must mean "predates the instrumentation" — a `NOT NULL DEFAULT 0`
would make an uninstrumented answer indistinguishable from an uninterrupted one.

- `thinking_time_ms` is **not** renamed; it is on the wire and rendered today.
- **Enables slice 5 only.** Regenerate `docs/database-erd.md`.

Done: migration filename is `V300000302` (next available after `V300000301`,
not the `V300000303` guessed when this plan was drafted — later slices should
likewise compute their number fresh from the migration directory rather than
trusting a hardcoded number in this plan text; **slice 15's stated
`V300000302` will collide and must be recomputed when that slice runs**).
`Answer.java` gained 5 nullable boxed `Integer` fields matching the existing
`thinkingTimeMs` pattern. `docs/database-erd.md` regenerated — no diff, since
none of the new columns are keys/FKs.

#### 5. Time away from the tab is recorded and shown — Behavior `[x]`

Switch away mid-question and back: Recall History shows away time and count
beside the thinking time.

- E2E: new `recall/recall_timing.feature`

Done: `useThinkingTimeTracker.ts` gained internal `pauseForAway()`/
`resumeFromAway()` wrapping the existing `pause()`/`resume()`, triggered only
by the tracker's own tab-visibility listeners — NOT by external callers like
slice 1's view-history pause, which stays uncategorized (excluded from
thinking time but not counted as "away"). `awayMs`/`awayCount` flow
DTO → `Answer.buildAnswer()` → entity, mirroring `thinkingTimeMs`.
`RecallHistory.vue` renders them only when truthy (old rows stay silent, not
zero). MCQ path only — `AnswerSpellingDTO` untouched, since spelling's
Recall History branch doesn't render thinking/away time at all. API client
regenerated. E2E scenario passes for real (not `@wip`) by dispatching
`blur`/`focus` on `window` and waiting the away duration in real wall-clock
time, since the tracker reads real `performance.now()`.

#### 6. A detour into a note is recorded separately — Behavior `[x]`

Open the note mid-question: Recall History distinguishes a study detour from a
tab-away. Detour is attributed to the note of the open prompt.

Done: `useThinkingTimeTracker.ts`'s `away` accumulator was generalized into a
`createInterruptionAccumulator()` factory shared by a second `detour`
accumulator, exposing `pauseForDetour`/`resumeFromDetour`. Unlike `away`
(triggered only by the tracker's own tab-visibility listeners), detour is
driven externally: `useQuestionThinkingTime.ts`'s `onDeactivated`/`onActivated`
(the `RecallPage` `KeepAlive` lifecycle hooks already used for the away/resume
plumbing) call `pauseForDetour`/`resumeFromDetour` directly, since navigating
to a note unmounts-and-remounts via KeepAlive rather than destroying the
tracker instance — no reliance on `useRecallData.ts` module-level state was
needed. `detourMs`/`detourCount` flow `QuestionDisplay.vue` →
`AnswerDTO`/`Answer` entity → `RecallHistory.vue`, rendered only when truthy,
mirroring the away-time pattern. API client regenerated.
E2E scenario added but left `@wip`: `useRecallPageLoading` onActivated
compares `new Date()` to the due-recall window, so simulated-time E2E always
treats the window as stale and remounts Quiz, discarding the detour
accumulator. **Slice 14.6 fixes that.** Unit tests cover the wiring.

#### 7. Idling in place past the threshold is recorded — Behavior `[x]`

Leave a question untouched on screen past the threshold (45–60 s, deliberately
generous — genuine hard thinking without input is common): the idle time is shown
and flagged. Censors the attempt; never subtracts silently.

Done: idle is a fresh mechanism, not a third `createInterruptionAccumulator()`
instance — it deliberately does **not** pause the clock (idle time stays
inside `thinkingTimeMs`, per "never subtracts silently"), so there is no
`idleCount` to match the `idle_ms`-only schema from slice 4. `lastActivityAt`
resets on `mousemove`/`keydown`/`click`/`touchstart`/`scroll`; the existing
250ms watchdog's `checkIdle()` accumulates only the portion of an inactivity
stretch beyond `IDLE_THRESHOLD_MS = 60000` (upper end of the 45–60s range, no
prior precedent — mirrors how slice 3 picked `SUSPEND_GAP_THRESHOLD_MS`) once
that stretch first crosses the threshold; new activity resets detection for
the next stretch without erasing what was already counted. Wired
`idleMs` → `QuestionDisplay.vue` → `AnswerDTO`/`Answer` → `RecallHistory.vue`
(`daisy-badge-warning`, `data-testid="recall-history-idle-time"`, shown only
when truthy). API client regenerated. No E2E scenario — none required by this
slice; unit/controller coverage is sufficient (idle detection depends on
DOM input events at real timescales, not something Cypress simulates
usefully here).

### Pace channel — no schema change

#### 8. Extend the `RecallAnswerRow` projection — Structure `[x]`

Add memory tracker id and note id to the existing single projection query.

- **Enables slice 9 only.** No second query, no entity hydration.

Done: appended `Integer memoryTrackerId, Integer noteId` to the
`RecallAnswerRow` record; `RecallPromptRepository.findAnsweredRecallAnswerRows`
now also selects `mt.id, mt.note.id` in the same JPQL constructor expression —
`mt.note.id` traverses the existing `MemoryTracker.note` association directly,
no extra join needed. `RecallStatsTestFixtures.answered()`/`overlapAnswered()`
pass `null, null` placeholders since nothing consumes the fields yet.

#### 9. Recall Stats shows today's pace against your usual — Behavior `[x]`

Per-item time-intensity EWMA plus session position, as a tile above
`RecallStatsTiles`.

- **Interim:** no retrievability or difficulty correction. **Permanent, not
  removed by slice 17** — see 17.1's dropped-decision note.
- E2E: new `recall/recall_stats.feature` (no existing coverage)
- Backend unit: through `UserController.getRecallStats` with `makeMe` data

Done: the research memo linked at the top of this plan is not accessible
(private artifact); design confirmed with the developer instead —
`RecallPaceAggregator.buildPace()` maintains a per-item (`memoryTrackerId`)
EWMA of `ln(responseTimeMs)` (alpha=0.3, seeded on first valid answer);
today's residual per qualifying item is `ln(observed) − τ_j` (baseline taken
*before* today's update); the tile's `pctVsUsual = (exp(mean residual) − 1) ×
100` (positive = slower). Items with no prior baseline are excluded from
`sampleSize` (slice 12 addresses cold-start weighting later) but still count
toward `totalAnsweredToday` ("session position" context). New
`RecallStatsDTO.PaceStats`, new `PaceTile.vue` rendered above `RecallStatsTiles`. E2E scenario added but left
`@wip`: `Answer.createdAt` uses `System.currentTimeMillis()` while the stats
query window uses the testability clock. **Slice 14.5 fixes that.** Backend
(`RecallStatsServicePaceAggregationTest`) and frontend (`PaceTile.spec.ts`,
`RecallStatsSettingsTab.spec.ts`) unit tests are the primary verification
until then.

#### 10. Implausibly fast attempts stop distorting pace — Behavior `[x]`

A 200 ms mistap drops out of the tile **and** out of retention, instead of
counting as a fast correct answer. Item-relative floor:
`t < max(300ms, 0.25 · exp(τ_j))`.

Done: `RecallPaceAggregator.buildPace` became `compute(...)`, returning
`PaceResult(PaceStats stats, Set<RecallAnswerRow> implausiblyFastRows)` from
the same single chronological walk (no second pass, no duplicated EWMA
logic) — a row with a present-but-implausibly-fast response time (floor
computed against the item's prior baseline, or a flat 300ms when the item has
no baseline yet) is added to the identity-based `implausiblyFastRows` set and
excluded from both `todaysResiduals` and the `tauByItem` EWMA update, so one
mistap can't corrupt that item's future baseline. `RecallStatsService.aggregateRows`
now calls `compute(...)` once and skips any row in that set in the retention
loop over `recentReviews` — relying on `recentReviews`/`recent` sharing the
same `RecallAnswerRow` object references as `allTime` (verified). Also fixed
`totalReviews365`, previously `recentReviews.size()`, to an in-loop counter so
an excluded row drops out of the retention denominator too, not just the
numerator. `totalAnsweredToday` (session position) is unaffected — it still
counts every today row regardless of speed, per slice 9's precedent. No DTO
shape change, no migration, no API regeneration needed. **Gap found later:**
sub-1000ms rows never join `implausiblyFastRows` because
`responseTimeMs` returns empty first — slice 14.3.

#### 11. A single very slow attempt stops dominating pace — Behavior `[x]`

Winsorized log-residual (cap ≈ `ln(8)`) and median-of-logs: one six-minute think
shifts the tile slightly instead of swamping it. Hard drop only above ~5 min
on-task.

Done: scoped to `RecallPaceAggregator` only — retention/accuracy is untouched,
since a genuinely slow-but-correct answer is real signal, not noise. Added
`HARD_DROP_MS = 300_000`: a row at or above 5 minutes on-task is skipped
entirely (no residual, no EWMA update, not added to slice 10's
`implausiblyFastRows` set since that set drives retention exclusion and slow
attempts must keep counting there) but still counts toward
`totalAnsweredToday`. For rows between the fast-tail floor and the hard drop,
the raw residual (`lnRt - baseline`) is winsorized via
`Math.min(rawResidual, RESIDUAL_CAP)` (`RESIDUAL_CAP = Math.log(8)`) before
being added to `todaysResiduals` — but `tauByItem`'s EWMA update still uses
the raw, uncapped `lnRt`, so a genuinely slow item still teaches its own
baseline over time; only today's reported number is robustified. `pctVsUsual`
switched from mean to median of `todaysResiduals` via a new private `median()`
helper. No DTO/API change, no migration.

#### 12. Cold-start items stop adding noise — Behavior `[x]`

Confidence weight `w_j = m_j / (m_j + 3)`. A morning of freshly assimilated cards
reports lower confidence rather than a wrong number.

Done: `RecallPaceAggregator` now tracks `m_j` (prior surviving observation
count) per item alongside `tauByItem`, read before increment on the same
rows that update the EWMA. Each today's residual is paired with its weight
`w_j = m_j/(m_j+3.0)` in a new `WeightedResidual(residual, weight)` record;
`pctVsUsual` is now a **weighted** median over these pairs (cumulative weight
crosses half of total weight), so a cold-start item barely moves the number
instead of distorting it. A new `PaceStats.confidence` field (plain mean of
today's `w_j`, null when `sampleSize==0`) surfaces the aggregate certainty.
`PaceTile.vue` shows a `daisy-badge-warning` ("low confidence — mostly new
cards") when `confidence < 0.5`, matching the existing idle-time badge
convention. API client regenerated (`PaceStats.confidence` is new on the
wire). No E2E — slice 9's pace E2E is already `@wip` for an unrelated clock
mismatch; backend + frontend unit tests cover this slice, matching slice 7's
precedent.

#### 13. Recall Stats shows today's retrieval-lapse count — Behavior `[x]`

Correct answers taking ≥ 2.5× their expected time, counted. Restricted to
*correct* answers deliberately: a slow wrong answer is a knowledge gap, a slow
right answer is the retrieval analogue of a vigilance lapse.

Done: `PaceStats` gained a plain `int lapseCount`. Counted inside the same
chronological walk, at the same guard (`baseline != null && isToday`) that
produces today's residuals — so a lapse candidate naturally inherits the
fast-tail floor and 5-minute hard-drop exclusions already sitting earlier in
the loop (too extreme in either direction to be a momentary attention lapse).
Unweighted plain count, unlike `pctVsUsual`: `r.correct() && rt >= 2.5 *
exp(baseline)`. `PaceTile.vue` renders "N retrieval lapse(s) today" only when
`lapseCount > 0`. API client regenerated. Backend tests split into their own
`RecallStatsServiceLapseAggregationTest.java` (post-change-refactor,
file-size convention) alongside the existing pace-comparison test file.

#### 14. Recall Stats shows today's consistency — Behavior `[x]`

Spread of within-session residuals, standardized against the learner's own
baseline (median and MAD, trailing 60 days, excluding the last 3).

Done: the plan gave no exact formula for this one (unlike slices 10-13), so
the coordinator resolved it before delegating. The same capped-residual
computation used for `pctVsUsual` now also fires for rows in a baseline
window `[today-63, today-4]` (60 days, excluding the 3 immediately before
today), accumulated per day in `residualsByDate`. `todaySpread`/each
qualifying day's spread is MAD (median absolute deviation) of that day's
residuals, requiring ≥2 residuals/day; the baseline needs ≥10 qualifying
days (`MIN_BASELINE_DAYS`) or `consistencyZScore` is null.
`consistencyZScore = (todaySpread − median(baselineSpreads)) /
(mad(baselineSpreads) × 1.4826)` — `1.4826` is the standard MAD-to-SD scaling
constant; null if `baselineMad == 0`. Positive = more erratic than usual,
matching `pctVsUsual`'s sign convention. `PaceTile.vue` shows a
"more erratic than usual" badge only when `consistencyZScore > 1`, mirroring
the low-confidence badge exactly (one-sided; no "more consistent" messaging
in this slice). API client regenerated. This closes the original "Pace channel
— no schema change" implementation (slices 8-14). **Do not start Accuracy
(slice 15) yet** — repair slices 14.1–14.8 first. The ADR 0003 Jidoka before
slice 15 still applies.

### Repair the shipped readouts

Inspection of slices 1–14 (commits `c5c1449bdc`..`15609de4af`). Execute 14.1–14.8
in this order before slice 15. Each is stop-safe: stopping after any of them
leaves the already-shipped timer/pace channel more trustworthy than before.

#### 14.1 Split the thinking-time tracker and drop redundant tracker tests — Structure `[x]`

Done: extracted idle detection into new sibling module
`thinkingIdleDetection.ts` (`createIdleDetector(clock, isRunning)`, 51 lines),
following the existing sibling-module convention (e.g. `folderAdminMutations.ts`
next to `useFolderAdmin.ts`). `useThinkingTimeTracker.ts` now 245 lines (was
281), just wiring the extracted detector in. Deleted
`useThinkingTimeTracker.keepAlive.spec.ts` and the weaker KeepAlive-deactivation
case in `QuestionDisplay.thinking.spec.ts`; the stronger detour case remains.
`thinkingTimeTrackerTestSupport.ts`'s `setupTrackerClock()` now returns an
injectable `Clock` object instead of spying `performance.now()`; the three
tracker spec files pass `{ clock }` explicitly. Removed the redundant
`onMounted` start call in `useQuestionThinkingTime` — the existing
`watch(isActiveQuestion, ..., { immediate: true })` already covers setup.
Production comments referencing "the plan" removed. No DTO/API/schema change.

#### 14.2 A silent device suspend is not recorded as idle — Behavior `[x]`

Done: `reconcileGap()` now calls `thinkingIdleDetection.ts`'s existing
`markActivityAt(now)` when a dropped gap exceeds `SUSPEND_GAP_THRESHOLD_MS`,
rebasing the idle detector's activity baseline so the next watchdog
`checkIdle()` doesn't attribute the sleep to idle. `stop()` now calls
`checkIdle()` before `isRunning.value` flips false, flushing any
in-progress idle-accumulating stretch instead of losing up to one watchdog
interval. `useThinkingTimeTracker.ts` stayed under the 250-line budget (249
lines). New tests in `useThinkingTimeTracker.idle.spec.ts`: a 6-hour silent
clock jump excludes from both thinking time and idle time; a `stop()` mid
idle-accumulation flushes the partial stretch without waiting for the
watchdog. Existing suspend tests (thinking-time-only claim) unchanged and
still passing.

#### 14.3 Pace and retention exclusion use on-task time, not the trend-chart caps — Behavior `[x]`

Done: extracted the shared raw-extraction logic (thinkingTimeMs, diff-fallback
only when null) into `RecallAnswerRow.rawElapsedMs()`, a single source of
truth used by both a new `RecallPaceAggregator.onTaskTimeMs` (uncapped, own
floor/hard-drop, feeds pace/retention) and the existing
`RecallStatsAggregator.responseTimeMs` (unchanged 1s-drop/120s-cap/300s-diff-cap
policy layered on top, still feeding only the trend/AM-PM charts). A 200ms
correct mistap now lands in `implausiblyFastRows` and is excluded from
`totalReviews365`/retention, not just pace. `thinkingTimeMs = 180_000` (3 min)
now feeds `pctVsUsual` uncapped instead of being clamped to 120s.
`thinkingTimeMs >= 300_000` hard-drops via the real field, not only the
null-fallback diff path used in older tests. Dead-code wrap-up: removed
unused `noteId` from `RecallAnswerRow` and the `mt.note.id` JPQL selection in
`RecallPromptRepository`; confirmed and removed the dead
`weightedMedian` total-weight-zero branch (every recorded residual carries
`priorObservationCount >= 1`, so `weight >= 0.25` and `totalWeight` can never
be 0 when the list is non-empty); fixed the stale field-count comment in
`RecallStatsService`. `RecallStatsServicePaceAggregationTest` split into
itself (core weighted-median/confidence mechanics) plus a new
`RecallStatsServicePaceExclusionTest` (exclusion/hard-drop/winsorization) to
stay under the 250-line file-size convention.

#### 14.4 Spelling answers record away, detour, and idle — Behavior `[x]`

Done: `AnswerSpellingDTO` gained `awayMs`/`awayCount`/`detourMs`/`detourCount`/
`idleMs` (matching `AnswerDTO` exactly); `SpellingRecallGrading` copies them
onto the persisted `Answer` entity via a shared `applyAnswerTimingMetrics`
helper (was duplicated across its two overloads). `SpellingQuestionDisplay.vue`
now destructures these from `useQuestionThinkingTime` into its emit payload.
**Gap found beyond the stated scope:** `Quiz.vue`'s `onSpellingAnswer` was
hand-picking only `spellingAnswer`/`thinkingTimeMs` into the API call body
(unlike MCQ, which forwards the whole `answerData` object), which would have
silently dropped the new fields one hop after the emit fix — changed to
`body: answerData` to forward everything, removing the hand-picking pattern
that caused the gap so it can't recur for future fields. Added the missing
MCQ `idleMs` persist test (sibling to existing away/detour persist tests)
plus a spelling equivalent. `RecallHistory.vue` needed no changes — its
away/detour/idle rendering is already generic on `item.recallPrompt.answer`.
MemoryTracker's "does not display away"/"does not display detour" tests
merged into one canonical uninstrumented fixture, extended to assert idle
stays silent too. API client regenerated (`AnswerSpellingDto` gained the new
optional fields).

#### 14.5 Answers are stamped with the scheduling clock — Behavior `[x]`

Done: `currentUTCTimestamp` now threads through `Answer.buildAnswer(...)` →
`AnswerService.createAnswerForQuestion(...)` → `RecallQuestionService` (which
already received it) and both `SpellingRecallGrading` construction sites, so
`Answer.createdAt` is stamped from `TestabilitySettings.getCurrentUTCTimestamp()`
instead of `System.currentTimeMillis()`. Production-neutral: that method
returns real wall-clock time when no time-travel is set. Verified by a new
controller test (`shouldStampAnswerCreatedAtWithTheTestabilityClock`) and by
direct DB inspection during E2E runs — answers now persist with their
simulated day's timestamp, not real wall-clock time.

**`recall_stats.feature`'s pace scenario stays `@wip` — new root cause found,
not the one this slice targeted.** The clock-mismatch fix works (confirmed
above), but `answerSlowlyOnDay`'s repeated `backendTimeTravelTo(day, 8)` +
`submitWrongMcqRecallAnswer(...)` pairs are built on generated-SDK calls
whose underlying `fetch` fires eagerly at call time rather than being
deferred until Cypress's command queue reaches that step. Looping this
across days dispatches overlapping time-travel + answer requests before
earlier ones resolve, so the backend's shared `@ApplicationScope`
testability clock gets overwritten mid-flight — confirmed via DB inspection
showing two of three expected answers landed on the same simulated day. The
pace aggregator then never sees the two-prior-day baseline it needs. This is
an E2E step/SDK sequencing race, unrelated to `Answer.createdAt` — out of
scope here; a future fix would sequence these steps so each request's
promise resolves before the next fires. The backend controller test above is
this slice's actual verification.

#### 14.6 Returning to recall does not remount an in-flight question unless the due window changed — Behavior `[x]`

Done: `useRecallPageLoading.ts`'s `onActivated` no longer compares `new
Date()` to `currentRecallWindowEndAt`. `loadSessionStrips` now returns the
fetched `DueMemoryTrackers` response (mirroring `loadMore`'s existing
pattern in the same file); `onActivated` awaits it and only calls
`loadCurrentDueRecalls()` (which clears/remounts `toRepeat`) when
`response.currentRecallWindowEndAt !== currentRecallWindowEndAt.value` — the
due-window identity, not wall-clock staleness. New
`RecallPage.activation.spec.ts` covers both directions: unchanged window on
reactivation leaves `toRepeat` alone; a genuinely changed window (production
half-day rollover) still refreshes it. `recall_timing.feature`'s detour
scenario un-`@wip`'d and passes end-to-end.

#### 14.7 Viewing a previous answer's E2E scenario passes — Behavior `[x]`

Done: the slice 1 scenario is green without `@wip`. It stubs a known MCQ via
`@usingMockedOpenAiService` (learner-facing `getRecallPrompt` strips
`correctAnswerIndex`). Other scenarios in the file keep `@disableOpenAiService`.
View-history linger uses a real `setTimeout` (not `cy.wait` under `cy.clock()`,
which would fake-tick and break Vue clicks). `resumeRecall` does `cy.tick(1)`
so the restored question is clickable. Recall History exposes
`data-thinking-time-ms`; the Then asserts that raw number on the understanding
tracker (the current MCQ, not spelling). No second unit test.

#### 14.8 Cold-start items stop dominating the consistency badge — Behavior `[x]`

Done: `RecallPaceAggregator` gained a `weightedMad(List<WeightedResidual>)`
helper — weighted median of residuals, then weighted median of the deviations
from that median — reusing the existing `weightedMedian` machinery rather
than duplicating it. `consistencyZScore`'s `todaySpread` now calls
`weightedMad(todaysResiduals)` instead of the old plain
`mad(todaysPlainResiduals)`, so the same `w_j = priorObservationCount/(priorObservationCount+3)`
weight already used for `pctVsUsual` also down-weights cold-start items in
today's spread. `residualsByDate` (the 60-day baseline spread) is
deliberately untouched — still plain/unweighted, per this slice's stated
scope. New backend test in `RecallStatsServiceConsistencyAggregationTest`
covers a morning with several wildly-spread cold-start items (weight 0.25
each) plus one tight, well-established item (weight ≈0.91): confirmed the
score is dominated by the established item instead of flipping "more erratic
than usual" on cold-start noise alone. **Gap found in two pre-existing
tests:** `weightedMedian` resolves an exact half-total-weight tie by
returning the lower value — a tie-break an existing pinned test elsewhere
relies on, so it was not changed — which two older consistency tests'
"today" data (exactly 2 equal-weight symmetric residuals) happened to hit
once `todaySpread` became weighted; fixed by adding a third equal-weight
zero-residual item to those tests' "today" construction (odd-sized sample
sidesteps the tie), preserving each test's original intent. Deleted two
`PaceTile.spec.ts` tests that only asserted a badge is absent when its
backing field (`confidence`, `consistencyZScore`) was omitted from props —
redundant with canonical render tests that already omit those fields. No
DTO/API/schema change.

### Accuracy channel

#### 15. Memory-state columns on `recall_log` — Structure `[x]`

Done: migration is `V300000303__add_memory_state_to_recall_log.sql` (302 was
already taken by slice 4's `answer` pause-tracking migration, as anticipated —
version recomputed fresh from the migration directory). Adds `stability_before
FLOAT NULL`, `difficulty_before FLOAT NULL`, `retrievability DOUBLE NULL` to
`recall_log`, no defaults (NULL means "predates this instrumentation", same
convention as slice 4). `RecallLog.java` gained matching `Float
stabilityBefore`, `Float difficultyBefore`, `Double retrievability` fields
with per-field `@Column`/`@Getter`/`@Setter`, matching this entity's existing
style. `docs/database-erd.md` regenerated — no diff, since none of the new
columns are keys/FKs (same precedent as slice 4). Nothing reads or writes
these fields yet — that's slice 16. Verified via a controller test that
forces a real Flyway migration apply + Hibernate schema validation
(`MemoryTrackerRecallHistoryControllerTest`, `--rerun-tasks`), not a fake
behavior test, since this is a pure Structure slice. **Jidoka checkpoint
resolved beforehand** — see the resolution note above and ADR 0003 (commit
`5a8b19c085`). **Gap found in CI, not by this slice's own verification:**
the new fields have public getters and `RecallLog` is nested directly inside
`RecallHistoryItem` (no `@JsonIgnore`), so they were already on the wire —
`RobotsTests.openApiDocsMatchCommittedYaml` failed until `open_api_docs.yaml`
and the generated TS client were regenerated (commit `c72eae010e`). A
Structure slice that adds fields to an entity nested in a serialized DTO is
not exempt from API regeneration even when nothing yet reads/writes those
fields — check for this the next time a "Structure only, no DTO/API change"
slice touches an entity embedded in a response DTO.

#### 16. Recall History shows what recall was predicted — Behavior `[x]`

Done: `persistRecallLog` (`MemoryTrackerService.java`), the single choke point
creating every `RecallLog` row, now sets `stabilityBefore`, `difficultyBefore`,
and `retrievability` right after `elapsedHours`. `MemoryTracker.java` gained
`public double retrievabilityAt(Timestamp now)`, delegating to the existing
package-private `Fsrs.retrievabilityFromHours`. **Real ordering bug found and
fixed:** `persistRecallLog` has two call sites, and they called it in opposite
order relative to the tracker's Stability/Difficulty mutation — the Grade path
(`markAsRecalled`) already called it *before* `applyGrade` (correct), but the
Confusion path (`SpellingRecallGrading.java`, accidental spelling match)
called it *after* `applyConfusionAdjustment`, which would have silently
captured the already-adjusted state as "before." Fixed by reordering the
Confusion path's two calls to match the Grade path's pattern; a new
regression test (`MemoryTrackerRecallHistoryRetrievabilityTest.java`) asserts
pre-adjustment state specifically on the Confusion path, not just the
happy-path Grade case. `RecallHistory.vue` renders `Predicted: NN%` beside
the existing outcome/recorded/elapsed-hours row, shown only when
`retrievability != null` (old rows stay silent). No migration, no API
regeneration needed (slice 15's already-done regen covers these fields).

**Second gap found in CI, not by this slice's own tests:** grading a New
tracker (never graded, `stability = 0`) for the first time made
`Fsrs.retrievabilityFromHours` divide `0/0` (elapsed is also 0 with no prior
`lastRecalledAt`), producing `NaN`, which crashed on JDBC bind
(`'NaN' is not a valid numeric value`) — broke `LearningSessionRecordTests`
and two accidental-match E2E scenarios on `main`. The implementer's own tests
used `ownedTracker()`/`ownedSpellingTracker()` fixtures, which set
`stabilityAndNextRecallAt(200.0f)` — never a genuinely New tracker — so the
edge case wasn't caught locally. Fixed: `retrievabilityAt` returns `null` for
a New tracker (`isNew()`), since Retrievability is undefined before any grade
exists; `RecallHistory.vue`'s existing `!= null` guard already handles it.
New regression test added with a real New-tracker fixture
(`makeMe.aMemoryTrackerFor(note).please()`, no stability override). **Lesson
for future slices touching FSRS inputs:** a fixture that pre-sets stability
to establish a tracker (the common case for testing an already-scheduled
item) will never exercise the New/first-grade edge case — test that
explicitly when a formula divides by Stability or Difficulty.

#### 17. Recall Stats shows today's accuracy against expected — Behavior `[x]`

Done: `RecallAnswerRow` gained a `retrievability` field, sourced from
`RecallLog.retrievability` via a new `rl.retrievability` selection in
`RecallPromptRepository.findAnsweredRecallAnswerRows`'s existing JPQL
projection — no second query. New `RecallAccuracyAggregator.compute(...)`
(dedicated class, mirroring `RecallPaceAggregator`'s cohesion rather than a
method bolted onto it, since accuracy and pace are distinct concepts) sums
`A = Σ(y−p̂) / √Σp̂(1−p̂)` over qualifying rows, excluding any row with null
retrievability (a New tracker's first grade, per slice 16) and returning null
when the denominator is 0 (no qualifying rows, or all p̂ at 0/1) rather than
dividing by zero. `RecallStatsService.aggregateRows` collects today's
qualifying rows from the same loop that already skips
`paceResult.implausiblyFastRows()`, so implausibly-fast mistaps are excluded
from accuracy too — consistent with the plan's stated design decision that a
mistap invalidates the accuracy observation, not just the timing. New
`RecallStatsDTO.AccuracyStats { standardizedResidual, sampleSize }` field
`accuracy`; new `AccuracyTile.vue` rendered beside `PaceTile.vue` in
`RecallStatsSettingsTab.vue`. Pace's own time-expectation formula
(`RecallPaceAggregator`) was not touched, per the slice-17/17.1 split above.
API client regenerated. No new E2E scenario — slice 9's pace E2E precedent
(backend/frontend unit tests as primary verification) followed, since the
existing `recall_stats.feature` pace scenario is already `@wip` for an
unrelated SDK-sequencing race documented in Discoveries.

#### 17.1. Pace expectation is corrected for retrievability and difficulty — Behavior `[dropped]`

**Split from slice 17** (pre-slice Jidoka stop — developer chose to split
rather than have the coordinator guess a formula). Slice 9's interim note
says the per-item EWMA time baseline has no retrievability/difficulty
correction, and slice 17's original text said this slice would "remove that
interim by feeding `R` and `D` into the pace expectation," but neither slice
specifies the correction model itself (e.g. a multiplicative adjustment to
the EWMA baseline, a regression term, or something else).

**Dropped, not implemented.** The developer decided against specifying a
formula at all, given 21.9's finding that retrievability is null for ~95% of
reviews and even populated values come from recently-backfilled data of
unproven accuracy — building a correction on top of a mostly-absent, unproven
signal contradicts "keep it simple." Slice 9's uncorrected per-item EWMA
baseline is the permanent pace expectation. Verified this leaves no dead code:
`RecallPaceAggregator.java` has zero references to retrievability/difficulty
and never had a stub or hook anticipating this slice; the `retrievability`
field slice 17 added to `RecallAnswerRow` is fully consumed by the accuracy
pipeline only (`RecallAccuracyAggregator`/`RecallCalibrationFitter`/
`RecallGuessingFloorFitter`); `PaceStats`/`PaceTile.vue` have no unwired
fields. Slice 17 built nothing speculative for pace, so nothing needs
cleanup.

#### 18. Historical reviews gain their memory state — Behavior `[x]`

Done: **location deviates from this plan's literal text** — "under `scripts/`"
turned out to have no precedent for JVM/JPA-touching data repair (that
directory is all shell/Node dev tooling); the repo already has an established,
actively-used convention for exactly this instead —
`backend/src/main/java/com/odde/donut/services/` backfill classes
(`NotePropertyTrackingBackfill.java`, `NotePropertyIndexTargetNoteBackfill.java`:
`public final class`, private constructor, static `run(...)`, invoked only
from a `@SpringBootTest`, no wired runner). New
`RecallLogMemoryStateBackfill.java` follows that shape. For each tracker with
NULL `stability_before` rows, replays its `recall_log` rows oldest-first
through the real production `MemoryTracker` methods (`applyGrade`,
`adjustForConfusion`, `retrievabilityAt`) on a scratch, never-persisted
`MemoryTracker` — no FSRS math reimplemented — writing each row's "before"
snapshot prior to applying that row's own effect (state is derived purely
from the row sequence, so it can't reintroduce slice 16's Grade-vs-Confusion
ordering bug). Each row's stored `elapsed_hours` doubles as a checksum: on
mismatch, that row and everything later for the tracker is left NULL rather
than guessed (unreplayable → NULL → excluded from the index). Stops at the
first row that already has `stability_before` set (the live-instrumented
boundary — new data is always the tail). New `RecallLogRepository` queries
`findAllByMemoryTracker_IdOrderByRecordedAtAscIdAsc` and
`findDistinctMemoryTrackerIdsWithNullStabilityBefore`. New
`RecallLogMemoryStateBackfillTest` (4 tests): grade-path history,
confusion-path history from a genuine New tracker, a corrupted-`elapsed_hours`
row correctly left NULL along with everything later, and the
live/pre-instrumented boundary correctly stopping without touching
already-populated rows. `answer` pause columns confirmed not backfilled, per
plan. Not run against production data — building/testing the script was this
slice's deliverable; running it is an operator action outside this plan.

#### 19. Personal recalibration removes the scheduler's bias — Behavior `[x]`

Done: the plan's second sentence was garbled/unparseable
("A learner FSRS is consistently overconfident about stops reading as
permanently below par") — the coordinator confirmed real intent with the
developer before delegating rather than guessing. **Clarified intent:** fit a
2-parameter logistic (Platt-scaling) recalibration
`p̂ = sigmoid(α + β·logit(retrievability))` from trailing history, so a
scheduler that's systematically over/under-confident for a learner doesn't
make the accuracy readout perpetually read "worse than expected" for reasons
that are really the scheduler's bias, not the learner's. New
`RecallCalibrationFitter.java` — Newton-Raphson/IRLS fit with backtracking
line search (a bare Newton step oscillated/diverged; step-halving on
log-likelihood improvement fixed it); no new dependency (no numerics library
existed in this repo — hand-rolled following `Fsrs.java`'s self-contained
numeric style). `RecallAccuracyAggregator` fits calibration live per request
from trailing-180-day rows (excluding today, non-null retrievability,
non-implausibly-fast), then uses the recalibrated p̂ in place of raw
retrievability in slice 17's `A = Σ(y−p̂)/√Σp̂(1−p̂)` sum. **"Refit nightly"
resolved as live-per-request, not a new scheduled job**: this codebase has no
`@Scheduled`/cron mechanism for this kind of statistic; trailing-window
recalculation happens inline per stats request, matching the existing
`consistencyZScore` baseline pattern in `RecallPaceAggregator`.
`MIN_CALIBRATION_SAMPLES = 50` trailing qualifying rows; below that, or on
no-outcome-variance, or a numerically degenerate fit (singular Fisher
information, NaN/Infinite, or a non-improving step), falls back to the
identity mapping (α=0, β=1 — raw retrievability unchanged via a
reference-equality fast path, so pre-slice behavior is exactly preserved
below the threshold). `RecallStatsService` passes `allTimeQualifyingRows`,
`today`, and `zoneId` through. New `RecallCalibrationFitterTest` and
`RecallStatsServiceAccuracyCalibrationTest`; existing accuracy tests
unmodified (none reach the 50-sample threshold, so they exercise the
identity fallback exactly as before). No DTO/schema/controller signature
change, no API regeneration needed.

#### 20. The guessing floor is fitted rather than assumed — Behavior `[x]`

Done, on the second attempt. **A first attempt was rejected**: it fit α/β
once unconditionally and only grid-searched γ against that fixed fit —
numerically safe but biased low (a true injected γ=0.3 recovered as only
~0.05). The developer chose to redo it properly rather than accept the
damped estimate. The accepted implementation does the mathematically correct
**conditional refit / profile likelihood**: new
`RecallGuessingFloorFitter.java` grid-searches γ over `[0, 0.5]` (step 0.02),
and at each candidate γ runs a fresh Newton-Raphson (BHHH
outer-product-of-gradients Hessian approximation, backtracking line search,
warm-started via continuation from the previous grid point's converged α/β)
to *refit* α/β conditional on that γ, maximizing the exact 3PL log-likelihood
`p̂ = γ + (1−γ)·σ(α+β·logit(retrievability))`. The γ with the highest
profile log-likelihood wins. Verified against a finite-difference
gradient/Hessian check (analytic vs. numerical gradient agreed within
`1e-4`), catching the sign/derivative bugs this kind of math invites.
**Sanity checks (on synthetic data):** spelling-shaped data (no genuine
guessing floor) fit γ = 0.0 (bar was ≤0.02); MCQ-shaped data with an injected
true γ=0.3 fit γ ≈ 0.28 — confirming the refit is genuinely conditional,
unlike the rejected attempt. Below `MIN_TRAILING_REVIEWS = 300` qualifying
rows for a question type, γ is held at exactly 0, delegating straight to
slice 19's `RecallCalibrationFitter` 2PL fit (which separately still enforces
its own `MIN_CALIBRATION_SAMPLES = 50` threshold — two independent
thresholds gating two different parameters). "Fitted per question type" uses
the existing `RecallPrompt.questionType` (`MCQ`/`SPELLING`) discriminator,
added to `RecallAnswerRow`/the JPQL projection (projection-only, no second
query, no API regen — slice 8/17 precedent);
`RecallAccuracyAggregator` groups both today's rows and the trailing
calibration rows by type and fits/applies an independent `ThreePlFit` per
type, so MCQ and spelling never share a guessing floor. Post-change-refactor
extracted the `clamp`/`logit`/`sigmoid` primitives duplicated between slice
19's and slice 20's fitters into a shared `RecallProbabilityMath.java`; the
Newton-Raphson loop bodies themselves were deliberately left unshared since
the 2PL analytic-Hessian scoring and 3PL BHHH-approximation scoring are
genuinely different math.

### The index

Slice 21 was split into 21.1–21.4 below (pre-slice Jidoka: computing "the
index" for the first time needed a composite formula the plan never
specified — resolved with the developer, see the composite-formula note at
21.2 — and then turned out to need day-level baselines that don't exist yet
for two of the four components, more than one observable behavior).

**Composite formula (developer-approved, applies from 21.2 onward):**
`index = 100 − 10 × mean(zA, zPace, zLapse, zConsistency)`, equal-weight, no
per-component weight tuning at this stage — the plan explicitly says "do not
tune weights to rescue the number." Each z is signed so positive =
worse-than-usual; `zA` and `zLapse` are sign-flipped (higher-raw-value =
better) so all four share pace/consistency's "positive = worse" convention.

#### 21.1 Pace and lapses gain day-level baselines like consistency — Structure `[x]`

Done: `RecallPaceAggregator`'s `residualsByDate` changed from
`Map<LocalDate, List<Double>>` to `Map<LocalDate, List<WeightedResidual>>` so
baseline-window days retain each residual's cold-start weight, not just its
value. New `Map<LocalDate, Integer> lapseCountByDate`. New
`paceDayBaseline(...)` reuses the exact `weightedPctVsUsual` transform
already used for today's tile, applied per baseline-window day; new
`lapseDayBaseline(...)` computes median/MAD of each day's plain lapse count.
Both route through a new shared `dayBaseline(...)` helper (returning a
`DayBaseline(median, mad)` record) gated by the existing `MIN_BASELINE_DAYS`
constant — post-change-refactor consolidated `consistencyZScore`'s own
previously-inline gate/median/MAD logic onto this same helper, so "gated
day-level median/MAD" now has one representation in the file instead of
three near-duplicates. `PaceResult` gained `paceDayBaseline`/
`lapseDayBaseline` fields — deliberately internal (not `PaceStats`/DTO), per
this slice's Structure-only scope; no composite, no wiring, no user-facing
change. New `RecallPaceAggregatorDayBaselineTest.java`. File size (269 lines,
~8% over the 250-line convention) was reviewed and left as one file — the
generic-looking statistics helpers (`weightedPctVsUsual`, `averageWeight`)
are pace-domain-specific, not generic math, so a split would be artificial.

#### 21.2 The composite index can be computed for any single day — Structure `[x]`

Done: new `RecallCognitiveIndex.java` (package-private, final, static-only,
mirroring slice 20's `RecallProbabilityMath` shape) — `static double
compute(double zA, double zPace, double zLapse, double zConsistency)` →
`100 - 10 * mean(...)`. No dependency on `RecallStatsService`, aggregators,
or Spring/JPA — pure arithmetic, deliberately narrow: it assumes its four
inputs are already correctly-signed z-scores and does not decide how
`A`/`lapseCount` get sign-flipped into that convention (javadoc notes that
inversion is deferred to 21.3, which has the real per-morning values to wire
up). New `RecallCognitiveIndexTest.java` (4 tests: all-zero baseline → 100,
uniform positive/negative z's, mixed z's).

- **Enables 21.3 only.**

#### 21.3 A morning's index can be computed from just its odd or even attempts — Structure `[x]`

Done: `RecallPaceAggregator.compute` gained an overload taking an explicit
`Set<RecallAnswerRow> todayRowsToScore`, splitting the existing `isToday`
(still drives the unconditional per-item EWMA update — baselines are never
rebuilt per half) from a new `scoreToday` gate
(`isToday && (todayRowsToScore == null || todayRowsToScore.contains(r))`)
that restricts `totalAnsweredToday`/residual/lapse capture to the given
subset; the original 3-arg overload delegates with `null` (unchanged full-day
behavior for every existing caller). New `RecallMorningHalfIndex.compute(...)`
(package-private `Half { ODD, EVEN }`): for a given day D, runs a full-day
`RecallPaceAggregator.compute` once for `implausiblyFastRows` (invariant to
the half), 1-indexes D's qualifying rows chronologically into the two halves
(identity-based `Set`, matching slice 10's precedent), then scores each half
by calling `RecallAccuracyAggregator` with just that half's rows and
re-invoking `RecallPaceAggregator.compute` with the half `Set` for
pace/lapse/consistency — the trailing-window calibration and day-baselines
are derived from history before D exactly as if D were "today," never
rebuilt per half, and each half is z-scored against the *same* existing
full-day baseline (`paceDayBaseline`/`lapseDayBaseline`/consistency spread),
not a separate half-day baseline, since a shared reference is all a
split-half correlation needs. Returns `null` if any component is unavailable
(e.g. a half with <2 consistency residuals).
**Sign-convention correction found during implementation, and confirmed
correct on review:** `zA = -A` (accuracy is higher-is-better, needs
flipping), but **lapse needs no flip** — unlike accuracy, `lapseCount` is
already higher-is-*worse* in raw form (more lapses = worse), so its
day-baseline z-score already matches the composite's "positive = worse"
convention directly; flipping it would have silently inverted that
component. This corrects an error in 21's original approved-formula prose
("zA and zLapse are ... sign-flipped"), which mischaracterized lapse's
natural direction — the composite formula itself (`100 −
10×mean(zA,zPace,zLapse,zConsistency)`, positive=worse) is unaffected, only
which raw values need inverting to reach that convention. Post-change-refactor
also fixed 21.2's `RecallCognitiveIndex` javadoc, which had stated the
now-incorrect "zA and zLapse are sign-flipped" claim. New
`RecallMorningHalfIndexTest` (3 tests). Post-change-refactor extracted the
day-baseline/z-score machinery (`DayBaseline`, `dayBaseline()`,
`zScoreAgainstDayBaseline()`, `median()`, `mad()` — generic day-level
statistics, distinct from the per-item EWMA walk) into a new
`RecallDayBaseline.java`, bringing `RecallPaceAggregator` back under the
250-line convention (254 lines) after this slice had pushed it to 297.

- **Enables 21.4 only.**

#### 21.4 The developer can see split-half reliability across recent mornings — Behavior `[x]`

Done: new `RecallSplitHalfReliability.compute(allTimeReviews, today, zoneId)`
enumerates candidate mornings in a trailing 90-day window
(`TRAILING_MORNING_WINDOW_DAYS`, consistent with existing baseline-window
magnitudes elsewhere in this code), pre-filters with
`MIN_QUALIFYING_ROWS_PER_DAY = 4` (2+2, the minimum to feed both halves —
mirrors consistency's own ≥2-residuals-per-half precedent), calls
`RecallMorningHalfIndex.compute` for both halves per candidate day, and
keeps only pairs where **both** halves are non-null (a day where one half
returns null contributes no reliability information and is excluded, not
zero-filled). Reports raw Pearson correlation (null if either series has
zero variance — mathematically undefined) and its Spearman-Brown correction
`2r/(1+r)`, both null below `MIN_PAIRS_FOR_CORRELATION = 10`
(mirroring `RecallDayBaseline.MIN_BASELINE_DAYS`, this codebase's existing
precedent for "how many days before a cross-day statistic is trustworthy").
New `RecallSplitHalfReliabilityDTO { pairCount, rawCorrelation,
spearmanBrownCorrelation }`. New endpoint
`GET /api/user/recall-split-half-reliability` on `UserController`, using the
exact same auth pattern as `getRecallStats` (current-user-only, no new
admin/elevated-role concept — this is per-user diagnostic data like the rest
of Recall Stats). Not wired into `RecallStatsDTO` or any user-facing page.
API client regenerated (no frontend call site — diagnostic-only). New
`RecallSplitHalfReliabilityTest` (Pearson math incl. the zero-variance→null
case) and `UserRecallSplitHalfReliabilityControllerTest`.
Post-change-refactor (reviewing the whole 21.1–21.4 arc for cohesion, not
just this slice's diff) extracted three duplicated helpers in
`RecallStatsService` (`findAllTimeAnsweredRows`, `reviewsOnly`,
`localToday`) shared by `compute()`/`computeSplitHalfReliability()`/
`aggregateRows()`; moved a fully-duplicated test fixture out of
`RecallSplitHalfReliabilityTest` (copy-pasted from
`RecallMorningHalfIndexTest`) into the shared `RecallStatsTestFixtures`; and
extracted `RecallPaceAggregator`'s pure weighted-median/MAD statistics
(`WeightedResidual`, `weightedMedian`, `weightedMad`, `weightedPctVsUsual`,
`averageWeight`, `madOfResiduals`) into a new `RecallWeightedResidualStats`,
bringing `RecallPaceAggregator` to 204 lines.

**This closes slices 21.1–21.4.** The endpoint exists; the developer still
needs to query it against real history and decide against the ~0.6
threshold (by whichever of the two reported numbers is judged appropriate)
whether slices 22–25 proceed. **Do not tune 21.2's formula weights to
rescue a low number** — that would defeat the point of the gate.

- **This is the gate. Resolved: failed, via 21.9 rather than a low
  correlation number.** See Jidoka above and 21.9's section — slices 22–25 are
  dropped; the component readouts stand on their own.

### Repair the accuracy/index readouts (session review)

At the developer's request, a full session-wide code review across every
slice landed this session (17–21.4: accuracy, recalibration, guessing floor,
historical backfill, and split-half reliability) found the gaps below.
21.5–21.8 are done. Each is stop-safe: stopping after any
of them leaves the already-shipped accuracy/index readouts more trustworthy
than before, same as 14.1–14.8 did for the timer/pace channel.

#### 21.5 The reliability endpoint's real-correlation path has regression coverage — Behavior `[x]`

Done: `RecallSplitHalfReliabilityTest.tenScorableMorningsYieldThePearsonOfTheirHalfIndexesAndTheSpearmanBrownCorrection`
builds 10 scorable mornings, independently scores each day's odd/even halves
via `RecallMorningHalfIndex.compute`, and asserts `compute()` returns that
pair count, the same Pearson `r`, and Spearman-Brown `2r/(1+r)`. Vacuous
`spearmanBrownCorrectsTheRawCorrelationUpward` (hardcoded `r = 0.5`, never
called production) deleted. Production already implemented this path;
no formula change.

**Learning for remaining 21.x tests:** the two-value even/odd
`warmedUpBaselines()` pace/lapse pattern majority-votes MAD to 0 when a
later scored morning also sits in an earlier morning's trailing baseline
window, which makes half-indexes null. Multi-day half-index fixtures must
use `variedBaselinesThrough` (3-phase pace/lapse) plus `addScorableMorning`.

#### 21.6 Accuracy documentation and tests reflect recalibration, not raw retrievability — Structure `[x]`

Done: `RecallStatsDTO.AccuracyStats` and
`RecallStatsServiceAccuracyAggregationTest` javadocs now describe the
recalibrated 3PL `p̂` (identity fallback when trailing history is sparse),
cross-referencing `RecallAccuracyAggregator`. Deleted redundant
`todaysAccuracyUsesRawRetrievabilityWhenTrailingHistoryIsSparse`; kept
`todaysAccuracyIsScoredAgainstTheRecalibratedProbabilityWhenTrailingHistoryIsAbundant`.
Aggregation tests unchanged (they remain the sparse/identity-fallback cases).

- **Enables 21.7 only.**

#### 21.7 The reliability diagnostic scores both halves of a day without duplicating expensive work — Structure `[x]`

Done (plan heading said Behavior; implemented as Structure — same numbers).
`RecallMorningHalfIndex.computeBothHalves` prepares one `DaySetup`
(whole-day pace exclusions, qualifying-row order, per-question-type 3PL
`RecallAccuracyAggregator.fit`) and `scoreHalf`s both sides.
`RecallAccuracyAggregator` split into `fit` / `apply`; full-day `compute`
still fit-then-apply. `RecallSplitHalfReliability` calls
`computeBothHalves` once per candidate day. Characterization test:
`scoringBothHalvesTogetherMatchesScoringEachHalfIndependently`. Per-half
`compute(..., Half)` kept for existing tests.

#### 21.8 (optional) Share the Newton-Raphson line-search scaffold between the two fitters — Structure `[x]`

Done: extracted the duplicated iteration / 2×2 Newton step / 30-halving
line search / NaN-Infinite-non-improving bailout / convergence check into
`RecallNewtonRaphson.maximize` (dedicated class; clamp/logit/sigmoid stay in
`RecallProbabilityMath`). Callers still own scoring math: 2PL analytic
Fisher in `RecallCalibrationFitter.score`, 3PL BHHH in
`RecallGuessingFloorFitter.score`. Bailout mapping unchanged (2PL →
`CalibrationFit.IDENTITY`; 3PL grid skips a γ when `converged=false`;
max-iter still counts as success). Numeric tests unchanged.

Not a prerequisite for slice 22.

#### 21.9 Nearly every review's retrievability is null — find why `memory_tracker.stability` reads as New — Structure `[escalated, closed in this plan]`

Found while finally querying 21.4's endpoint against real production data
(prod investigation via three rounds of temporary, response-embedded debug
counters on `RecallSplitHalfReliabilityDTO`, each deployed then reverted —
current production state is clean, byte-identical to before this
investigation). Today's whole-day `RecallStatsDTO.AccuracyStats.sampleSize`
was 4 against `totals.reviewsToday = 61` (`pace.totalAnsweredToday = 85`) —
roughly 5% of today's reviews had a usable retrievability. Across the
split-half diagnostic's 90-day window (91 candidate days), 180 of 182
candidate half-scorings failed on `RecallAccuracyAggregator`'s
zero-*sample* branch (0 rows with non-null retrievability in that half),
not the zero-*variance* branch (a degenerate 3PL fit) — 0 of 180 — which
rules out 21.8's Newton-Raphson refactor as the cause.

`RecallAnswerRow.retrievability` comes from `RecallPromptRepository`'s
`LEFT JOIN RecallLog rl ON rl.answer = a AND rl.memoryTracker = mt AND
rl.grade IS NOT NULL`. `MemoryTracker.retrievabilityAt(now)` — the value
`persistRecallLog` writes on every live-graded answer, present or absent
alike — returns null for exactly one reason: `isNew()`, i.e. `stability <=
Fsrs.NEW_STABILITY_HOURS` (`0.0f`), the field's default. So the finding is
really: as far as `memory_tracker.stability` is concerned, most trackers
this account reviews are perpetually "New" — never observed to have
received a prior grade — despite the account having 126,440 total reviews
and a 200-day streak.

**Developer's hint, not yet verified:** `RecallLog` is a recent addition
and current data was backfilled from previously-answered questions; that
backfill may be incomplete. Worth checking, but note one thing already
established by reading `RecallLogMemoryStateBackfill` (slice 18): it
replays history onto a **scratch, never-persisted** `MemoryTracker` purely
to fill in `recall_log.stability_before` / `difficulty_before` /
`retrievability` retrospectively on old rows — it never writes back to the
*live* `memory_tracker.stability`/`difficulty` columns, and it's explicitly
"not wired to run automatically." So even a fully complete backfill would
not, by itself, explain *today's* live-instrumented answers reading null —
today's `stability` comes from whatever is currently persisted on the
`memory_tracker` row itself, updated by `applyGrade` on every graded
review. That points at a second, more consequential hypothesis worth
ruling in/out first: `memory_tracker.stability` updates from `applyGrade`
are not sticking (a transaction/session issue around
`entityPersister.save`, a stale read elsewhere, or something — a
migration, a reset — that zeroed `stability` back to `NEW_STABILITY_HOURS`
for pre-existing trackers without re-deriving it from history). If true,
this isn't just starving this stats readout — it would silently be
feeding the live FSRS *scheduler* wrong stability for most items too.

**Suggested first step:** a direct read-only query (DB console, not the
stats API) — count `memory_tracker` rows where `stability <= 0` but that
have more than one `recall_log` row — would directly confirm or rule out
the "updates aren't sticking" hypothesis before touching production code
again. If confirmed, check whether `RecallLogMemoryStateBackfill` (or
whatever ran the historical `RecallLog` backfill in production) was
supposed to also seed `memory_tracker.stability`/`difficulty` from the
last-known state and didn't, versus a live persistence bug in `applyGrade`
itself.

- **Blocked the slice 22–25 gate** (see "This is the gate" at 21.4): the
  split-half reliability number cannot be trusted, or even meaningfully
  computed past `pairCount` ≈ 1, until this is resolved — independent of
  whether the composite formula itself is sound.

**Closed in this plan, escalated separately.** Confirming or ruling out the
"`memory_tracker.stability` updates aren't sticking" hypothesis needs a
direct read-only production query the developer runs themselves — outside
`quick/001`'s scope either way. More importantly, if confirmed, it is a live
FSRS *scheduler* bug (wrong stability feeding real scheduling for most items),
not a stats-readout defect — too consequential to leave as a sub-slice of a
stats-display plan. Escalated in
`.planning/notes/memory-tracker-stability-not-persisting.md`. This plan
treats the reliability gate as **failed**, not pending: slices 22–25 are
dropped, per "This is the gate" — the component readouts (pace, accuracy,
consistency, lapses) stand on their own; the composite is not built.

#### 22. Recall Stats leads with the morning index — Behavior `[dropped]`

Hero readout above `RecallStatsTiles`, against a personal baseline of 100, with
shrinkage `n/(n+8)` and sample size. Morning = first qualifying session of the
local day, not a clock-hour bucket.

**Dropped along with 23–25**, per the reliability gate at 21.4/21.9: the
composite index needs retrievability data that's null for ~95% of reviews, so
the gate cannot pass and the composite is abandoned rather than reworked
around missing data. The component readouts (pace, accuracy, consistency,
lapse count) already ship independently and stand on their own.

#### 23. A thin morning says so instead of showing a number — Behavior `[dropped]`

Under ~6 valid attempts the hero reads "not enough reviews this morning",
falling out of the same shrinkage term rather than a separate rule.

Dropped with 22 — no hero readout, no thin-morning case needed.

#### 24. Contribution bars explain the index — Behavior `[dropped]`

Signed A / S / L / V bars turn "96" into "accuracy normal, slower and more
erratic than usual".

Dropped with 22 — no index to explain. **Note for slices 32–33 below**, which
were written assuming this index exists: 32 compares "index versus probe" and
33's trigger condition references "slice 24 shows speed and accuracy moving
against each other" — both need re-scoping if/when the daily-probe slices
(26+) are picked up, since the composite index they reference won't exist.

#### 25. The index gets a trend — Behavior `[dropped]`

Reuses the existing 30 / 90 / All toggle and the established insufficient-data
legend rather than introducing a second control grammar.

Dropped with 22 — no index to trend.

### Daily probe

#### 26. Probe flag on `user` — Structure `[ ]`

`V300000304__add_daily_probe_enabled_to_user.sql`:
`TINYINT(1) NOT NULL DEFAULT '0'`, following `health_remove_empty_folders_default`.
Plus the DTO field and a regenerated TypeScript client.

- The one deliberate exception to the nullable rule: this is a setting, not an
  observation, and default-off is the intended behaviour for existing users.
- **Enables slice 27 only.**

#### 27. The daily probe can be switched on in General settings — Behavior `[ ]`

A checkbox in `GeneralSettingsTab.vue` that persists, default off. Nothing runs
yet — but the learner opts in before anything is measured.

- E2E: extend `users/user_profile.feature`
- Help text must state that turning it off closes the convergent-validity route.

#### 28. The probe runs and shows this morning's result — Behavior `[ ]`

~20 trials, about 60 seconds, offered before the first recall of the day when
enabled. Identical stimuli every day — that is what removes every item confound
by construction.

- **Interim:** result is not stored — removed by slice 30.
- E2E: new `recall/daily_cognitive_probe.feature`

#### 29. `cognitive_probe` table — Structure `[ ]`

`V300000305__create_cognitive_probe.sql`: user FK with CASCADE,
`started_at timestamp(3)`, summary columns, and `trials_json` for the raw
per-trial array.

- Raw trials as well as summaries, deliberately: summary statistics cannot be
  un-summarized, and a revised lapse definition or an EZ fit needs them.
- **Enables slice 30 only.** Regenerate `docs/database-erd.md`.

#### 30. The probe result persists and is offered once a day — Behavior `[ ]`

A second recall session the same morning does not re-prompt.

#### 31. Recall Stats shows the probe trend — Behavior `[ ]`

Mean reciprocal RT, lapses and variability over the same window toggle.

#### 32. Convergent validity is reported against the probe — Behavior `[ ]`

Internal diagnostic: index versus probe on mornings with both, compared against
the index-versus-raw-accuracy correlation. The probe is an independent speeded
task with no shared item structure, which is what makes it a usable criterion.

### Optional tail

#### 33. Rolling EZ-diffusion separates caution from capacity — Behavior `[ ]`

Drift rate and boundary separation over a rolling three-morning window, on the
MCQ subset, fitted on residualized latencies.

- Only worth building if slice 24 shows speed and accuracy moving against each
  other often enough to matter.
- EZ assumes two-choice symmetric boundaries and needs 30–50 trials; RT variance
  is its noisiest input. Rolling, never daily.

---

## Permanent artifacts (capability-named)

| Artifact | Slices |
|----------|--------|
| `e2e_test/features/recall/recall_timing.feature` | 5–7, 14.6 |
| `e2e_test/features/recall/recall_stats.feature` | 9–14, 14.5, 17, 31 |
| `e2e_test/features/recall/daily_cognitive_probe.feature` | 28, 30 |
| `e2e_test/features/recall/browse_answer_and_notes_while_recalling.feature` | 1, 14.7 |
| `e2e_test/features/users/user_profile.feature` | 27 (extend) |
| `scripts/` backfill script | 18 |

## Per-slice wrap-up

Per `.cursor/rules/planning.mdc`: test first and confirm it fails for the right
reason → smallest change to green → `post-change-refactor` on the uncommitted
change → update this plan → commit and push before the next slice. Targeted
`cypress run --spec` only, never the full suite. Unfinished E2E stays `@wip`;
never commit on red.

Migration slices additionally regenerate `docs/database-erd.md`
(`database-erd` skill). Slices changing a controller signature regenerate the
TypeScript client (`generate-api-client` skill).
