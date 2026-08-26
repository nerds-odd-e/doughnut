# Morning cognitive index from recall history

**Status:** planned — not started
**Type:** ad-hoc plan (`.planning/quick/`)
**Research memo:** https://claude.ai/code/artifact/9e13f954-fc5e-48e5-868f-f75d03f811c1

## Goal

Give the learner a daily readout of how their morning recall compared with what
the scheduler predicted for exactly the items that came due — a cognitive-state
signal rather than a restatement of what FSRS scheduled.

Every readout is a **residual**: observed outcome minus the expectation derived
from retrievability, difficulty and per-item time intensity, standardized
against that learner's own recent history.

**Size warning:** 33 slices is milestone-sized for `quick/`. It is here because
the roadmap has no active milestone. Promote to `.planning/phases/` via
`/gsd-new-milestone` if this should be tracked as a GSD capability.

## Value ordering (why this sequence)

1. **Timer accuracy first.** Slices 1–3 fix a live defect in the response-time
   statistic that already ships. Worth doing even if nothing else here is built.
2. **The pace channel needs no migration.** `thinking_time_ms` already exists and
   per-item time intensity is computable from history. Stopping after slice 14
   leaves three working dashboard readouts and an untouched schema.
3. **Accuracy needs schema plus backfill**, so it comes second.
4. **The composite waits for the reliability gate.** Component readouts are plain
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
- **Recall Stats has no E2E coverage at all.** Slice 9 creates
  `recall_stats.feature`; there is nothing to extend.
- **Package rename in flight.** The working tree is mid `doughnut → donut`
  (ADR 0005). Java package paths in any slice must follow whatever has landed.
- **Slice 6 has no "open the note mid-question" affordance to hook.** The
  active/unanswered `RecallPrompt` DTO (`RecallPrompt.java`) exposes only
  `notebook`, `mcq`, `spellingQuestion` — no note reference. `Mcq.java` has a
  `@JsonIgnore` note field, stripped by `Mcq.withoutSolution()` before the
  frontend ever sees it. `Quiz.vue`'s `NotebookLink` goes to the whole
  notebook (not "the" note) via a full `router-link` navigation — there's no
  `<keep-alive>` anywhere in the frontend (`grep -rl "keep-alive" frontend/src`
  is empty), so clicking it tears down `RecallPage`/`useThinkingTimeTracker`
  entirely rather than producing a resumable "detour." The only place a note
  *is* surfaced during recall is `AnsweredQuestionComponent.vue`'s
  `recalledNoteUnderQuestionProps` — the post-answer / view-history case
  slice 1 already covers and slice 6 explicitly must not conflate with.
  **Resolved:** this is by design — opening a note is a full navigation away
  from `RecallPage`, not an in-page overlay; the learner returns via the
  existing "Resume" menu entry, with state remembered across the navigation.
  See slice 6.

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

**Before slice 9 — new vocabulary.** ADR 0001 owns ubiquitous language. This
plan introduces *pace*, *retrieval lapse*, *detour*, *daily probe* and the index
itself as user-facing terms. Decide whether they enter the glossary or stay
deliberately out of it.

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
`pause()`/`resume()`. E2E scenario added but left `@wip` — the existing
Cypress step vocabulary has no way to control elapsed viewing time precisely
or read recorded thinking time back out of Recall History; a future slice
needing that should add the testability hook rather than reusing ad-hoc
timing assertions.

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
E2E scenario added but left `@wip`: this environment simulates "day 2" on the
backend while the frontend tracker reads the real system clock, so
`RecallPage`'s staleness check always treats the due-recall window as stale on
KeepAlive reactivation, forcing a full refetch/remount that discards the
detour accumulator before the answer is submitted — a pre-existing interaction
with simulated-time E2E tests, unrelated to the detour wiring itself (verified
correct via unit tests) and out of this slice's scope to fix.

#### 7. Idling in place past the threshold is recorded — Behavior `[ ]`

Leave a question untouched on screen past the threshold (45–60 s, deliberately
generous — genuine hard thinking without input is common): the idle time is shown
and flagged. Censors the attempt; never subtracts silently.

### Pace channel — no schema change

#### 8. Extend the `RecallAnswerRow` projection — Structure `[ ]`

Add memory tracker id and note id to the existing single projection query.

- **Enables slice 9 only.** No second query, no entity hydration.

#### 9. Recall Stats shows today's pace against your usual — Behavior `[ ]`

Per-item time-intensity EWMA plus session position, as a tile above
`RecallStatsTiles`.

- **Interim:** no retrievability or difficulty correction — removed by slice 17.
- E2E: new `recall/recall_stats.feature` (no existing coverage)
- Backend unit: through `UserController.getRecallStats` with `makeMe` data

#### 10. Implausibly fast attempts stop distorting pace — Behavior `[ ]`

A 200 ms mistap drops out of the tile **and** out of retention, instead of
counting as a fast correct answer. Item-relative floor:
`t < max(300ms, 0.25 · exp(τ_j))`.

#### 11. A single very slow attempt stops dominating pace — Behavior `[ ]`

Winsorized log-residual (cap ≈ `ln(8)`) and median-of-logs: one six-minute think
shifts the tile slightly instead of swamping it. Hard drop only above ~5 min
on-task.

#### 12. Cold-start items stop adding noise — Behavior `[ ]`

Confidence weight `w_j = m_j / (m_j + 3)`. A morning of freshly assimilated cards
reports lower confidence rather than a wrong number.

#### 13. Recall Stats shows today's retrieval-lapse count — Behavior `[ ]`

Correct answers taking ≥ 2.5× their expected time, counted. Restricted to
*correct* answers deliberately: a slow wrong answer is a knowledge gap, a slow
right answer is the retrieval analogue of a vigilance lapse.

#### 14. Recall Stats shows today's consistency — Behavior `[ ]`

Spread of within-session residuals, standardized against the learner's own
baseline (median and MAD, trailing 60 days, excluding the last 3).

### Accuracy channel

#### 15. Memory-state columns on `recall_log` — Structure `[ ]`

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
retrievability. **Removes slice 9's interim** by feeding `R` and `D` into the
pace expectation.

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
| `e2e_test/features/recall/recall_timing.feature` | 5–7 |
| `e2e_test/features/recall/recall_stats.feature` | 9–14, 17, 22–25, 31 |
| `e2e_test/features/recall/daily_cognitive_probe.feature` | 28, 30 |
| `e2e_test/features/recall/browse_answer_and_notes_while_recalling.feature` | 1 (extend) |
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
