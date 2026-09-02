# Assimilation controls redesign

**Status:** planned
**Goal:** Simplify the note-level Assimilation settings panel down to compact, mobile-friendly per-mode controls; move the two things that don't belong there (refine-note, progress numbers) to where they're actually used (toolbar, nav); and make the note-property panel share the same control instead of a parallel implementation.

## Design decisions

- **No backend changes.** `AssimilationCountDTO.totalUnassimilatedCount` already exists and is already returned by both `GET /api/user/menu-data` and `GET /api/assimilation/next`. `NoteController.getNoteInfo` already returns per-tracker `nextRecallAt`/`type`/`propertyKey`. Every slice below is frontend-only.
- **Nav badge format:** replace the single `dueCount` badge on the `assimilate` nav item with a combined `"{due}/{total}"` string (e.g. `5/128`). Each side is independently abbreviated above 3 digits using a shared `abbreviateCount` helper (1000 → `1k`, 12400 → `12.4k`), so a large total never breaks the badge's compact shape. The existing `AssimilationMenuProgress` thin bar is untouched — it already shows today's progress and doesn't compete with the new badge. Tooltip/`title` on the badge spells out "N due today, M total unassimilated" for anyone who wants the unabbreviated numbers.
- **Shared control, not two implementations:** `AssimilationModes.vue` is the new compact per-mode control, eventually replacing `AssimilationButtons.vue` for both callers. It takes `allowedModes: MemoryTrackerType[]` plus the note/property scope it already receives, and renders one compact row per allowed mode:
  - no tracker for that mode → an "Assimilate" action (dropdown collapses away — each mode gets its own direct trigger now that the row already identifies the mode, so the old "Assimilate as commissioned / Remember spelling" dropdown-under-one-button structure goes away).
  - tracker exists for that mode → the row becomes a link-styled status ("In recall · next 12 Sep") that navigates to that tracker's detail page; recall count shown only as a `title` tooltip on the status (native attribute — no `Tooltip.vue` exists in the codebase and none is needed for one number). Stability is never shown here.
  - Notes get `[UNDERSTANDING, SPELLING, COMMISSIONED]`; note properties get `[UNDERSTANDING]` only — matches today's actual capability split.
- **Scope clarification (confirmed with the user):** the constraint "Keep the panel focused on starting or inspecting recall modes; tracker details and removal belong on the tracker page" covers *all* tracker-state actions, not just "Remove from recall" literally. So **Revive** (re-enabling a removed tracker) and **Remove from recall** both move to the tracker detail page only, for both note-level and property-level. The one exception is **Skip / Return to sequence**: that pair is about the MCQ assimilation *sequence* before any tracker exists (opting a note out of ever appearing in the sequence), not about an existing tracker, so it stays as a small secondary affordance on the MCQ row only, next to its "Assimilate" action.
- **Mode labels:** "MCQ", "Spelling", "Comprehension" (dropping the internal "Commissioned" enum name from user-facing text). Confirm against any existing i18n/label table at execute time; `trackerTypeLabel` in `NoteInfoMemoryTracker.vue` is the closest current precedent and is being deleted in slice 9, so this is a fresh label, not a rename.
- **Staged migration, not a one-shot rebuild (revised after a first attempt was reverted):** a first attempt bundled building `AssimilationModes.vue`, cutting `AssimilationSettings.vue` over, deleting the old Memory Trackers table, and fixing the resulting E2E fallout into one delegated slice. It took multiple hours across several sub-agent runs, so it was reverted and re-planned into six smaller slices (4-9 below): build the new component fully but **unwired** first (zero integration risk), cut over the note-level trigger/skip controls while the old table stays mounted (small, contained blast radius), remove the old table as its own slice paired with fixing the E2E fallout it causes, then migrate the property panel, then delete dead code last. `AssimilationButtons.vue` and the old table are deliberately left in place across several slices — legitimate interim duplication per `planning.mdc`'s "Interim behavior" allowance, removed once the slices that replace them have shipped.
- **Removing the old Memory Trackers table breaks unrelated E2E — already diagnosed, resolution pre-approved:** the table renders both note-level and property-level tracker rows (`NoteInfoComponent.vue`/`NoteInfoMemoryTracker.vue`), and 4 E2E features outside this plan's stated scope read tracker existence/recall-count from it: `relationships/relationship_edit_and_remove.feature`, `recall/property_memory_tracker.feature`, `assimilation/assimilate_with_remembering_spelling.feature`, `learning_session/commissioned_learning_session.feature`. Developer-approved fix (asked and answered during the reverted attempt, do not re-ask): redirect existence/absence checks to the new row markup, and recall-count checks to the tracker detail page — matching this plan's own philosophy that tracker details belong on the tracker page. This is slice 7's scope below, with the exact recipe already spelled out there.
- **Lesson for `post-change-refactor`'s dead-code check:** the reverted attempt's slice 3 already shipped a deletion (`AssimilationProgressSummary.vue`) whose "zero remaining callers" check only grepped `frontend/src`, missing an E2E step definition that read its `data-test` attribute directly — this broke `assimilation_walkthrough.feature` on `main` (fixed by slice 4 below). Any future deletion of a component with a `data-test`/`data-testid` attribute must also grep `e2e_test/` before concluding it's dead.

## Slices

### 1. Assimilate nav item shows due and total as one combined, abbreviated badge

- **Type:** Behavior
- **Status:** done
- **Pre-condition:** user has both a due-today count and a total-unassimilated backlog (already computed server-side).
- **Trigger:** nav renders.
- **Post-condition:** the `assimilate` nav item shows one badge `"{due}/{total}"` (each side abbreviated above 3 digits) instead of the bare due-count badge; no second competing badge appears.
- **Touches:** `frontend/src/composables/useNavigationItems.ts` (badge value), a new small `abbreviateCount`/`formatAssimilationBadge` helper (co-located with `useAssimilationCount.ts`), `frontend/src/components/navigation/NavigationItem.vue` only if the badge markup needs more width for the combined string.
- **Tests:** unit test for the abbreviation/format helper (small-test style: 0, 3-digit boundary, 1000, large value); a component/composable test asserting the nav badge string for a given due/total pair.
- **Learning:** the combined badge stays hidden when both due and total are 0 (matches prior single-badge behavior). `NavigationItem.vue`'s badge prop widened to `number | string` plus a new `badgeTitle` prop for the "N due today, M total unassimilated" tooltip; post-change-refactor extracted a shared `NavigationItemProps` type (`frontend/src/components/navigation/navigationItem.ts`) so badge/badgeTitle no longer need four parallel edits across `NavigationItem.vue`, `HorizontalMenu.vue`, `VerticalMenu.vue`, `HomeWelcomeSection.vue`.

### 2. "Refine note" moves from the assimilation panel into the note toolbar

- **Type:** Behavior
- **Status:** done
- **Pre-condition:** user is viewing a note.
- **Trigger:** user opens the note's toolbar / more-options.
- **Post-condition:** "Refine note" appears as a toolbar action next to "Questions for the note" (same overflow/collapse behavior as other toolbar actions) and opening it still opens `RefineNoteModal`; it no longer appears anywhere inside the assimilation panel.
- **Touches:** `frontend/src/components/notes/widgets/noteMoreOptionsTitles.ts` (add a `refine` entry), `frontend/src/components/notes/widgets/NoteMoreOptionsActions.vue`, `frontend/src/components/notes/widgets/NoteToolbarMoreOptions.vue`, `frontend/src/composables/noteToolbarOverflow.ts` (`NOTE_TOOLBAR_MORE_OPTIONS_ORDER`), `frontend/src/components/recall/AssimilationSettings.vue` (delete the button + its trigger wiring — `RefineNoteModal` mount moves with it).
- **Tests:** component test that the toolbar/more-options renders a refine action that opens the modal; assert `AssimilationSettings` no longer renders it.
- **Learning:** the `refinementContentUpdated → reloadNeeded` listener in `AssimilationPanel.vue` was removed as dangling — it only refreshed assimilation-panel recall info, and note content already updates via the shared store independent of that event. Post-change-refactor extracted a shared `hasNoteContent()` util (`frontend/src/utils/hasNoteContent.ts`) to collapse three copies of the same "does this note have content" check (`NoteMoreOptionsActions.vue`, `AssimilationSettings.vue`, `AnsweredQuestionComponent.vue`).

### 3. Assimilation panel drops its title and the three-number progress summary

- **Type:** Behavior
- **Status:** done
- **Pre-condition:** user opens the assimilation panel from the note toolbar.
- **Trigger:** panel renders.
- **Post-condition:** no "Assimilation settings" heading and no assimilated/planned/total summary line; whatever the panel now contains (still the old table at this point) is otherwise unchanged.
- **Touches:** `frontend/src/components/recall/AssimilationSettings.vue` (remove the `<h2>` and `<AssimilationProgressSummary>` usage).
- **Tests:** component test asserting the heading/summary are absent from the rendered panel.
- **Note:** deliberately split from the mode-control rebuild (now slices 5-7) — this is a pure deletion with zero risk to that bigger change, so it can land and be reverted independently.
- **Learning:** post-change-refactor found `AssimilationProgressSummary.vue` had zero remaining production callers after this slice's edit, so it (and its test, and the now-orphaned `assimilationProgressFromCounts`/`AssimilationProgressCounts` in `useAssimilationCount.ts`) were deleted here rather than left dead until slice 6 — slice 6's pre-condition/touches below is updated to match.

### 4. Fix pre-existing E2E regression: assimilation-progress assertions read a deleted element

- **Type:** Behavior
- **Status:** done
- **Pre-condition:** `assimilation_walkthrough.feature` currently fails 4 of 9 scenarios on `main` — slice 3 deleted `AssimilationProgressSummary.vue` (`[data-test="assimilation-progress-summary"]`) but nothing updated the E2E step that reads it.
- **Trigger:** the 5 occurrences of `Then I should see assimilation progress "X/Y/Z"` across 4 scenarios in `e2e_test/features/assimilation/assimilation_walkthrough.feature` (lines 21, 27, 30, 94, 105).
- **Post-condition:** those assertions read the nav badge (`due/total`, already shipped in slice 1) instead of the removed element; all 9 scenarios in the feature pass.
- **Touches:** `e2e_test/features/assimilation/assimilation_walkthrough.feature`, `e2e_test/step_definitions/assimilation.ts` (`I should see assimilation progress` step), `e2e_test/start/pageObjects/assimilationPage/assimilationFlow.ts` (delete `expectAssimilationProgressSummary`; add a nav-badge reader following the existing `expectCount`/`expectAssimilationMenuProgress` convention in `assimilationMenu.ts`, e.g. `expectAssimilationNavBadge(dueOverTotal)` asserting `.due-count` text on the assimilate nav item).
- **Tests:** `cypress run --spec e2e_test/features/assimilation/assimilation_walkthrough.feature` green (9/9). This is a standalone regression fix, not new product scope — commit it separately from the redesign slices.
- **Known values (already derived once — re-derive only if fixture setup changes):** with `dailyLimit=2` and 5 total notes, old triple `0/2/5` → new badge `2/5`; old triple `1/2/5` → new badge `1/4` (`totalUnassimilatedCount` decrements as notes are assimilated, unlike the old triple's static third number). Backend logic for reference: `AssimilationCounter.getDueCount() = min(dailyLimit - assimilatedToday, totalUnassimilated)`, `getTotalUnassimilated() = subscribedUnitCount + ownedUnitCount`.
- **Learning:** this slice's fix had already landed on `main` in commit `4dc4a72c77` before execution reached this slice (folded in alongside an unrelated toolbar-overflow refactor). No new code change was needed — verified the commit's content matches this slice's spec exactly and confirmed all 10 scenarios in `assimilation_walkthrough.feature` pass (feature has 10 scenarios, not 9 as originally estimated).

### 5. Build `AssimilationModes.vue` — full component, not yet wired into any page

- **Type:** Structure
- **Status:** planned
- **Pre-condition:** slices 1-4 shipped; `AssimilationButtons.vue` and its current callers (`AssimilationSettings.vue`, `RichFrontmatterPropertyPanel.vue`) are untouched.
- **Trigger:** none externally — new file only, not referenced by any production page yet.
- **Post-condition:** `frontend/src/components/recall/AssimilationModes.vue` exists with full component-test coverage for all four row states, but nothing renders it — existing test suite and app behavior are unchanged (verified: existing suite stays green).
- **Touches:** new `AssimilationModes.vue` (props: `allowedModes: MemoryTrackerType[]`, `trackers?: MemoryTracker[]`, `propertyKey?`, `disabled?`, `skippedFromAssimilationSequence?`, `size?`; emits: `assimilate`, `skip`, `returnToSequence`); new `noteLevelTrackerOfType(trackers, type, propertyKey?)` helper in `assimilationMemoryTrackers.ts` returning the tracker object itself (not just a boolean — the row needs `nextRecallAt`/`recallCount`/`id`); new `frontend/tests/components/recall/AssimilationModes.spec.ts`.
- **Tests:** component tests covering: no tracker for a mode → Assimilate button (per mode, direct trigger, no dropdown); active tracker for a mode → whole-row link "In recall · next {date}" to `memoryTrackerShow`, recall count in `title` only, no stability shown; MCQ (COMMISSIONED) row → Skip/Return-to-sequence secondary affordance (falls back to the Comprehension/UNDERSTANDING row when COMMISSIONED isn't in `allowedModes`, i.e. property-level reuse in slice 8); mode-label mapping (COMMISSIONED→"MCQ", SPELLING→"Spelling", UNDERSTANDING→"Comprehension").
- **Design resolution (settled, do not re-open):** "tracker exists for that mode" means an *active* note-level tracker of that type (`removedFromTracking !== true`) — a previously-removed tracker is treated the same as "no tracker," matching the existing `activeUnderstandingTrackers`/`showRemoveFromRecall` precedent. No "Remove from recall"/"Revive" anywhere in this component — those stay tracker-page-only per the scope decision above.
- **Sizing note:** if this still runs long, split by row state (assimilate-only first, then linked-status, then MCQ skip/return-to-sequence) — same technique as before, now scoped to a single unwired file so any split point is trivially safe to stop at.

### 6. Note-level panel cuts over its trigger/skip controls to `AssimilationModes`

- **Type:** Behavior
- **Status:** planned
- **Pre-condition:** slice 5 shipped; `AssimilationModes.vue` exists, fully tested, unused.
- **Trigger:** user opens the assimilation panel for a note.
- **Post-condition:** the panel's trigger row (previously `AssimilationButtons.vue`: combined Assimilate button + commissioned/spelling dropdown + skip/revive/remove-from-recall) is replaced by `AssimilationModes`'s per-mode rows (`allowed-modes="[COMMISSIONED, SPELLING, UNDERSTANDING]"`); the old Memory Trackers table (`NoteInfoComponent`) still renders below, unchanged, for tracker inspection. Note-level "Remove from recall"/"Revive" are gone from the panel (tracker-page-only from here on); MCQ Skip/Return-to-sequence still works, now via the new row.
- **Touches:** `AssimilationSettings.vue` (mount `AssimilationModes` in place of `AssimilationButtons`, keep `NoteInfoComponent` mounted as-is), its own tests (`AssimilationPanel.spec.ts`, `.commissioned.spec.ts`, `.spelling.spec.ts`, `.loadingModal.spec.ts`, both support files).
- **Tests:** update the above component tests for the new row markup; update `assimilation_walkthrough.feature`'s assimilate/skip/return-to-sequence scenarios and remove its now-impossible "Remove from recall on assimilation settings" scenario. Do **not** touch the 4 unrelated feature files here — they still read the still-present old table, untouched by this slice.
- **Known pitfall:** `e2e_test/start/pageObjects/assimilationPage/shared.ts`'s `noteLevelControl()` helper does `cy.get(selector).filter(fn)` — this throws "expected to find element, but never found it" instead of passing a `.should('not.exist')` check once the raw selector matches zero elements anywhere in the DOM (Cypress only special-cases the direct `cy.get(sel).should('not.exist')` form, not `.get().filter().should('not.exist')`). Any "assimilate should be disabled" style assertion needs `cy.document().then(doc => expect(noteLevelControlElements(doc, selector)).to.have.length(0))` instead (mirrors `expectOtherNoteLevelSecondaryActionsAbsent`'s existing pattern) — `noteLevelControlElements` needs `export`ing from `shared.ts` for this.

### 7. Remove the old Memory Trackers table; redirect the E2E it was carrying

- **Type:** Behavior
- **Status:** planned
- **Pre-condition:** slice 6 shipped — assimilate/status/skip are fully covered by `AssimilationModes` rows.
- **Trigger:** panel renders.
- **Post-condition:** `AssimilationSettings.vue` no longer renders `NoteInfoComponent`/`NoteInfoMemoryTracker` — only the compact per-mode rows show. The 4 E2E features that read tracker info from that table (note-level and property-level *existence*/*recall-count* checks only — not the property panel's own Remove-from-recall button, which is slice 8's concern) are redirected to equivalent new sources and stay green.
- **Touches:** `AssimilationSettings.vue` (drop `NoteInfoComponent` usage); E2E: `e2e_test/start/pageObjects/assimilationPage/shared.ts` (replace old-table row-label helpers with row/status selectors scoped to `[data-test="assimilation-row-<mode>"]`), `e2e_test/start/pageObjects/assimilationPage/propertyMemoryTrackerExpectations.ts` (note-level existence/recall-count checks), `e2e_test/start/pageObjects/noteRichPropertyAssimilationMethods.ts` (property-row-scoped equivalents, reusing the existing `withPropertyPanel` scoping convention), `e2e_test/step_definitions/assimilation_memory_tracker.ts`, `e2e_test/start/pageObjects/noteMoreOptionsForm.ts` (`openAssimilationSettings()`'s "already open" signal must key off `[data-testid="assimilation-settings"]`, not `[data-test="assimilate"]` — that button can now be legitimately absent).
- **Known recipe (already solved once during the reverted attempt — redo directly, don't rediscover):** existence/absence of a tracker for a mode → check `[data-test="assimilation-row-<mode>"]` for a nested `[data-test="assimilation-status"]` (exists) vs. an Assimilate button (doesn't). Recall-count assertions → click the row's status link to the tracker page, assert via `assumeMemoryTrackerPage().expectRecallCount(count)`, then `cy.go('back')` if the scenario has more steps after.
- **Tests:** `cypress run --spec` each of: `e2e_test/features/relationships/relationship_edit_and_remove.feature`, `e2e_test/features/recall/property_memory_tracker.feature` (its non-remove scenarios only — see slice 8), `e2e_test/features/assimilation/assimilate_with_remembering_spelling.feature`, `e2e_test/features/learning_session/commissioned_learning_session.feature`; also re-run `assimilation_walkthrough.feature` as a regression check (should already be green from slice 6).
- **Sizing note:** the one slice in this breakdown that legitimately touches many files at once, because the table removal and its readers must move together to stay CI-green — but the recipe above is already known from the reverted attempt, so treat any further exploration/rediscovery time as the signal to stop and re-read this note rather than re-deriving it from scratch.

### 8. Note-property panel reuses `AssimilationModes` instead of its own wiring

- **Type:** Behavior
- **Status:** planned
- **Pre-condition:** user is editing a note's frontmatter property that supports assimilation; slice 7 shipped.
- **Trigger:** the property row renders (and user can start assimilation, or follow an existing tracker's status).
- **Post-condition:** `RichFrontmatterPropertyPanel.vue` renders `<AssimilationModes :allowed-modes="[UNDERSTANDING]" .../>` instead of `<AssimilationButtons size="sm">`; "Remove from recall"/"Revive" are gone from this panel too (tracker-page-only); assimilate/status behavior matches slices 6-7's rows.
- **Touches:** `frontend/src/components/form/RichFrontmatterPropertyPanel.vue` and its own tests.
- **Tests:** component test for the property row's two states (assimilate / linked status), reusing the same assertions as slice 5/6's tests rather than duplicating them. E2E: `property_memory_tracker.feature`'s "Remove from recall on assimilation settings for a property" scenario — its assertion source (the property panel's own `AssimilationButtons` remove/revive button) only breaks here, not in slice 7. Rewrite it to remove-from-recall via the tracker page instead, reusing existing step definitions (`I open the property memory tracker for`, `I remove the memory tracker from recall`, `the memory tracker should be skipped`) — same pattern already used in `spaced_repetition.feature`; no new step definitions needed.

### 9. Delete the dead memory-tracker-table code

- **Type:** Structure
- **Status:** planned
- **Pre-condition:** slices 1-8 shipped; nothing renders `NoteInfoComponent.vue`, `NoteInfoMemoryTracker.vue`, or `AssimilationButtons.vue`.
- **Trigger:** none (no external behavior change — verified by existing test suite staying green).
- **Post-condition:** those files and their now-unreachable tests are deleted; `assimilationMemoryTrackers.ts` predicates trimmed to whatever `AssimilationModes.vue` actually still uses (`showRemoveFromRecall`, `assimilateDisabledForProperty` likely drop out entirely); any e2e helpers left unused after slice 7 (old table row-label helpers, if not already removed there) are removed too.
- **Touches:** deletions only, plus a grep pass for stale imports/routes.
- **Tests:** run the full frontend unit suite for the touched directories to confirm nothing else referenced the deleted files.

## Not planned (considered and rejected)

- **New `Tooltip.vue` / `Badge.vue` primitives** — the recall-count-as-tooltip need is a single native `title` attribute; not enough repetition yet to justify a shared component.
- **Backend endpoint or DTO changes** — every number and timestamp the new UI needs is already exposed.
- **Touching `MemoryTrackerInformation.vue` / `MemoryTrackerPage.vue`** — out of scope; it already has the removal/revive controls this plan is consolidating onto that page, no rework needed there.
