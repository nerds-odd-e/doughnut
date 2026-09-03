# Recall E2E test optimization

Status: in-progress

**Execution:** do **not** execute until asked. When executing, use **execute-plan** (commit + push per slice).

Scope: `e2e_test/features/recall/` only. Product behavior must not change. E2E that remain should document the system’s external business purpose (precondition → trigger → state change of user value). Overlaps across feature files are merged. Scenarios that only pin presentation, FSRS numbers, or internals already covered by unit tests are replaced by those unit tests (or a new small-test at the stable boundary).

## Profiling baseline (2026-09-03)

Command:

```bash
CURSOR_DEV=true nix develop -c pnpm cy:run-on-sut --reporter json --spec 'e2e_test/features/recall/**/*.feature' --expose tags='not @ignore and not @wip and not @skipOptimizationDueToKnownNecessarySlowness'
```

- **59 tests**, suite wall **~5:39** (Cypress) / **~338s** summed scenario CPU
- Eligible: **59** (excluded: `@skipOptimizationDueToKnownNecessarySlowness` “Browse notes while recalling and come back”; `@wip` `recall_stats.feature`)
- Raw profile: `.planning/quick/042-recall-e2e-test-optimization/recall-profile-results.json` — **do not commit**

### Top 10% slowest (n = ceil(59 × 0.10) = 6)

| # | ms | file | scenario |
|---|-----|------|----------|
| 1 | 11118 | `browse_answer_and_notes_while_recalling.feature` | Viewing a previous answer does not count toward the current question's thinking time |
| 2 | 9428 | `recall_timing.feature` | Switching away mid-question and back records away time and count |
| 3 | 8934 | `spaced_repetition.feature` | Memory Tracker shows Stability and Again Difficulty after incorrect just-review |
| 4 | 8906 | `accidental_match_reveal.feature` | Accidental match reveals reviewed and matched notes |
| 5 | 8403 | `spaced_repetition.feature` | Same-hour Good after first Good stays Stability 55 |
| 6 | 8285 | `accidental_match_scheduling.feature` | Unique matched understanding tracker is brought forward when spelling is absent |

Top 10% total: **~55s**.

File wall (all eligible): `spaced_repetition` 65.7s (11), `property_memory_tracker` 60.4s (14), `daily_probe` 53.8s (9), `accidental_match_reveal` 38.2s (6), `browse…` 32.0s (5), others ≤20s.

### Grouping

- By file (top 10% only): **5** groups
- Batches of 3: **2** groups
- Strict test-opt pick would be **batches of 3**.
- **Chosen for this pass: by capability across all recall features** (not only the 6 slowest). Reason: the user asked to cut **cross-file** redundancy in the whole folder; file wall time is dominated by many medium tests (`property_memory_tracker` 14, `daily_probe` 9) that never enter the top 10%. Tactic 1 (remove redundant tests first) applies to that whole set. The six slowest scenarios are still the first deletions (slices 1–3).

## Optimization rules

1. Remove or simplify redundant tests first.
2. Strictly no fixed-time waits (`cy.wait(ms)` / linger `setTimeout` in recall steps).
3. Flaky = failure.

Hard-to-improve tests: propose under **Candidates** in `.planning/test-optimization-blacklist.md`. Do not add `@skipOptimizationDueToKnownNecessarySlowness` without developer Jidoka. The existing skip on “Browse notes while recalling and come back” stays.

## Inventory: overlaps and excess

### Cross-feature overlaps (merge)

| Duplicate | Files | Keep |
|-----------|-------|------|
| Accidental-match **reveal chrome** (reviewed + matched titles) | `accidental_match_reveal` (all 6) and every `accidental_match_scheduling` scenario | The unique-spelling **scheduling** scenario already shows the reveal, then asserts schedule. Drop the reveal-only file. |
| Due **assimilate/recall counts** | `recall_pages` “Count of recall and assimilate notes”; `spaced_repetition` “The assimilation and recall page” | One count scenario on `spaced_repetition`. |
| **Remove / revive** tracker | `spaced_repetition` “Remove from recall does not change Last Recall Time”; `browse…` remove + revive | One user journey: remove after a recall, then revive. Last-recall-time invariance stays in `MemoryTrackerTrackingControllerTest`. |
| **Last answered spelling correct** | `browse…` “View last answered…”; `recall_quiz_spelling_question` already opens last answered | Keep the spelling-quiz scenario. |
| Property **skip / return / assimilate skipped** | `property_memory_tracker` (4 scenarios) vs note-level `assimilation_walkthrough.feature` | Those are assimilation, not recall. Drop from recall E2E; unit tests already cover skip/return/assimilate. |
| Property **due count** vs answering | `property_memory_tracker` “becomes due” vs “Answering a property recall…” | Fold due-ness into the answering scenario. |

### Excessive for business-purpose E2E (replace with unit tests; already covered unless noted)

ADR 0003: source code and tests own FSRS mechanics and numeric outcomes. E2E should not re-pin Stability 5 / 21 / 55 / 18 or Difficulty 6.4133.

| Scenario(s) | Why not E2E | Existing coverage |
|------------|--------------|-------------------|
| `recall_timing.feature` (both; `cy.wait(2s)`) | Presentation of away/detour fields; fixed wait | `useThinkingTimeTracker.interruptions.spec.ts`, `QuestionDisplay.thinking.spec.ts`, `RecallPromptAnswerControllerTest` persist, `MemoryTrackerPageView.spec.ts` display |
| Browse “viewing previous answer does not count thinking time” (5s linger) | Same as frontend thinking-time pause | `RecallPage.viewHistoryThinkingTime.spec.ts` |
| Browse “same half-day keeps unanswered prompt” | KeepAlive queue, not a distinct business outcome | `RecallPage.activation.spec.ts` |
| Spaced-repetition **FSRS number / RecallLog / N/A last recall** (7 scenarios) | Algorithm + tracker card display | `MemoryTrackerCorrectRecallSchedulingTest`, `MemoryTrackerIncorrectRecallSchedulingTest` (`FIRST_AGAIN_*` = 5h / 6.4133), `MemoryTrackerSameHourRecallSchedulingTest`, `MemoryTrackerTrackingControllerTest`, `MemoryTrackerPageView*.spec.ts` |
| Accidental-match **ambiguous** and **understanding-when-no-spelling** | Schedule selection | `RecallPromptAccidentalMatchConfusionAdjustmentTests` |
| Accidental-match **offers** (wiki, relationship, overlapped, folder-qualified disable, reopen) | Resolve UI variants | `AnsweredSpellingQuestionAccidentalMatch.spec.ts`, `AnsweredSpellingQuestionAddAsOverlapped.spec.ts`, `MatchedNoteWikiLinkOrRelationshipOffer.spec.ts` |
| Daily probe **tap vs key** | Input modality | `DailyProbe.spec.ts` tap vs F/J |
| Daily probe **off → ordinary recall** | Gate only | `RecallPage.dailyProbe.spec.ts` |
| Daily probe **stats trend / window / hide when off** | Stats presentation | `UserRecallStatsControllerTest`, `RecallStatsSettingsTab.dailyProbe.spec.ts`; **add** empty-series hides trend if missing |
| Daily probe **next local day** | Day boundary | `DailyProbeControllerTest` timezone today |
| Daily probe **leave mid-run** | No save on abort | `DailyProbe.spec.ts` KeepAlive detour |
| Frequent failure warning | Threshold + alert copy | `MemoryTrackerThresholdControllerTest`, `RecallPage.threshold.spec.ts` |
| AI **focus-context POST bodies** | Not user-visible | `FocusContextRetrieval*`, `QuestionGenerationRequestBuilderTests` |
| Spelling stem **wikilink without brackets** | Stem formatting | `prepareQuestionStemMarkdown.spec.ts`, `RecallPromptTest` |
| Property OpenAI **property focus** POST | Prompt internals | `QuestionGenerationRequestBuilderTests.shouldIncludePropertyFocus…` |
| Property panel assimilate / skip / delete tracker / note-level link | Assimilation panel & editor | `RichMarkdownEditor.propertyAssimilation.spec.ts`, `AssimilationPanel.spec.ts`, `NoteUnderQuestion.spec.ts`, `usePropertyMemoryTrackerGuard.spec.ts` |
| `recall_stats.feature` `@wip` pace tile | Known E2E SDK race; not a product gap | `RecallStatsServicePaceAggregationTest` |

### Keep as recall E2E (business document)

| Feature file | Scenario to keep |
|--------------|------------------|
| `accidental_match_scheduling.feature` | Unique matched **spelling** tracker is brought forward without recall credit |
| `daily_probe.feature` | Opted-in learner completes the probe and continues into recall — extend with same-day remount skip |
| `spaced_repetition.feature` | Assimilation/recall **counts**; strictly follow the **9-day schedule**; **recall more** |
| `property_memory_tracker.feature` | Answering a property question updates **only** the property tracker; following the note from a property answer opens that property |
| `recall_quiz_ai_question.feature` | AI generated question — incorrect answer |
| `recall_quiz_spelling_question.feature` | Spelling quiz accepts a correct answer |
| `overlap_try_again.feature` | One path: overlap try-again, then distinguishing alias credits |
| `browse_answer_and_notes_while_recalling.feature` | Already-tagged “Browse notes while recalling and come back”; **one** remove-then-revive |
| `assimilation/` (moved) | “Different assimilation pages for different notes” (image + relationship) — not recall |

Target remaining in `recall/`: **~12 scenarios** (from 59). `recall_timing.feature`, `frequent_failure_warning.feature`, `accidental_match_reveal.feature`, `recall_stats.feature` go away.

---

### 1. Drop recall timing E2E and thinking-time linger
Type: Structure
Status: done

**Tests:**
- `recall_timing.feature` — both scenarios (~9.4s + ~6.5s)
- `browse_answer_and_notes_while_recalling.feature` — “Viewing a previous answer does not count…” (~11.1s)

**Goals:**
- Delete `recall_timing.feature` and its exclusive steps if unused (`I switch away from the tab`, `I take a detour into the notebook for {int} seconds`).
- Delete the browse thinking-time linger scenario (`viewLastAnsweredQuestionFor` / 5s linger).
- Remove `cy.wait(ms)` / linger `setTimeout` from remaining recall page objects if nothing else uses them.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/recall/browse_answer_and_notes_while_recalling.feature
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/pages/RecallPage.viewHistoryThinkingTime.spec.ts tests/composables/useThinkingTimeTracker.interruptions.spec.ts tests/components/recall/QuestionDisplay.thinking.spec.ts tests/pages/MemoryTrackerPageView.spec.ts
```

---

### 2. Drop FSRS-number Memory Tracker E2E from spaced repetition
Type: Structure
Status: done

**Tests:**
- `spaced_repetition.feature` — Last Recall N/A; GOOD RecallLog; first Again Stability 5; on-time Good Stability 21; incorrect just-review Stability 15; same-hour Good 55; same-hour Again 18 (~top 10% #3 and #5)

**Goals:**
- Keep only: “The assimilation and recall page” (counts), “Strictly follow the schedule”, “Strictly follow the schedule but want to recall more”.
- Move remove/revive out of this file in slice 6 (or drop Last Recall Time assertions here if slice 6 has not run yet — do not leave two remove/revive journeys).
- Do not add new FSRS unit tests; numbers already match `MemoryTracker*SchedulingTest`.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/recall/spaced_repetition.feature
```

---

### 3. Collapse accidental match to one scheduling journey
Type: Structure
Status: done

**Tests:**
- `accidental_match_reveal.feature` — all six (~38s file)
- `accidental_match_scheduling.feature` — ambiguous; understanding-when-spelling-absent (~top 10% #6)

**Goals:**
- Keep **one** E2E: unique matched spelling tracker brought forward without recall credit (includes the reveal as the visible trigger).
- Delete `accidental_match_reveal.feature`.
- Delete the two extra scheduling scenarios.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/recall/accidental_match_scheduling.feature
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/recall/AnsweredSpellingQuestionAccidentalMatch.spec.ts tests/components/recall/AnsweredSpellingQuestionAddAsOverlapped.spec.ts tests/components/recall/MatchedNoteWikiLinkOrRelationshipOffer.spec.ts
```

---

### 4. Collapse Daily probe to one complete-and-continue journey
Type: Structure
Status: done

**Tests:**
- `daily_probe.feature` — 9 scenarios (~54s)

**Goals:**
- Keep one scenario: opted-in complete (keyboard) → Saved → Continue → ordinary recall; add same-day remount skip as extra When/Then on that path.
- Delete tap-complete, leave-mid-run, probe-off, next-day, and all Recall Stats trend scenarios.
- If missing: frontend unit test that an empty `dailyProbe` series hides the trend (API already empty when probe is off: `UserRecallStatsControllerTest.dailyProbeSeriesIsEmptyWhenDailyProbeIsOff`).

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/recall/daily_probe.feature
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/pages/RecallPage.dailyProbe.spec.ts tests/components/recall/DailyProbe.spec.ts tests/pages/settings/RecallStatsSettingsTab.dailyProbe.spec.ts
```

---

### 5. Collapse property memory tracker to property-recall journeys
Type: Structure
Status: done

**Tests:**
- `property_memory_tracker.feature` — 14 scenarios (~60s)

**Goals:**
- Keep: answering a property question updates only the property tracker (include “becomes due” as a Given/Then on that path, not a second visit-counts-only scenario).
- Keep: following the note from a property recall answer opens that property.
- Delete assimilation-queue, skip, return-to-sequence, skip-then-assimilate, panel remove, note-level assimilate still available, delete property, assimilate from property panel, tracker note-link, note-level note-link, OpenAI property-focus POST.
- Add only if absent: controller test that creating a **property** sequence skip does not create a memory tracker (`AssimilationSequenceSkipControllerTest` today covers note-level create and property **delete**).

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/recall/property_memory_tracker.feature
CURSOR_DEV=true nix develop -c pnpm backend:test_only
```

(Backend `test_only` once if a skip-controller test is added; otherwise focused frontend property tests + the remaining E2E spec.)

---

### 6. Merge remaining quiz, pages, browse, overlap, and dead stats
Type: Structure
Status: planned

**Tests:**
- `frequent_failure_warning.feature` (~8.1s)
- `recall_quiz_ai_question.feature` — focus-context POST scenario
- `recall_quiz_spelling_question.feature` — wikilink stem
- `overlap_try_again.feature` — two scenarios
- `recall_pages.feature` — both
- `browse_answer_and_notes_while_recalling.feature` — remaining extras
- `recall_stats.feature` — `@wip` pace

**Goals:**
- Delete `frequent_failure_warning.feature` and `recall_stats.feature`.
- Keep AI incorrect-answer; delete focus-context POST assertion scenario.
- Keep spelling correct-answer; delete wikilink-stem scenario (`prepareQuestionStemMarkdown.spec.ts` already strips `[[LinkTarget]]`).
- Merge overlap into **one** scenario: overlap try-again, then distinguishing alias is credited. Partner schedule-unchanged stays in `RecallPromptOverlapTryAgainTests`.
- Move “Different assimilation pages for different notes” to `e2e_test/features/assimilation/` (capability: assimilation page types). Delete `recall_pages.feature` after counts live only on `spaced_repetition`.
- Browse: keep skip-tagged resume-from-note; keep **one** remove-then-revive; delete “view last answered when correct” and “same half-day unanswered prompt”.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/recall/recall_quiz_ai_question.feature,e2e_test/features/recall/recall_quiz_spelling_question.feature,e2e_test/features/recall/overlap_try_again.feature,e2e_test/features/recall/browse_answer_and_notes_while_recalling.feature,e2e_test/features/recall/spaced_repetition.feature,e2e_test/features/assimilation
```

E2E groups: 3+ consecutive green runs on touched specs before closing.

---

### 7. Re-profile and close
Type: Structure
Status: planned

Same command as baseline (recall spec glob + same `--expose tags`).

| Metric | Before | After |
|--------|--------|-------|
| Test count | 59 | ~12 (plus moved assimilation page-types scenario) |
| Suite wall | ~5:39 | (fill) |
| Top 10% total time | ~55s | (fill) |

**Candidates proposed this run:** (none expected; 9-day schedule is ~5.5s)

**Commits:** (fill during execute-plan)

**Planning cleanup:** prune spent slice detail after the pass; keep this PLAN only while in progress; never commit profile JSON. Leave `.planning/test-optimization-blacklist.md` untouched unless a genuine Candidate appears.
