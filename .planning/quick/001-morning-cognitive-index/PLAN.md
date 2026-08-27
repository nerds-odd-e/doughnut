# Morning cognitive index from recall history

**Status:** in progress — slices 1–14, 14.1–14.7 done; **next: 14.8** (repair
shipped readouts before Accuracy). `recall_stats.feature`'s pace scenario
stays `@wip` — a second, unrelated E2E race condition was found (see
Discoveries).
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
  `useRecallPageLoading` onActivated compares `new Date()` to
  `currentRecallWindowEndAt`, so a simulated day-2 window is always "stale"
  vs 2026; KeepAlive reactivation refetches and remounts Quiz, discarding the
  detour accumulator; the detour scenario in `recall_timing.feature` is `@wip`.
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

**Before slice 15 — ADR 0003 tension.** ADR 0003 states Retrievability "is a
scheduling input, not part of the persisted current memory state", defines
RecallLog as the history of "Grades and Confusion", and asserts "the state can
be rebuilt from RecallLog". Persisting `stability_before` / `difficulty_before`
/ `retrievability` on `recall_log` extends that definition.

The honest justification is **query cost, not correctness**: the ADR freezes the
FSRS profile, so replay is valid, and the columns are a materialized cache of a
derivable value. Alternative design: compute by replay at query time and add no
columns at all. Decide, and amend ADR 0003 or record why no amendment is needed
— do not drift silently. Humans own the advice process.

**Before slice 9 — new vocabulary.** **Done** (commits `97cc69940f`,
`f67d894175`): *pace*, *retrieval lapse*, *detour*, *away*, *idle*, *daily
probe*, and *cognitive index* are in ADR 0001 / ADR 0003.

**Before slice 22 — the reliability gate.** If slice 21 reports split-half
reliability below ~0.6, slices 22–25 do not ship. The component readouts stand
on their own and the composite is abandoned or reworked. Do not tune weights to
rescue the number.

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

- **Interim:** no retrievability or difficulty correction — removed by slice 17.
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

#### 14.8 Cold-start items stop dominating the consistency badge — Behavior `[ ]`

A morning of mostly new cards with one established item no longer flips
"more erratic than usual" just because the new cards' residuals are noisy.
Apply the same `w_j = m_j/(m_j+3)` already used for `pctVsUsual` to today's
spread (unweighted MAD of capped residuals is the gap slice 14 left).

- Backend unit: through `aggregateRows` — unique claim is `consistencyZScore`
- Wrap-up: delete PaceTile tests that only re-assert a badge is absent when
  the field is omitted (canonical render tests already omit those fields)

### Accuracy channel

#### 15. Memory-state columns on `recall_log` — Structure `[ ]`

**Do not start until 14.1–14.8 are done.**

`V300000302__add_memory_state_to_recall_log.sql`: `stability_before FLOAT NULL`,
`difficulty_before FLOAT NULL`, `retrievability DOUBLE NULL`, plus entity fields.

- **Note:** slice 4 already claimed `V300000302` — compute the actual next
  available version number from `backend/src/main/resources/db/migration/` at
  implementation time rather than reusing this stale filename.
- **Jidoka first — see ADR 0003 tension above.**
- **Enables slice 16 only.** Regenerate `docs/database-erd.md`.

#### 16. Recall History shows what recall was predicted — Behavior `[ ]`

Answer a card: the history row shows the predicted recall probability beside the
outcome. Makes the new snapshot immediately verifiable instead of write-only.

#### 17. Recall Stats shows today's accuracy against expected — Behavior `[ ]`

Standardized Poisson-binomial residual `A = Σ(y−p̂) / √Σp̂(1−p̂)` on raw FSRS
retrievability. **Removes slice 9's remaining interim** by feeding `R` and `D`
into the pace expectation. Pace already uses on-task thinking time (14.3);
this slice must not reintroduce the trend-chart 1s/2min caps.

#### 18. Historical reviews gain their memory state — Behavior `[ ]`

One-time script under `scripts/`, replaying each tracker's ordered grades and
`elapsed_hours` through FSRS. The accuracy trend visibly extends backwards past
the deploy date.

- **Not a Flyway migration** — the repo's migration rule keeps one-off data repair
  out of the permanent chain, and this needs the FSRS implementation.
- Unreplayable rows keep NULL; NULL means excluded from the index.
- `answer` pause columns are **not** backfillable — that data was never recorded.

#### 19. Personal recalibration removes the scheduler's bias — Behavior `[ ]`

`logit` α and β fitted on trailing 180 days, refit nightly. A learner FSRS is
consistently overconfident about stops reading as permanently below par.

#### 20. The guessing floor is fitted rather than assumed — Behavior `[ ]`

3PL γ, bounded to [0, 0.5], held at 0 until ~300 trailing reviews. Fitted per
question type; spelling's γ landing near zero is the built-in check that the fit
is sane.

### The index

#### 21. Split-half reliability is reported internally — Behavior `[ ]`

Odd/even split of each morning's attempts, index computed on each half,
correlated across mornings. Internal diagnostic endpoint; the developer is the
observer. Composite computed internally, no user-facing number yet.

- **This is the gate.** See Jidoka above.

#### 22. Recall Stats leads with the morning index — Behavior `[ ]`

Hero readout above `RecallStatsTiles`, against a personal baseline of 100, with
shrinkage `n/(n+8)` and sample size. Morning = first qualifying session of the
local day, not a clock-hour bucket.

#### 23. A thin morning says so instead of showing a number — Behavior `[ ]`

Under ~6 valid attempts the hero reads "not enough reviews this morning",
falling out of the same shrinkage term rather than a separate rule.

#### 24. Contribution bars explain the index — Behavior `[ ]`

Signed A / S / L / V bars turn "96" into "accuracy normal, slower and more
erratic than usual".

#### 25. The index gets a trend — Behavior `[ ]`

Reuses the existing 30 / 90 / All toggle and the established insufficient-data
legend rather than introducing a second control grammar.

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
| `e2e_test/features/recall/recall_stats.feature` | 9–14, 14.5, 17, 22–25, 31 |
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
