# Assimilation Restructure Plan

Restructure assimilation from two dedicated pages (`/assimilate` queue, `/assimilate/:noteId` single note) into an **inline settings panel on the normal note page**, driven by a per-note view toggle, with menu/button navigation that walks the user through notes to assimilate one at a time.

## Current state (discovered)

- Inline assimilation on `/n/:noteId` via `AssimilationPanel` on `NoteShowPage` when `useAssimilationView` is on (toggle in `NoteMoreOptionsForm.vue`, or menu/keep/skip via `goToNextAssimilation`).
- Main menu "Assimilate" (`NavigationItem.vue`) is a **click action** calling `goToNextAssimilation()` (not a route). Dedicated `/assimilate` pages removed (Phases 5.1, 5.3).
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
7. **Stop-safe ordering** — dedicated assimilation pages removed in Phase 5; backend `assimilating` list endpoint remains until Phase 5.4.

## E2E coexistence strategy (keeps every commit green)

Two facts make small, always-green commits possible:

1. **The inline panel reuses the same `data-test` selectors** as today's pages (`keep-for-recall`, `open-refine-note-modal`, the spelling popup, the memory-tracker table, `#main-note-content`). So `assumeAssimilationPage()` helper methods work unchanged against the inline panel; only the **entry navigation** differs.
2. **E2E entry** — menu uses `startAssimilationFromMenu()`; dropdown uses `openAssimilationSettings()` on `/n/:noteId`. `assumeAssimilationPage()` targets the inline panel selectors.

Entry points migrate one at a time:
- **Phase 1 (done)** rewired the **dropdown** page-objects (`openAssimilationSettings`, `assimilateNote`, `notePage.openAssimilationSettings`) to the inline toggle.
- **Phase 2** wires the menu to `goToNextAssimilation()` instead of routing to `/assimilate`.

Step defs touched: `assimilation.ts` (dropdown steps in Phase 1; walkthrough/menu steps in Phase 2+), `recall.ts` (assimilation setup via `testability.assimilateNote` after Phase 5.2). Backend setup via `testability.assimilateNote` is unaffected.

`@wip` budget (max 5 project-wide): write each new scenario `@wip`, drive it red→green within its sub-phase, then drop the tag. Never carry more than one intentionally-failing scenario at a time.

## Commit-sized sub-phases

### Phase 1 — inline assimilation settings via a per-note toggle

- **1.1 (Structure) — done** Extract `recall/AssimilationPanel.vue` from `recall/Assimilation.vue` = `AssimilationSettings` + spelling popup + assimilate logic (everything except `NoteShow`). `Assimilation.vue` becomes `NoteShow` + `AssimilationPanel`. No behavior change; existing `Assimilation.spec.ts` and all assimilation E2E stay green. Justified by 1.3.
- **1.2 (Structure) — done** `DropdownMenuActionButton.vue` gains an optional `checked` prop that renders a check to the left of the icon. Unit test for checked/unchecked; existing usages render identically (default off). Justified by 1.3.
- **1.3 (Behavior) — done** Add `composables/useAssimilationView.ts` (singleton: `showAssimilationSettings`, `pendingOnForNoteId`, `toggle`, `requestOnFor(noteId)`, `resetForNote(noteId)`). `NoteMoreOptionsForm.vue`: replace navigation with a toggle bound to the composable + `checked`. `NoteShowPage.vue`: render `AssimilationPanel` at the bottom when on; reset the toggle on note-id change. Interim keep/skip: stay on the note and refresh recall info.
  - E2E (`e2e_test/features/assimilation/assimilation_settings_panel.feature`): open a note → toggle on → check visible → Keep for recall → tracker created; navigate to another note → toggle off.
  - Unit: `NoteMoreOptionsForm.spec.ts` (toggle + check, no route push), `useAssimilationView` reset semantics, panel renders only when on.
- **1.4 (Behavior/test-migration) — done** Dropdown E2E page-objects use `openAssimilationSettings` (toggle on, wait for `keep-for-recall`, return `assumeAssimilationPage()`), `assimilateNote`, and `notePage.openAssimilationSettings` (no `/assimilate/` URL). `assimilate_with_remembering_spelling.feature`, `understanding_check.feature`, `edit_when_assimilating.feature` green via inline panel.

### Phase 2 — "Assimilate" menu navigates to the next note

- **2.1 (Behavior, backend) — done** `AssimilationService.getNextNoteToAssimilate()` (subscription-first then owned, **ignoring all daily caps**); `AssimilationNextDTO { Integer nextNoteId; AssimilationCountDTO counts; }`; `GET /api/assimilation/next?timezone=`. Controller tests: returns next id past the daily cap, subscription-before-owned ordering, `null` when none, counts correct. Regenerate TS client (same commit).
- **2.2 (Behavior, frontend)** `goToNextAssimilation()` (calls `/next`, updates `useAssimilationCount`, sets `pendingOnForNoteId`, routes to `noteShow`). Wire the "Assimilate" nav item (and Home `LearningFlowSection` card) to call it instead of routing — `NavigationItem.vue` joins the existing `resumeRecall` click-handler path. Switch `navigateToAssimilationPage()` E2E helper to `cy.visit('/assimilate')`.
  - E2E (new `assimilation_walkthrough.feature`, `@wip`→green): "Assimilate" from the menu lands on the first note with settings on.
- **2.3 (Behavior, frontend) — done** Toasts in `goToNextAssimilation()`: daily-goal-met (`nextNoteId != null && counts.dueCount === 0`, navigate anyway) and no-more (`nextNoteId == null`, stay on current note). `assimilation_walkthrough.feature`: past-cap shows the goal toast but still loads a note; nothing-left shows the no-more toast and stays put.

### Phase 3 — keep/skip walk to the next note

- **3.1 (Behavior) — done** In `AssimilationPanel.vue`, on Keep-for-recall / Skip-recall success call `goToNextAssimilation()` (replaces the Phase 1 interim). Same toasts; `goToNextAssimilation` returns whether navigation happened and dismisses the panel when there is no next note. Walkthrough feature extended with keep-walk scenarios; `assimilation_settings_panel.feature` re-opens settings after keep to assert disabled button.

### Phase 4 — thin progress bar under the "Assimilate" menu item

- **4.1 (Behavior) — done** `AssimilationMenuProgress.vue` in all `NavigationItem` branches: thin bar when `assimilatedCountOfTheDay > 0 && dueCount > 0`; width = `assimilated / (assimilated + dueCount)`. Uses `useAssimilationCount` from menu-data; no backend change. Unit tests in `MainMenu.spec.ts`; E2E scenario in `assimilation_walkthrough.feature`.

### Phase 5 — remove old pages, routes, and dead deps

- **5.1 (Structure) — done** Delete `/assimilate/:noteId` route, `AssimilateSingleNotePage(View).vue` + their specs/stories. Nothing references it after Phase 1.
- **5.2 (Structure/test) — done** Removed `navigateToAssimilationPage()` and the queue-only helpers (`expectToAssimilateAndTotal`, progress-bar/tooltip) from `assimilationPage.ts`; migrated `recall.ts`/`assimilation.ts` setup to the walkthrough/testability path. Added `recall/AssimilationProgressSummary.vue` (assimilated/planned/total) inside the inline Assimilation settings panel, asserted in `assimilation_walkthrough.feature`; recall scenarios assert the assimilation due count via menu-data and `RecallPage` now keeps `totalAssimilatedCount` in sync with its recall fetches.
- **5.3 (Structure) — done** Delete `/assimilate` route, `AssimilationPage(View).vue` + specs/stories; drop page-only bits of `useAssimilationCount` and the `assimilationPage*` storybook decorators/scss.
- **5.4 (Structure, backend)** Remove `GET /api/assimilation/assimilating` + `getNotesToAssimilate()` and now-dead daily-cap-gated streaming once unused; regenerate TS client.
- **5.5 (Structure) — done** Removed redundant `recall/Assimilation.vue` wrapper (padding already on `NoteShowPage`); retargeted unit tests to `AssimilationPanel.spec.ts`. E2E `assumeAssimilationPage()` name kept (inline panel helper).

## Behavior change to confirm with stakeholders
Existing `assimilating.feature` asserts the daily cap (e.g. 2) **stops** assimilation. The new model lets the user **continue past the cap** with a toast; those scenarios are superseded by the walkthrough feature and rewritten/removed in Phase 5.
