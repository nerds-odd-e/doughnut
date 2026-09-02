# Assimilation controls redesign

**Status:** planned
**Goal:** Simplify the note-level Assimilation settings panel down to compact, mobile-friendly per-mode controls; move the two things that don't belong there (refine-note, progress numbers) to where they're actually used (toolbar, nav); and make the note-property panel share the same control instead of a parallel implementation.

## Design decisions

- **No backend changes.** `AssimilationCountDTO.totalUnassimilatedCount` already exists and is already returned by both `GET /api/user/menu-data` and `GET /api/assimilation/next`. `NoteController.getNoteInfo` already returns per-tracker `nextRecallAt`/`type`/`propertyKey`. Every slice below is frontend-only.
- **Nav badge format:** replace the single `dueCount` badge on the `assimilate` nav item with a combined `"{due}/{total}"` string (e.g. `5/128`). Each side is independently abbreviated above 3 digits using a shared `abbreviateCount` helper (1000 → `1k`, 12400 → `12.4k`), so a large total never breaks the badge's compact shape. The existing `AssimilationMenuProgress` thin bar is untouched — it already shows today's progress and doesn't compete with the new badge. Tooltip/`title` on the badge spells out "N due today, M total unassimilated" for anyone who wants the unabbreviated numbers.
- **Shared control, not two implementations:** refactor `AssimilationButtons.vue` in place into the new compact per-mode control (rename to `AssimilationModes.vue` since it's no longer just buttons) rather than adding a second component. It takes `allowedModes: MemoryTrackerType[]` plus the note/property scope it already receives, and renders one compact row per allowed mode:
  - no tracker for that mode → an "Assimilate" action (dropdown collapses away — each mode gets its own direct trigger now that the row already identifies the mode, so the old "Assimilate as commissioned / Remember spelling" dropdown-under-one-button structure goes away).
  - tracker exists for that mode → the row becomes a link-styled status ("In recall · next 12 Sep") that navigates to that tracker's detail page; recall count shown only as a `title` tooltip on the status (native attribute — no `Tooltip.vue` exists in the codebase and none is needed for one number). Stability is never shown here.
  - Notes get `[UNDERSTANDING, SPELLING, COMMISSIONED]`; note properties get `[UNDERSTANDING]` only — matches today's actual capability split.
- **Scope clarification (confirmed with the user):** the constraint "Keep the panel focused on starting or inspecting recall modes; tracker details and removal belong on the tracker page" covers *all* tracker-state actions, not just "Remove from recall" literally. So **Revive** (re-enabling a removed tracker) and **Remove from recall** both move to the tracker detail page only, for both note-level and property-level. The one exception is **Skip / Return to sequence**: that pair is about the MCQ assimilation *sequence* before any tracker exists (opting a note out of ever appearing in the sequence), not about an existing tracker, so it stays as a small secondary affordance on the MCQ row only, next to its "Assimilate" action.
- **Mode labels:** "MCQ", "Spelling", "Comprehension" (dropping the internal "Commissioned" enum name from user-facing text). Confirm against any existing i18n/label table at execute time; `trackerTypeLabel` in `NoteInfoMemoryTracker.vue` is the closest current precedent and is being deleted in slice 6, so this is a fresh label, not a rename.

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
- **Status:** planned
- **Pre-condition:** user opens the assimilation panel from the note toolbar.
- **Trigger:** panel renders.
- **Post-condition:** no "Assimilation settings" heading and no assimilated/planned/total summary line; whatever the panel now contains (still the old table at this point) is otherwise unchanged.
- **Touches:** `frontend/src/components/recall/AssimilationSettings.vue` (remove the `<h2>` and `<AssimilationProgressSummary>` usage).
- **Tests:** component test asserting the heading/summary are absent from the rendered panel.
- **Note:** deliberately split from slice 4 — this is a pure deletion with zero risk to the mode-control rebuild, so it can land and be reverted independently of the bigger change.

### 4. Note-level panel replaces the Memory Trackers table with compact per-mode controls

- **Type:** Behavior
- **Status:** planned
- **Pre-condition:** user opens the assimilation panel for a note that has zero, some, or all of its three note-level trackers.
- **Trigger:** panel renders (and user can act: start a mode's assimilation, or follow a mode's status to its tracker page).
- **Post-condition:** the panel shows one compact row per note-level mode (MCQ, Spelling, Comprehension): "Assimilate" when no tracker exists for that mode, or a linked "In recall · next {date}" status (recall count in `title` tooltip, no stability shown) when one does, whole-row-clickable to that tracker's page. MCQ row additionally carries the Skip/Return-to-sequence affordance per the scope note above. No "Remove from recall" anywhere in the panel.
- **Touches:** `AssimilationButtons.vue` → `AssimilationModes.vue` (the rebuild), `AssimilationSettings.vue` (mount the new component instead of `NoteInfoComponent`), new small props contract (`allowedModes`, tracker lookup by type). `NoteInfoComponent.vue`/`NoteInfoMemoryTracker.vue` stop being referenced from here (left in place until slice 6 confirms nothing else uses them).
- **Tests:** component tests for `AssimilationModes.vue` covering all four row states (no tracker/assimilate, active tracker/status+link, MCQ skip, MCQ return-to-sequence) in the "small test" style; existing assimilate/skip/revive E2E re-pointed at the new row instead of the old table+buttons layout.
- **Sizing note:** likely the biggest slice — if it runs past the ~10 min hard trigger during execution, split state-by-state (assimilate-only row first, then linked-status row, then the MCQ skip/return-to-sequence affordance) rather than pushing through.

### 5. Note-property panel reuses `AssimilationModes` instead of its own wiring

- **Type:** Behavior
- **Status:** planned
- **Pre-condition:** user is editing a note's frontmatter property that supports assimilation.
- **Trigger:** the property row renders (and user can start MCQ assimilation, or follow an existing tracker's status).
- **Post-condition:** `RichFrontmatterPropertyPanel.vue` renders `<AssimilationModes :allowed-modes="[UNDERSTANDING]" .../>` instead of `<AssimilationButtons size="sm">`; "Remove from recall" is gone from this panel too (only reachable from the tracker page); assimilate/status behavior matches slice 4's rows.
- **Touches:** `frontend/src/components/form/RichFrontmatterPropertyPanel.vue`.
- **Tests:** component test for the property row's two states (assimilate / linked status), reusing the same assertions style as slice 4's tests rather than duplicating them.

### 6. Delete the dead memory-tracker-table code

- **Type:** Structure
- **Status:** planned
- **Pre-condition:** slices 1–5 shipped; nothing renders `NoteInfoComponent.vue`, `NoteInfoMemoryTracker.vue`, `AssimilationProgressSummary.vue`, or the old dropdown/remove/revive branches of the pre-rebuild `AssimilationButtons.vue`.
- **Trigger:** none (no external behavior change — verified by existing test suite staying green).
- **Post-condition:** those files and their now-unreachable tests are deleted; `assimilationMemoryTrackers.ts` predicates trimmed to whatever `AssimilationModes.vue` actually still uses (e.g. `showRemoveFromRecall` likely drops out entirely).
- **Touches:** deletions only, plus a grep pass for stale imports/routes.
- **Tests:** run the full frontend unit suite for the touched directories to confirm nothing else referenced the deleted files.

## Not planned (considered and rejected)

- **New `Tooltip.vue` / `Badge.vue` primitives** — the recall-count-as-tooltip need is a single native `title` attribute; not enough repetition yet to justify a shared component.
- **Backend endpoint or DTO changes** — every number and timestamp the new UI needs is already exposed.
- **Touching `MemoryTrackerInformation.vue` / `MemoryTrackerPage.vue`** — out of scope; it already has the removal/revive controls this plan is consolidating onto that page, no rework needed there.
