# Inline autosaving folder and notebook titles

**Status:** in progress

**Type:** ad-hoc quick plan

**Goal:** A writable folder or notebook page has exactly one title editor: its
page heading. Edits save automatically, failures stay visible beside the
heading, and the former manual title-editing surfaces no longer exist.

## Scope

- Make the folder-page heading directly editable and autosave it.
- Give the notebook-page heading the same interaction.
- Remove the folder Settings rename form and the notebook click-to-edit /
  Update / Cancel / confirmation flow.
- Keep readonly notebook/folder views readonly.
- Keep the existing backend contracts: folder rename already has a focused
  endpoint, while notebook update already validates and persists an optional
  name. No migration or generated-client change is required.
- Do not change ordinary note-title editing; it is only the precedent for the
  inline autosave interaction.

## Current-state discoveries

- `FolderPage.vue` renders a plain `<h1>`; renaming is duplicated in the
  Settings tab through `FolderSettings.vue`, `useFolderAdmin`, and
  `renameFolderOnPage`.
- `NotebookPageView.vue` already puts renaming in the summary, but
  `NotebookPageNameEditor.vue` uses a second interaction mode with explicit
  Update, Cancel, confirmation, and success-toast behavior rather than
  autosave.
- `PathNameEditor.vue` already supplies the inline contenteditable heading,
  illegal path-character normalization, Enter-to-blur behavior, and inline
  error slot.
- `useDebouncedTextAutosave.ts` already defines the product autosave contract:
  persist after one second of inactivity and flush on blur/unmount. The new
  page-title behavior should reuse it, including its protection against stale
  API responses overwriting a newer draft.
- Folder rename already refreshes sidebar structural listings and reloads the
  folder realm. Notebook rename already emits the updated notebook so
  `NotebookPage` reloads its realm.
- Existing backend controller tests cover notebook trim/blank validation and
  folder authorization/name conflicts. This plan changes frontend interaction,
  not those contracts.
- ADR check: ADR 0000 is the only Accepted ADR and imposes process rather than
  title-editing constraints. ADR 0001 is Proposed, so it is not binding; its
  current `wiki link` wording can still be preserved in the notebook warning.

## Design decisions

1. **One editor, always inline.** The heading itself is the editor. There is no
   separate edit mode, save button, cancel button, or Settings form.
2. **Established autosave semantics.** A valid changed name saves after the
   existing one-second debounce and immediately on blur or Enter. Unchanged or
   blank values do not call the API; blank input shows a local inline error.
   Returning to the saved value or becoming invalid must cancel any older
   pending draft so a stale intermediate name cannot save later.
3. **Fail visibly and keep the draft.** Wrapped SDK errors must be propagated
   into the autosave boundary, normalized with `toOpenApiError`, and rendered
   beside the heading so the user can correct and retry. Do not catch merely to
   log, reset, or continue silently.
4. **Keep the notebook risk visible without blocking autosave.** Retain the
   warning that renaming may break wiki links from other notebooks, but make it
   non-blocking. Remove the confirmation popup because an explicit confirmation
   is a second save workflow and contradicts autosave.
5. **Generalize only after the first concrete slice.** Implement the folder
   behavior first. In the notebook phase, extract the proven heading/autosave
   mechanics into one capability-named shared component and use it from both
   pages. Folder/notebook API adapters remain domain-specific.
6. **No success-toast churn.** The updated heading/sidebar is the positive
   success signal. Autosave errors remain inline; successful pauses while
   typing should not generate repeated success toasts.

## Phases

| Phase | Type | Status | One observable outcome |
|---|---|---|---|
| 1. Folder heading rename | Behavior | done | On an owned folder page, editing the heading automatically persists the new folder name, and Settings contains no second rename control. |
| 2. Notebook heading rename and consolidation | Behavior | planned | On an owned notebook page, editing the same kind of heading automatically persists the new notebook name, with all former manual title controls removed. |

## Phase 1 — Folder heading rename

**Type:** Behavior

**Status:** done

**Result:** The owned-folder heading is the autosaving name editor, including
trim/blank/conflict handling and page/sidebar refresh. The Settings rename form
and its obsolete state/helpers are gone; move and dissolve remain unchanged.

**Verification:** Mounted `FolderPage` coverage passed 8/8, the folder
organization E2E feature passed 10/10, the refactor gate completed, and
repository lint/format plus diff-whitespace checks passed.

**Learning for Phase 2:** The concrete folder slice confirms that
`PathNameEditor` plus `useDebouncedTextAutosave` can own draft, normalization,
flush, and stale-save cancellation. Consolidation should preserve this behavior
while keeping each page's persistence and refresh callback domain-specific.

## Phase 2 — Notebook heading rename and consolidation

**Type:** Behavior

**Status:** planned

**Precondition:** The learner owns a notebook and is viewing its notebook page.

**Trigger:** The learner edits the notebook heading, then pauses, presses Enter,
or moves focus away.

**Postcondition:** The notebook name is persisted and still present after page
reload; the wiki-link risk is visible without a modal; folder and notebook pages
share one heading/autosave implementation; no click-to-edit, Update, Cancel,
confirmation, or alternate title editor remains.

### Test-first work

1. Rewrite the notebook-title case in
   `frontend/tests/pages/NotebookPageView.settings.spec.ts` through the mounted
   page boundary:
   - editing/blurring calls `updateNotebook` with the trimmed name and current
     description/memory-tracking values, then emits the updated notebook;
   - the wiki-link warning remains visible without creating a popup;
   - blank/unchanged values do not save and API errors remain inline;
   - old edit/update/cancel controls are absent.
2. Update the existing
   `e2e_test/features/notebooks/notebook_catalog_navigation.feature` scenario
   and `notebookPage().rename(...)` to edit the heading directly, blur, call
   `waitUntilAppIsNotBusy()`, reload the notebook page, and assert the saved
   summary name. Remove the popup-confirm action.
3. Retire `frontend/tests/components/notebook/NotebookPageNameEditor.spec.ts`
   when its warning/interaction coverage has moved to the mounted page test.
   Do not leave a second low-level test surface for the deleted interaction.

### Smallest production change and consolidation

1. Extract the proven folder heading/autosave mechanics into one shared,
   capability-named component (for example
   `frontend/src/components/commons/AutosavingPageNameEditor.vue`). Keep it
   limited to heading rendering, draft/validation state, debounce/flush, and
   error presentation; it receives an async domain persistence callback.
2. Use that component from both `FolderPage.vue` and `NotebookPageView.vue`.
   Keep the folder and notebook API calls at their page/domain boundary rather
   than teaching the shared component about controller types.
3. For notebooks, persist via `NotebookController.updateNotebook` with
   `{ ...settingsBody, name }`, emit `notebook-updated` on success, and
   propagate wrapped errors to the shared editor. Preserve the current settings
   payload so a name save cannot reset `skipMemoryTrackingEntirely`.
4. Delete `NotebookPageNameEditor.vue` and remove its import, popup dependency,
   manual editing state, Update/Cancel selectors, confirmation, and title
   success toast. Remove the temporary folder-specific editor after both pages
   use the shared component.
5. Audit for stale duplicate paths and wording:
   `folder-rename-submit`, `notebook-page-name-update`,
   `notebook-page-name-cancel`, `Click to rename notebook`, and
   `submitNotebookNameUpdate` must have no live product/test references; the
   former Settings-tab `folder-name` state/helpers must also be gone (the new
   folder form's legitimate `folder-name` selector remains). Update the
   folder-organize page-object comment so it describes move/dissolve only.

### Verification and final boundary

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/pages/NotebookPageView.settings.spec.ts
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/pages/FolderPage.renameDissolve.spec.ts
CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/notebooks/notebook_catalog_navigation.feature
CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/folder_organization/folder_organization.feature
CURSOR_DEV=true nix develop -c pnpm frontend:test
scripts/check_diff_whitespace.sh
```

Run the no-duplicate search above, Jidoka, and `post-change-refactor`; remove all
`@wip`; mark the plan done; prune this execution diary down to a short durable
result; commit and push.

## Expected final ownership

| Concern | Single owner |
|---|---|
| Inline page-name editing, debounce/flush, local blank/error presentation | Shared autosaving page-name editor |
| Folder rename API, sidebar invalidation, folder reload | Folder page/domain adapter |
| Notebook update API and notebook-updated event | Notebook page/domain adapter |
| Folder move/dissolve UI and state | `FolderSettings.vue` + `useFolderAdmin` |
| Main user behavior coverage | Existing folder-organization and notebook-catalog-navigation E2E features |
