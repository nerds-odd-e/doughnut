# Assimilation Restructure Plan

Restructure assimilation from two dedicated pages (`/assimilate` queue, `/assimilate/:noteId` single note) into an **inline settings panel on the normal note page**, driven by a per-note view toggle, with menu/button navigation that walks the user through notes to assimilate one at a time.

## Current state (discovered)

- Routes: `/assimilate` (`AssimilationPage` + `AssimilationPageView`, queue) and `/assimilate/:noteId` (`AssimilateSingleNotePage` + `View`). Both render `recall/Assimilation.vue` = `NoteShow` + `AssimilationSettings` dock + spelling popup + assimilate logic.
- Dropdown "Assimilation settings" (`NoteMoreOptionsForm.vue`) **navigates** to `/assimilate/:noteId`.
- Main menu "Assimilate" (`useNavigationItems.ts` + `NavigationItem.vue`) is a **router-link** to `/assimilate`.
- Backend: `GET /api/assimilation/assimilating?timezone=` → `List<NoteRealm>` via `AssimilationService.getNotesToAssimilate()`. **Returns empty as soon as the daily cap is reached** (`remainingDailyCount <= 0` ⇒ `Stream.empty()`).
- `POST /api/assimilation` (`AssimilationRequestDTO{noteId, skipMemoryTracking}`) → `List<MemoryTracker>`.
- Counts: `GET /api/user/menu-data` → `AssimilationCountDTO{dueCount, assimilatedCountOfTheDay, totalUnassimilatedCount}`. `dueCount = remainingDaily>0 ? min(remainingDaily, total) : 0`.
- Frontend count state: module composable `useAssimilationCount.ts`. Toggle/labels: "Keep for recall" (`skipMemoryTracking=false`), "Skip recall" (`true`). There is **no** "Skill recall" string — interpreting the requirement's "Skill recall" as **"Skip recall"**.

## Key design decisions

1. **Daily-cap tension (most important).** The requirement "won't block the user from assimilating more" after finishing the daily plan **conflicts** with the current backend, which returns no notes once the cap is hit. We add a new backend capability `getNextNoteToAssimilate()` that returns the next unassimilated note in priority order (subscriptions first, then owned) **ignoring all daily caps**, or none. Caps only drive counts/toasts, never block.
2. **"Daily goal met" vs "nothing left" are distinct.** Detected from counts together with whether a next note exists:
   - next note exists AND `dueCount == 0` ⇒ daily goal met (but more available) ⇒ "you've completed your daily goal" toast, still navigate.
   - no next note ⇒ "no more to assimilate" toast; **stay on the current note** (no navigation). Confirmed with stakeholder.
3. **New endpoint shape:** `GET /api/assimilation/next?timezone=` → `AssimilationNextDTO { Integer nextNoteId; AssimilationCountDTO counts; }` (`nextNoteId == null` when none). Returns the id only; the note page already loads the note by id. Counts are returned fresh so the menu badge/progress stay in sync after each assimilation.
4. **Toggle state:** module composable `useAssimilationView` (singleton), holding `showAssimilationSettings` and a `pendingOnForNoteId`. The note page (`NoteShowPage`) renders the panel when on and resets the toggle to `pendingOnForNoteId === currentNoteId` on each note change (so plain navigation ⇒ off; navigation via menu/keep/skip ⇒ on). Not persisted ⇒ satisfies "not saved anywhere".
5. **Shared navigation action:** `goToNextAssimilation()` used by both the menu item and keep/skip. It calls `/api/assimilation/next`, updates counts, fires the right toast, sets `pendingOnForNoteId`, and routes to `noteShow`.
6. **Menu item becomes an action, not a link.** `NavigationItem.vue` already special-cases `resumeRecall` as a click handler; `assimilate` joins that path. The Home page "Assimilate" card uses the same action.
7. **Old pages can coexist** until the final cleanup phase ⇒ stop-safe ordering.

## E2E coexistence strategy (keeps every commit green)

Two facts make small, always-green commits possible:

1. **The inline panel reuses the same `data-test` selectors** as today's pages (`keep-for-recall`, `open-refine-note-modal`, the spelling popup, the memory-tracker table, `#main-note-content`). So `assumeAssimilationPage()` helper methods work unchanged against the inline panel; only the **entry navigation** differs.
2. **Both old pages survive until Phase 5.** `/assimilate` (queue) is reached by `assimilation().navigateToAssimilationPage()` (clicks the sidebar item); `/assimilate/:noteId` (single note) is reached by the dropdown via `noteMoreOptionsForm.openAssimilationPage()` / `notePage.openAssimilationSettings()`.

Entry points migrate one at a time:
- **Phase 1** rewires the **dropdown** page-objects (`openAssimilationPage`, `assimilateNote`, `notePage.openAssimilationSettings`) to the inline toggle. The `/assimilate/:noteId` route still exists but is no longer used by tests.
- **Phase 2** decouples `navigateToAssimilationPage()` from the menu by switching it to `cy.visit('/assimilate')` (queue page still present), so legacy queue scenarios stay green while the menu becomes the new action.

Step defs touched: `assimilation.ts` (dropdown steps 57–74 in Phase 1; `navigateToAssimilationPage` steps in Phase 2/3), `recall.ts` (uses `navigateToAssimilationPage`, handled in Phase 2). Backend setup via `testability.assimilateNote` is unaffected.

`@wip` budget (max 5 project-wide): write each new scenario `@wip`, drive it red→green within its sub-phase, then drop the tag. Never carry more than one intentionally-failing scenario at a time.

## Commit-sized sub-phases

### Phase 1 — inline assimilation settings via a per-note toggle

- **1.1 (Structure) — done** Extract `recall/AssimilationPanel.vue` from `recall/Assimilation.vue` = `AssimilationSettings` + spelling popup + assimilate logic (everything except `NoteShow`). `Assimilation.vue` becomes `NoteShow` + `AssimilationPanel`. No behavior change; existing `Assimilation.spec.ts` and all assimilation E2E stay green. Justified by 1.3.
- **1.2 (Structure)** `DropdownMenuActionButton.vue` gains an optional `checked` prop that renders a check to the left of the icon. Unit test for checked/unchecked; existing usages render identically (default off). Justified by 1.3.
- **1.3 (Behavior)** Add `composables/useAssimilationView.ts` (singleton: `showAssimilationSettings`, `pendingOnForNoteId`, `toggle`, `requestOnFor(noteId)`, `resetForNote(noteId)`). `NoteMoreOptionsForm.vue`: replace navigation with a toggle bound to the composable + `checked`. `NoteShowPage.vue`: render `AssimilationPanel` at the bottom when on; reset the toggle on note-id change. Interim keep/skip: stay on the note and refresh recall info.
  - E2E (new `e2e_test/features/assimilation/assimilation_settings_panel.feature`): open a note → toggle on → check visible → Keep for recall → tracker created; navigate to another note → toggle off.
  - Unit: `NoteMoreOptionsForm.spec.ts` (toggle + check, no route push), `useAssimilationView` reset semantics, panel renders only when on.
- **1.4 (Behavior/test-migration)** Point the dropdown E2E page-objects at the inline toggle: `noteMoreOptionsForm.openAssimilationPage`→`openAssimilationSettings` (toggle on, wait for `keep-for-recall`, return `assumeAssimilationPage()`), `assimilateNote`, and `notePage.openAssimilationSettings` (drop the `/assimilate/` URL assumption). Keeps `assimilate_with_remembering_spelling.feature`, `understanding_check.feature`, `edit_when_assimilating.feature` green via the inline panel. Pure test-layer commit.

### Phase 2 — "Assimilate" menu navigates to the next note

- **2.1 (Behavior, backend)** `AssimilationService.getNextNoteToAssimilate()` (subscription-first then owned, **ignoring all daily caps**); `AssimilationNextDTO { Integer nextNoteId; AssimilationCountDTO counts; }`; `GET /api/assimilation/next?timezone=`. Controller tests: returns next id past the daily cap, subscription-before-owned ordering, `null` when none, counts correct. Regenerate TS client (same commit).
- **2.2 (Behavior, frontend)** `goToNextAssimilation()` (calls `/next`, updates `useAssimilationCount`, sets `pendingOnForNoteId`, routes to `noteShow`). Wire the "Assimilate" nav item (and Home `LearningFlowSection` card) to call it instead of routing — `NavigationItem.vue` joins the existing `resumeRecall` click-handler path. Switch `navigateToAssimilationPage()` E2E helper to `cy.visit('/assimilate')`.
  - E2E (new `assimilation_walkthrough.feature`, `@wip`→green): "Assimilate" from the menu lands on the first note with settings on.
- **2.3 (Behavior, frontend)** Toasts in `goToNextAssimilation()`: daily-goal-met (`nextNoteId != null && counts.dueCount === 0`, navigate anyway) and no-more (`nextNoteId == null`, stay on current note). Extend `assimilation_walkthrough.feature`: past-cap shows the goal toast but still loads a note; nothing-left shows the no-more toast and stays put.

### Phase 3 — keep/skip walk to the next note

- **3.1 (Behavior)** In `AssimilationPanel.vue`, on Keep-for-recall / Skip-recall success call `goToNextAssimilation()` (replaces the Phase 1 interim). Same toasts. Update the new walkthrough scenarios to expect walk-to-next; update any dropdown-entry feature whose post-keep assertion assumed "stay" (re-jump to the specific note where needed — selectors already match). Land production flip + affected feature updates together so the suite stays green.

### Phase 4 — thin progress bar under the "Assimilate" menu item

- **4.1 (Behavior)** In `NavigationItem.vue` (assimilate item) render a thin bar only when `assimilatedCountOfTheDay > 0 && dueCount > 0`; width = `assimilated / (assimilated + dueCount)`. Uses existing menu-data counts; no backend change. Unit: hidden at 0 and when complete/empty, visible mid-way. Optional E2E assertion in the walkthrough.

### Phase 5 — remove old pages, routes, and dead deps

- **5.1 (Structure)** Delete `/assimilate/:noteId` route, `AssimilateSingleNotePage(View).vue` + their specs/stories. Nothing references it after Phase 1.
- **5.2 (Structure/test)** Rewrite/remove the legacy queue scenarios in `assimilating.feature` (now superseded by the walkthrough feature); remove `assimilation().navigateToAssimilationPage()` and the queue-only helpers (`expectToAssimilateAndTotal`, progress-bar/tooltip, "achieved your daily goal") from `assimilationPage.ts`. Migrate `recall.ts` setup steps to the walkthrough/testability path.
- **5.3 (Structure)** Delete `/assimilate` route, `AssimilationPage(View).vue` + specs/stories; drop page-only bits of `useAssimilationCount` and the `assimilationPage*` storybook decorators/scss.
- **5.4 (Structure, backend)** Remove `GET /api/assimilation/assimilating` + `getNotesToAssimilate()` and now-dead daily-cap-gated streaming once unused; regenerate TS client.
- **5.5 (Structure)** Collapse `recall/Assimilation.vue` into `AssimilationPanel.vue` if redundant; remove any remaining dead composables/page-objects. Verify: no `/assimilate` references remain; assimilation + recall E2E green.

## Behavior change to confirm with stakeholders
Existing `assimilating.feature` asserts the daily cap (e.g. 2) **stops** assimilation. The new model lets the user **continue past the cap** with a toast; those scenarios are superseded by the walkthrough feature and rewritten/removed in Phase 5.
