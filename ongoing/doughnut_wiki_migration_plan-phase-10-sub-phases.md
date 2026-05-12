# Doughnut Wiki Phase 10 — sub-phases plan

**Parent plan:** `ongoing/doughnut_wiki_migration_plan.md` — Phase 10 "Index-scoped configuration (notebook root + folders)".

**Goal of this plan:** break Phase 10 into small, stop-safe slices. Each behavior slice delivers one observable improvement around index notes, folder pages, scoped defaults, search, breadcrumbs, or sidebar navigation. Structure slices are only included where they directly enable the next behavior slice.

## Guiding principles for these sub-phases

- **Stop-safe:** after any sub-phase, the app remains shippable. Existing note editing, folder listing, and notebook navigation do not regress.
- **Behavior first:** favor user-visible slices (editable notebook index, folder page, direct container saves) over broad backend restructuring.
- **One source of truth from 10.14 onward:** scoped defaults live in notebook/folder `indexContent` frontmatter. Earlier `index_note_id` work is migration input, not the final target.
- **JPA for historical pointers in code:** for completed 10.1–10.13 work, map notebook/folder index pointers with entity associations (for example `Notebook` / `Folder` to `Note`) and exercise them through normal persistence and test fixtures. From 10.14 onward, avoid adding new pointer behavior except migration/cleanup.
- **One editor pipeline:** notebook page, folder page, and `/d/n/:noteId` use compatible Markdown/frontmatter editing behavior wherever practical, but notebook/folder index saves target container `indexContent`.
- **Route owns mode:** pages are either a container page (notebook or folder) or a note page. There is no "active folder + active note" product state.
- **Tests by capability:** name tests/features after behaviors (index editing, folder navigation, search), not sub-phase numbers.

---

## Central design decisions

### Historical scoped index identity through 10.13

Notebook and folder index notes are ordinary notes titled `index` by convention. The product resolves hot reads by cached pointer:

```text
notebook.index_note_id -> note.id
folder.index_note_id   -> note.id
```

Title-based lookup remains useful for backfill, import, repair, and compatibility with the existing notebook index behavior, but it should not be the primary read path once a pointer exists.

In backend Java and tests, treat these pointers as JPA associations to `Note` (not as columns you set only via raw SQL).

From 10.14 onward, this is no longer the target architecture. The pointers and legacy index notes are migration inputs to populate notebook/folder `indexContent`, then cleanup removes the normal read/write dependency on designated index notes.

### Container index content carries scoped configuration from 10.14 onward

The first predefined properties are:

```yaml
title_pattern: "{{date}}"
question_generation_instruction: "Focus on definitions; avoid trick wording."
```

These are visible/editable on notebook and folder index editors and are serialized into the container's `indexContent` frontmatter. Normal notes, including migrated `index_to_be_deleted` notes, do not get these predefined rows.

### Likely implementation touchpoints

- Notebook index page: `frontend/src/pages/NotebookPage.vue`, `frontend/src/pages/NotebookPageView.vue`, `backend/src/main/java/com/odde/doughnut/controllers/dto/NotebookPageClientView.java`, `backend/src/main/java/com/odde/doughnut/services/NotebookCatalogService.java`, `NotebookService`.
- Editor/frontmatter: `frontend/src/components/form/RichMarkdownEditor.vue`, `RichFrontmatterProperties.vue`, `frontend/src/utils/noteContentFrontmatter`.
- Folder model/API: `backend/src/main/java/com/odde/doughnut/entities/Folder.java`, `NotebookController`, `FolderListing`, `NotebookFolderIndexRow`, folder controller tests.
- Sidebar/routes: `frontend/src/components/notes/SidebarInner.vue`, `FolderSelector.vue`, `NoteSidebarToolbar.vue`, `frontend/src/composables/useCurrentNoteSidebarState.ts`.
- Breadcrumbs: `frontend/src/components/toolbars/BreadcrumbWithCircle.vue`, `frontend/src/components/notes/NoteShow.vue`.
- Search exclusion: `backend/src/main/java/com/odde/doughnut/controllers/SearchController.java`, `services/search/NoteSearchService.java`, `NoteRepository`, `NoteEmbeddingJdbcRepository`, frontend search components if result fixtures change.

---

## Lessons learned — why original 10.5 was split

An attempt to ship **folder index persistence + reconciliation + HTTP folder page + OpenAPI/codegen + Vue route/layout + sidebar navigation + shared index editor extraction + E2E** in one change set was **too large** for review and risk: many layers moved together, and regressions would be hard to attribute.

**Concrete findings:**

- **Persistence and hooks are a full slice:** `folder.index_note_id`, `ScopedIndexNoteService` folder branch, and reconciliation at **note create, move, soft-delete/restore, title update, and folder dissolve** mirror the notebook pointer story; missing one call site breaks pointer integrity without any UI.
- **API + generated client are a discrete step:** A new controller method changes the approved OpenAPI snapshot; `pnpm generateTypeScript` must run in the same slice as the Java change, which is already a sizable diff on its own.
- **Notebook UI refactor is separable structure:** Extracting the index editor block from `NotebookPageView` is **internally observable** (tests) but can ship **before** any folder route if it keeps notebook behavior equivalent — it directly enables a smaller folder-page slice.
- **Routing and shell are easy to get wrong in one go:** `folderPage` must be registered **before** the generic `/d/notebooks/:notebookId` pattern; `NotebookSidebarLayout` must treat the folder route as a **container** (like the notebook page) for breadcrumbs; sidebar click behavior is its own product contract.

**Decomposition approach (see sub-phases below):** one **structure** slice for folder pointer + hooks (no new folder-page route), one **behavior** slice for the folder-page **GET** API + codegen, one **structure** slice that **only** extracts the shared index editor from the notebook page, then one **behavior** slice for folder route, `FolderPage`, sidebar navigation, and wiring the shared editor with `folderId`. Remaining Phase 10 items are renumbered so each slice stays stop-safe and review-sized.

---

## Sub-phases

Numbering is **10.N** and is plan-only bookkeeping. Commit messages, tests, routes, DTOs, and classes should be named by product capability.

### 10.1 — Behavior: notebook page edits the index note inline and lazily creates it

**Why first:** The notebook index already exists as a preview. Making it editable and lazily creatable gives immediate user value before adding folder pages or cached pointers.

**Pre-condition:** User opens a notebook page with or without an index note.

**Trigger:** User edits the notebook index editor and saves.

**Post-condition:** If the index note existed, its Markdown/frontmatter is updated. If it did not exist, a root-level note titled `index` is created and the page now treats it as the notebook index.

**Scope:**
- Replace the current notebook index summary/edit-link-only surface with an embedded Markdown/frontmatter editor or a shared index-editor component.
- Keep notebook name, description, and management settings as-is; do not optimize the busy notebook layout in this phase.
- Use the existing root index resolution path if `notebook.index_note_id` is not implemented yet.

**Tests:**
- Frontend component/page test: notebook page with no index shows an editor and creates a root `index` note on save.
- Frontend component/page test: notebook page with an existing index edits that note rather than creating a duplicate.
- Targeted E2E if practical in the existing notebook feature: open notebook page, type index body, save, reload, body remains.

### 10.2 — Structure: persist and maintain `notebook.index_note_id`

**Why now:** The next behavior (default search exclusion for notebook index) needs an unambiguous designated index id.

**Scope:**
- Add a nullable `index_note_id` FK on `notebook`.
- Backfill from the existing root note titled `index` where unambiguous.
- Update notebook page APIs to return the cached pointer first.
- Centralize notebook-index create/update/delete/move/title-change consistency enough for the next search slice.
- Keep title lookup as repair/fallback only; do not add folder index behavior yet.

**Tests:**
- Backend controller/service tests: notebook page returns `indexNoteId` from the cached pointer.
- Backend migration/backfill test if the project has migration verification for similar schema changes.
- Existing notebook page tests from 10.1 stay green.

### 10.3 — Behavior: default search excludes the notebook index note

**Why now:** Uses the cached notebook pointer and delivers a clear user-facing search improvement before folder scope broadens the rule.

**Pre-condition:** Notebook has a designated index note containing searchable text and a normal note containing searchable text.

**Trigger:** User runs default note search for that text.

**Post-condition:** Normal notes appear; the designated notebook index note does not appear in default search results.

**Scope:**
- Exclude notes pointed to by `notebook.index_note_id` in literal/default note search.
- Decide in implementation whether semantic search should also exclude the same ids in this slice; if not, document and schedule it before Phase 10 closes.
- Do not introduce an "include index notes" UI toggle in this phase.

**Tests:**
- Backend search test through `SearchController` or `NoteSearchService`'s public entry point.
- Targeted search E2E only if default search behavior is already covered with reliable setup.

### 10.4 — Structure: introduce a scoped-index service for notebook and folder index operations

**Why now:** The next behavior adds folder pages. Keeping notebook and folder index handling cohesive prevents duplicating lazy-create, pointer maintenance, and index-only property checks.

**Scope:**
- Introduce a small service/domain concept for "index scope": notebook root or folder.
- Move notebook-index lazy-create and pointer maintenance behind that service.
- Keep existing notebook behavior externally unchanged.
- Do not add folder page UI yet.

**Tests:**
- Existing notebook index tests stay green.
- Focused backend tests for scope validation: notebook-root index note has `folderId` null; folder scope rejects notes from another notebook/folder once folder support is added.

### 10.5 — Structure: persist `folder.index_note_id` and folder index reconciliation

**Why now:** Completes the folder side of the scoped-index **model** before introducing a folder-page read API or SPA route; keeps pointer integrity when notes are created, moved, deleted, retitled, or when folders are dissolved.

**Scope:**
- Flyway: nullable `folder.index_note_id` FK to `note`, plus backfill where exactly one non-deleted note titled `index` exists in that folder (same ambiguity rules as notebook backfill).
- `Folder` entity: JPA `@ManyToOne` `indexNote`; optional wire `indexNoteId` in JSON for types that already serialize `Folder` (if exposing the pointer in listing payloads is undesirable in this slice, keep the association `@JsonIgnore` only and defer wire exposure to the folder-page API slice).
- `NoteRepository`: folder-scoped index candidates query (`LOWER(title) = 'index'`, non-deleted, `folder.id` match), analogous to notebook root candidates.
- `ScopedIndexNoteService`: implement folder `findDesignatedIndexNote` / `reconcileDesignatedIndexPointer` (replace stubs).
- Thin entry point parallel to notebook: e.g. `FolderService.reconcileFolderIndexNotePointer(folderId)` delegating to the scoped service.
- **Reconciliation hooks** (mirror every notebook pointer touchpoint that depends on folder membership): `NoteConstructionService` (create with `folderId`), `NoteMotionService` (old and new folder), `NoteService` destroy/restore, `TextContentController` title update path, `FolderRelocationService.dissolveFolder` (destination folder + notebook).

**Not in this slice:** `GET` folder page endpoint, new Vue routes, sidebar navigation changes, shared index component extraction.

**Tests:** Focused backend tests for folder pointer maintenance and folder scope validation in `ScopedIndexNoteService`; migration verification if the repo uses it for similar FK additions.

### 10.6 — Behavior: folder page data API

**Why now:** Establishes a single HTTP contract for notebook chrome + folder row + resolved designated `indexNoteId` before the SPA consumes it.

**Pre-condition:** Caller has read access to the notebook and folder.

**Trigger:** `GET` folder page for a folder that belongs to the notebook.

**Post-condition:** Response aggregates `NotebookClientView` (or equivalent chrome) and `Folder` with `indexNoteId` populated after designated-index resolution (cached pointer + repair), without requiring a note-show route.

**Scope:** DTO (e.g. `FolderPageClientView`), controller method on `NotebookCatalogService` / `NotebookController`, authorization and "folder in notebook" guards. Run `pnpm generateTypeScript` so `open_api_docs.yaml` and the TS client stay in sync (OpenAPI approval test).

**Tests:** Controller or integration test through the HTTP entry point; pointer/create behavior may be asserted here or remain covered in 10.5 depending on where create-note tests already live.

### 10.7 — Structure: extract shared index editor from the notebook page

**Why now:** Folder page should reuse the same index UX (loading, absent draft + save, present `NoteEditableContent`, 409 race handling) **without** copying a large block; this slice keeps the following folder-route slice thin and reviewable.

**Scope:** Extract the notebook index block from `NotebookPageView` into a reusable component parameterized by `notebookId`, optional `folderId` for create options, user-facing copy, `fetchPage` callback, and stable `data-testid` props so existing notebook tests stay aligned.

**External behavior:** Notebook page index editing is unchanged from a user perspective; regression = existing notebook page tests stay green.

**Not in this slice:** `FolderPage`, new routes, sidebar navigation.

**Tests:** Existing notebook page / component tests; add tests only if coverage would otherwise drop.

### 10.8 — Behavior: folder page route, shell, sidebar navigation, and folder index editing

**Why now:** Delivers the core product outcome: **folders are pages**, not only sidebar selection targets.

**Pre-condition:** User has a notebook with at least one folder.

**Trigger:** User activates folder navigation from the sidebar (per UX: e.g. folder label opens the page; chevron expands — match the tree pattern documented when implementing).

**Post-condition:** App navigates to the folder container route (not `noteShow`), shows the index editor via the shared component with `folderId`, lazy-creates the folder `index` note on first save using the existing create-note API with `folderId`; folder listing and organize flows still work.

**Scope:** `routeMetadata` entry **before** `/d/notebooks/:notebookId`, `routes.ts`, `DoughnutApp` notebook-sidebar route set, `NotebookSidebarLayout` breadcrumb handling for folder as container, `FolderPage.vue` loading 10.6 API, syncing `useCurrentNoteSidebarState` (notebook id, notebook chrome ref, active index note when present, `notebookSidebarUserActiveFolder`), `SidebarFolderItem` navigation to `folderPage`.

**Tests:** Frontend test for sidebar navigation to `folderPage`; E2E in folder/navigation capability: open folder page, edit/save folder index, reload, body remains.

### 10.9 — Behavior: folder page auto-scrolls the sidebar to the active folder

**Why now:** Small user-visible behavior that depends on the folder page route from 10.8.

**Pre-condition:** User opens a deeply nested folder page whose sidebar row is outside the visible scroll area.

**Trigger:** Folder page route loads.

**Post-condition:** The sidebar tree scrolls so the active folder row is visible.

**Scope:**
- Add `scrollIntoView`/equivalent behavior keyed by the active folder route.
- Avoid changing note-page scroll behavior unless the current implementation already uses the same active-folder marker.

**Tests:**
- Frontend component test for invoking scroll behavior on active folder row if practical.
- Manual/targeted E2E only if reliable scroll assertions already exist; otherwise keep unit/component coverage and record the manual verification in the phase notes.

### 10.10 — Behavior: breadcrumb folder links navigate to folder pages

**Why now:** Breadcrumbs should match the folder-page route before more index-specific properties are added.

**Pre-condition:** User views a note inside nested folders and sees breadcrumb folder segments.

**Trigger:** User clicks a folder segment in the breadcrumb.

**Post-condition:** App navigates to that folder's folder page.

**Scope:**
- Update breadcrumb link construction for folder ancestors.
- Keep the note title/current note breadcrumb behavior unchanged.

**Tests:**
- Frontend test around breadcrumb link destinations.
- E2E extension if an existing note-navigation feature already clicks breadcrumbs.

### 10.11 — Behavior: index notes show index-only predefined properties everywhere they are edited

**Why now:** Once notebook and folder pages both have index editors, the user should see the first scoped configuration fields consistently.

**Pre-condition:** User opens a notebook index, folder index, or the same index note through `/d/n/:noteId`.

**Trigger:** User edits index properties.

**Post-condition:** UI exposes index-only fields for `title_pattern` and `question_generation_instruction`, serializes them into leading YAML frontmatter, and normal notes do not show those predefined fields.

**Scope:**
- Add index-note detection by cached pointer/id where available, with safe fallback during transition.
- Add UI rows or metadata for the two predefined properties.
- Keep arbitrary frontmatter editing behavior unchanged.

**Tests:**
- Frontend editor tests: index note shows the two predefined fields and saves YAML.
- Frontend editor tests: normal note does not show those index-only predefined rows.
- Backend test only if the API needs to expose an explicit "isIndexNote" field.

### 10.12 — Behavior: new notes can use scoped `title_pattern`

**Why now:** This is the first concrete behavior powered by index frontmatter, and it can be delivered as a mostly frontend capability once the active context already carries the resolved index properties.

**Pre-condition:** Notebook or folder index frontmatter contains `title_pattern`, and the frontend has loaded the relevant index properties with the active note or active folder context.

**Trigger:** User creates a note from the sidebar or note-creation UI while a note or folder is active.

**Post-condition:** The create-note request is sent with an initial title rendered from the nearest loaded scoped `title_pattern`; existing explicit-title creation remains explicit.

**Scope:**
- Treat this as a frontend-owned defaulting feature: the backend create-note API should continue to receive an ordinary explicit title, not learn template rendering or implicit scoped defaults in this slice.
- Ensure the active note payload and active folder/page payload expose enough resolved index properties for note creation to decide immediately which pattern applies. Prefer one frontend shape for "current scoped index properties" whether the user is on a note page, notebook page, or folder page.
- Resolve inheritance before or during payload construction so the frontend does not need extra round trips at creation time. If the existing payload cannot include this without a small DTO/API addition, keep that backend change narrowly limited to exposing already-resolved index properties.
- Apply the pattern only where the creation flow lacks an explicit user title.
- Start with a small supported pattern set (e.g. date) implemented in a frontend utility rather than a broad template language.
- Keep the resolved properties refreshed when the active note changes, the active folder changes, or the user edits the relevant index note in the current page.

**Tests:**
- Frontend creation-flow tests: active note in a configured folder creates a note with the rendered patterned title; active folder page creates a note with the folder pattern; explicit title input still wins.
- Frontend state/payload test: active note and active folder contexts expose the same resolved index-properties shape to note creation.
- E2E: create a note in a configured folder and observe the patterned title.
- Unit tests for the frontend pattern renderer edge cases (unknown token, invalid pattern) if rendering is non-trivial.

### 10.13 — Behavior: question generation uses scoped instruction

**Why now:** It delivers the second planned property after the scoped config and inheritance mechanism are proven by `title_pattern`.

**Pre-condition:** Notebook or folder index frontmatter contains `question_generation_instruction`.

**Trigger:** User invokes question generation for a note in that scope.

**Post-condition:** The generation request includes the nearest scoped instruction without requiring that instruction on the normal note.

**Scope:**
- Resolve instruction via folder → parent folders → notebook index.
- Wire the instruction into the existing question-generation service prompt/options.
- Do not redesign question generation UI in this slice.

**Tests:**
- Backend service/controller test proving the resolved instruction is included in the generation request.
- Existing AI/question generation tests stay green.

### Direction change from 10.14 onward

Phases 10.1 through 10.13 shipped the index-note model far enough to prove editable notebook/folder configuration, scoped title patterns, and scoped question-generation instructions. From 10.14 onward, stop extending that model: the canonical index content moves onto the container itself:

```text
notebook.indexContent
folder.indexContent
```

The previous note titled `index` becomes legacy migration input only. After migration, notes may not be named `index`; migrated legacy index notes are renamed to `index_to_be_deleted` so they are visible for manual review but no longer participate in scoped configuration.

### 10.14 — Behavior: migrate index note content onto notebooks and folders

**Why now:** This preserves user-authored index text and frontmatter before removing the index-note save path.

**Pre-condition:** A notebook or folder may have a designated legacy index note created by earlier Phase 10 work.

**Trigger:** The database migration runs during deployment.

**Post-condition:** The notebook/folder row stores the former index note Markdown/frontmatter in `indexContent`; the former index note is renamed to `index_to_be_deleted`; notebook and folder page read APIs return the same visible index content as before, now from the container field.

**Scope:**
- Add nullable `indexContent` storage to notebooks and folders using the repo's normal naming conventions for DB columns, entity fields, DTOs, and generated API types.
- Flyway migration: copy the current designated index note content into the owning notebook/folder, then rename those notes to `index_to_be_deleted`.
- Preserve ambiguity handling explicitly: if a legacy pointer or title lookup is ambiguous, do not guess; leave enough diagnostic signal for manual cleanup.
- Update notebook/folder page backend reads so the page payload exposes container `indexContent` and no longer requires a note id to display the index editor.
- Regenerate the frontend API client in this phase if DTO signatures change.

**Tests:**
- Migration test or repository-level verification proving notebook and folder legacy index content is copied and legacy notes are renamed.
- Backend page/API test: notebook and folder pages expose `indexContent` after migration without resolving an index note page.
- Existing 10.12 and 10.13 behavior tests stay green until the next phase moves scoped configuration reads.

### 10.15 — Behavior: scoped configuration resolves from container `indexContent`

**Why now:** `title_pattern` and `question_generation_instruction` already shipped; they must keep working after index notes stop being canonical.

**Pre-condition:** Notebook and folder `indexContent` frontmatter contains scoped configuration.

**Trigger:** User creates a note in that scope or invokes question generation for a note in that scope.

**Post-condition:** The nearest folder/notebook `indexContent` frontmatter supplies the scoped title pattern and question-generation instruction. A legacy `index_to_be_deleted` note has no special behavior.

**Scope:**
- Replace normal scoped-config reads from index note lookup/pointers with parsing of notebook/folder `indexContent`.
- Keep inheritance behavior from 10.12/10.13: folder → parent folders → notebook.
- Remove index-note-specific editor affordances from normal note pages; scoped fields belong to notebook/folder pages through `indexContent`.
- Keep any temporary fallback only if needed for a safe rollout window, and schedule its removal in 10.18.

**Tests:**
- Existing `title_pattern` tests rewritten so setup stores frontmatter on notebook/folder `indexContent`, not on index notes.
- Existing question-generation instruction tests rewritten the same way.
- Regression test: a note renamed `index_to_be_deleted` does not affect scoped configuration.

### 10.16 — Behavior: notebook and folder index editors save directly to `indexContent`

**Why now:** Once reads and scoped configuration use container content, the save path can become the simpler product model.

**Pre-condition:** User opens a notebook page or folder page.

**Trigger:** User edits the index editor and saves.

**Post-condition:** The save updates the notebook/folder `indexContent` field directly. No note titled `index` is created, updated, renamed, moved, or selected as part of saving container index content.

**Scope:**
- Replace notebook/folder index save calls that create or update notes with focused notebook/folder index-content update APIs or existing container update APIs if they fit cleanly.
- Simplify frontend state: the editor needs container id + content, not `indexNoteId`, note route state, or lazy note creation logic.
- Update save conflict/error handling to match the chosen container update contract.
- Update E2E coverage for notebook and folder index editing so it asserts the final behavior: edit, save, reload, content remains, and no `index` note is produced as a user-visible artifact.

**Tests:**
- Backend API tests for notebook and folder `indexContent` update.
- Frontend component/page tests for notebook and folder save flows without `indexNoteId`.
- Targeted E2E feature update for notebook/folder index editing.

### 10.17 — Behavior: `index` is a reserved note title

**Why now:** After migration and direct container saves, users should not be able to recreate the old index-note convention accidentally.

**Pre-condition:** User creates a note, renames a note, imports a note, or otherwise submits a note title.

**Trigger:** The submitted title is exactly `index` using the product's normal title normalization rules.

**Post-condition:** The operation is rejected with a clear validation message; existing migrated `index_to_be_deleted` notes remain editable under that non-reserved title.

**Scope:**
- Enforce the reserved title in backend note creation and title update paths, including APIs used by sidebar creation and editor title saves.
- Decide and document whether matching is case-insensitive; prefer case-insensitive if title uniqueness/search already normalizes by lower-case.
- Surface the validation error in the frontend create/rename flows without leaving optimistic UI state behind.
- Include import/bulk paths if they share note creation; otherwise document them as a follow-up before closeout.

**Tests:**
- Backend controller/service tests for create and rename rejection.
- Frontend tests for visible create/rename error handling.
- E2E update if the current note creation feature already covers validation errors; otherwise keep this covered by backend/frontend tests.

### 10.18 — Structure: remove remaining index-note infrastructure and close Phase 10

**Why last:** This cleanup is only safe after data is migrated, reads use `indexContent`, saves no longer create notes, and `index` is reserved.

**Scope:**
- Remove normal-read usage of `notebook.index_note_id`, `folder.index_note_id`, scoped index note services, reconciliation hooks, and search exclusions that only existed for designated index notes.
- Drop or deprecate index pointer columns in the safest migration style for the project; if immediate dropping is too risky, mark the follow-up explicitly in the parent plan.
- Remove frontend state and generated API fields that only supported index-note ids.
- Update `ongoing/doughnut_wiki_migration_plan.md` Phase 10 status when complete.
- Record any remaining operational cleanup for `index_to_be_deleted` notes, such as a later user-facing delete/archive task.

**Tests:**
- Targeted backend tests for notebook/folder `indexContent` reads, writes, scoped config, and reserved-title validation.
- Targeted frontend/page tests for notebook page, folder page, breadcrumbs, sidebar, and direct saves.
- Targeted E2E features touched by the migration and save-flow changes.

---

## Mapping back to parent Phase 10 scope

| Parent Phase 10 behavior | Sub-phase(s) |
|---|---|
| Notebook index page editable with lazy create | 10.1 |
| `notebook.index_note_id` cached pointer | 10.2 |
| Default search excludes notebook index | 10.3 |
| Shared notebook/folder index handling | 10.4 |
| `folder.index_note_id` + reconciliation hooks | 10.5 |
| Folder page GET API + client regen | 10.6 |
| Shared index editor extraction (notebook) | 10.7 |
| Folder page route, sidebar nav, folder index editing | 10.8 |
| Sidebar scrolls to active folder page | 10.9 |
| Breadcrumb folder links open folder pages | 10.10 |
| Index-only predefined properties | 10.11 |
| `title_pattern` applies to note creation | 10.12 |
| `question_generation_instruction` applies to question generation | 10.13 |
| Legacy index note content migrates to notebook/folder `indexContent` | 10.14 |
| Scoped configuration reads from container `indexContent` | 10.15 |
| Notebook/folder index editors save directly to container `indexContent` | 10.16 |
| `index` is a reserved note title | 10.17 |
| Index-note infrastructure cleanup and plan refresh | 10.18 |

## Stop-safety check per sub-phase

| After… | Main branch state |
|---|---|
| 10.1 | Notebook index editing works; no folder-page work yet |
| 10.2 | Notebook index has a cached pointer; notebook editing still works |
| 10.3 | Default search no longer returns the notebook index |
| 10.4 | Shared index service exists; external behavior unchanged from 10.3 |
| 10.5 | Folder index pointer + hooks; no folder SPA page yet |
| 10.6 | Folder page payload available over HTTP; UI may not consume it |
| 10.7 | Notebook index UX factored into shared component; behavior unchanged |
| 10.8 | Folders have pages with lazy index editing |
| 10.9 | Folder page reveals its folder in the sidebar scroll |
| 10.10 | Breadcrumb folder links use folder pages |
| 10.11 | Index-only properties are visible and saved consistently |
| 10.12 | Scoped title pattern affects new note titles |
| 10.13 | Scoped question instruction affects question generation |
| 10.14 | Existing index text is preserved on notebooks/folders; legacy notes are renamed |
| 10.15 | Scoped title and question config work without canonical index notes |
| 10.16 | Notebook/folder index saves update container content directly |
| 10.17 | Users cannot create or rename notes to `index` |
| 10.18 | Index-note-specific infrastructure and planning notes are cleaned up |

## Commit checklist per sub-phase

1. Add or update tests for the behavior first where practical; for structure slices, keep existing tests green.
2. Run focused checks through Nix, for example:
   - Backend: `CURSOR_DEV=true nix develop -c pnpm backend:test_only`
   - Frontend single file: `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/path/to/TestFile.spec.ts`
   - E2E single feature: `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/path/to.feature`
3. Regenerate frontend API only after backend controller/DTO signature changes: `CURSOR_DEV=true nix develop -c pnpm generateTypeScript`.
4. Keep generated changes scoped to the phase that required the API change.
5. Update this plan as discoveries change the remaining slices; remove obsolete interim notes instead of preserving history.
