# Property memory tracker guard on delete/rename

When a note property is deleted or its key is renamed, detect linked property memory trackers, ask the user to confirm, then soft-delete or update the tracker before persisting the content change. Covers rich property rows and raw Markdown save.

**Status:** planned (implementation reverted; this plan incorporates lessons from the first attempt)

---

## Requirement (observable outcomes)

1. Removing a property that has a property memory tracker prompts for confirmation; on confirm, the tracker is soft-deleted and the property is removed/saved.
2. Renaming a property key that has a property memory tracker prompts for confirmation; on confirm, the tracker’s `property_key` is updated (schedule/stats preserved) and the new key is saved.
3. The same guard applies when the user edits frontmatter in **Markdown mode** (on debounced save, not only rich property rows).
4. Cancel leaves the property unchanged (rich editor) or blocks save (Markdown mode).

**Out of scope:** Index note editor (`ScopedIndexNoteEditor`) — no `noteId`, no property trackers.

---

## Grounding (how the system works today)

- Properties are YAML frontmatter in `note.content`; rich UI in `RichFrontmatterProperties.vue`; save via `PATCH /api/text_content/{note}/content`.
- Property trackers: `memory_tracker.property_key` (non-empty string); list per note via `GET /api/notes/{note}/note-info`.
- Delete tracker: `POST /api/memory-trackers/{id}/soft-delete` (exists).
- Rename tracker: **no public API** today (`setPropertyKey` only in `NoteService.rehomeNoteLevelMemoryTrackerToSourceProperty`).
- Confirmation: `usePopups().popups.confirm` + `Popups.vue` (OK / Cancel).
- Content save debounce lives in `TextContentWrapper.vue` (`changerInner` → `updateTextField`), not in `NoteEditableContent.vue`.

---

## Lessons from the first implementation attempt

| Area | What worked | Friction / fix for next time |
|------|-------------|------------------------------|
| Backend rename API | `MemoryTrackerService.updatePropertyKey` + `PATCH …/property-key`; controller tests; regen OpenAPI | Must run `pnpm generateTypeScript` before `backend:test_only` (OpenAPI approval test fails otherwise) |
| `diffFrontmatterPropertyKeyChanges` | Pair removed/added keys with same trimmed value → rename; rest → removal | Invalid frontmatter on one side → return `[]` (unclosed `---` fence parses as invalid, not empty props) |
| `usePropertyMemoryTrackerGuard` | Lazy `getNoteInfo` cache; `confirmAndApplyRemoval` / `confirmAndApplyRename` / batch helper | Invalidate cache after mutations |
| Rich editor wiring | Async `removeRow` / `commitRow` in `RichFrontmatterProperties.vue` | Only when `noteId` is set |
| Markdown save | Hook must run in **`TextContentWrapper.changerInner`** before `updateTextField`, not before `onUpdate` — otherwise guard runs too late or save proceeds without guard | Add optional `beforeSaveContent(lastSaved, newValue) => Promise<boolean>` prop; wire from `NoteEditableContent` when `asMarkdown` |
| Ignored files | `NoteEditableContent.vue` and `TextContentWrapper.vue` are `.cursorignore` | Edit via explicit path (agent may need shell) or temporarily read outside default index |
| Frontend unit tests | `mockImplementationOnce` for `confirm`; `vi.restoreAllMocks()` in `afterEach` for guard tests | Avoid async bleed between tests (first test’s `softDelete` completing during second test) |
| Rich editor tests | Property rows are **sorted by key**; remove button `[0]` may not be the intended key | Use single-property fixtures or query by `data-property-key` |
| E2E | `clickPopupConfirmOk()` must be **chained after** dialog is visible (`cy.get('dialog').filter(':visible').contains('memory tracker')`); avoid firing OK inside `.then()` without Cypress queue | After OK, `pageIsNotLoading()` before `flushPendingContentSave()` |
| E2E (not green at revert) | Delete/rename scenarios were added but failed: tracker still visible after delete; rename label not updated | Likely race: async guard + debounced save; confirm dialog timing; verify assimilation panel reloads fresh `note-info` after property edit |

---

## Key design decisions

- **Match trackers** by non-empty `propertyKey` equal to affected frontmatter key (include `removedFromTracking` trackers).
- **Rename** updates `property_key` in place (preserve stats); **delete** uses existing soft-delete.
- **Conflict** on rename to a key that already has a tracker → backend `409`; show `popups.alert`; rich mode reverts key.
- **Markdown rename heuristic:** one removed + one added key with same value → rename; otherwise removals only.
- **Batch markdown changes:** process renames then removals; cancel any → abort whole save.

---

## Phases and commit-sized sub-phases

**Commit discipline:** each sub-phase = one commit, compiles, targeted tests green. Push after each phase boundary (deploy gate). E2E: `cypress run --spec e2e_test/features/recall/property_memory_tracker.feature` only.

Permanent artifacts (capability-named): `property_memory_tracker.feature`, `usePropertyMemoryTrackerGuard.ts`, `diffFrontmatterPropertyKeyChanges` in `noteContentFrontmatter.ts`.

---

### Phase 1 — Confirm and delete tracker when removing a property (rich editor) *(Behavior)*

**Outcome:** User removes a property row on the note page; if that key has a property tracker, a confirm dialog appears; on OK the tracker is soft-deleted and the property is removed and content saved; on Cancel the row stays.

**Depends on:** existing `softDelete` API only (no new backend endpoint).

- **1a — Guard composable (removal only)** *(structure, minimal)*
  - Add `usePropertyMemoryTrackerGuard` with `confirmAndApplyRemoval` only (+ `getNoteInfo` load/cache).
  - Frontend unit tests for: no `noteId` → proceed; no tracker → no confirm; confirm → `softDelete`; cancel → abort.
  - **Commit.**

- **1b — Wire `removeRow` in `RichFrontmatterProperties.vue`** *(behavior)*
  - `await confirmAndApplyRemoval(key)` before `removePropertyRowAt`.
  - Extend `RichMarkdownEditor.properties.spec.ts` (mock APIs + `usePopups`).
  - **Commit.**

- **1c — E2E: delete tracked property in rich mode** *(behavior, `@wip` → green)*
  - Scenario in `property_memory_tracker.feature`: assimilate property → visit note → remove property → confirm → tracker absent on assimilation panel.
  - Page object: `removeRichNoteProperty` with explicit dialog wait + `clickPopupConfirmOk()` + `flushPendingContentSave()`.
  - Step defs + `expectPropertyMemoryTrackerAbsent` on assimilation page.
  - **Commit.** → **Phase 1 deploy gate.**

---

### Phase 2 — Confirm and update tracker when renaming a property key (rich editor) *(Behavior)*

**Outcome:** User renames a property key in the rich editor; confirm updates tracker `property_key`; cancel reverts the key.

- **2a — Backend `PATCH /api/memory-trackers/{id}/property-key`** *(structure)*
  - `UpdateMemoryTrackerPropertyKeyDTO`, `MemoryTrackerService.updatePropertyKey`, controller endpoint.
  - `MemoryTrackerBuilder.propertyKey()` for tests.
  - Regenerate TS client + OpenAPI.
  - Controller tests: rename preserves stats; conflict `409`; reject note-level tracker.
  - **Commit.**

- **2b — `diffFrontmatterPropertyKeyChanges` helper** *(structure)*
  - Pure function in `noteContentFrontmatter.ts` + unit tests (removal, rename-by-value, ambiguous same-value pairs, invalid frontmatter).
  - **Commit.**

- **2c — Extend composable with `confirmAndApplyRename`** *(behavior)*
  - Call `MemoryTrackerController.updatePropertyKey`; alert on error.
  - Unit tests.
  - **Commit.**

- **2d — Wire `commitRow` key-change path** *(behavior)*
  - Compare snapshot key vs committed key; `await confirmAndApplyRename`.
  - Frontend tests: rename confirm/cancel/no-tracker.
  - **Commit.**

- **2e — E2E: rename tracked property key in rich mode** *(behavior, `@wip` → green)*
  - Scenario: rename `topic` → `subject` with confirm → assimilation shows `property: subject`, not `topic`.
  - Page object: `renameRichNotePropertyKey`.
  - **Commit.** → **Phase 2 deploy gate.**

---

### Phase 3 — Same guard on Markdown mode save *(Behavior)*

**Outcome:** Editing raw Markdown that removes or renames a tracked property key triggers the same confirmations on debounced save; cancel blocks the API save.

- **3a — `beforeSaveContent` hook on `TextContentWrapper`** *(structure)*
  - Optional prop `(lastSaved, newValue) => Promise<boolean>`; call in `changerInner` before `updateTextField` for `edit content`.
  - No observable change until wired.
  - **Commit.**

- **3b — Wire `NoteEditableContent`** *(behavior)*
  - `usePropertyMemoryTrackerGuard` + `diffFrontmatterPropertyKeyChanges` in `beforeSaveContent` when `asMarkdown`.
  - `NoteEditableContent.spec.ts`: remove key / rename key / cancel → no `updateNoteContent`.
  - **Commit.**

- **3c — E2E: Markdown mode remove tracked property** *(behavior, optional)*
  - Switch to Markdown, delete `topic:` line, save, confirm, tracker gone.
  - Only if rich-mode E2E is stable; otherwise defer.
  - **Commit.** → **Phase 3 deploy gate.**

---

## Stop-safe ordering rationale

1. **Phase 1** — Delete is the most damaging orphan case; uses only existing APIs; immediately usable.
2. **Phase 2** — Rename needs new API but completes the rich-editor story.
3. **Phase 3** — Markdown is a second entry point; reuses composable + diff helper from Phase 2.

Stopping after any phase leaves coherent, tested behavior with no orphaned backend or UI scaffolding.

---

## Files touched (reference)

| Area | Files |
|------|-------|
| Backend | `MemoryTrackerController`, `MemoryTrackerService`, `UpdateMemoryTrackerPropertyKeyDTO`, `MemoryTrackerControllerTest`, `MemoryTrackerBuilder` |
| Frontend core | `noteContentFrontmatter.ts`, `usePropertyMemoryTrackerGuard.ts`, `RichFrontmatterProperties.vue`, `TextContentWrapper.vue`, `NoteEditableContent.vue` |
| Tests | `noteContentFrontmatter.spec.ts`, `usePropertyMemoryTrackerGuard.spec.ts`, `RichMarkdownEditor.properties.spec.ts`, `NoteEditableContent.spec.ts` |
| E2E | `property_memory_tracker.feature`, `notePage.ts`, `assimilationPage.ts`, step defs |

---

## Verification commands

```bash
CURSOR_DEV=true nix develop -c pnpm backend:test_only
CURSOR_DEV=true nix develop -c pnpm generateTypeScript   # after backend API changes
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/utils/noteContentFrontmatter.spec.ts
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/composables/usePropertyMemoryTrackerGuard.spec.ts
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/form/RichMarkdownEditor.properties.spec.ts
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/NoteEditableContent.spec.ts
CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/recall/property_memory_tracker.feature
```
