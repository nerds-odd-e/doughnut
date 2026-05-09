# Doughnut Wiki Phase 10 — sub-phases plan

**Parent plan:** `ongoing/doughnut_wiki_migration_plan.md` — Phase 10 "Index-scoped configuration (notebook root + folders)".

**Goal of this plan:** break Phase 10 into small, stop-safe slices. Each behavior slice delivers one observable improvement around index notes, folder pages, scoped defaults, search, breadcrumbs, or sidebar navigation. Structure slices are only included where they directly enable the next behavior slice.

## Guiding principles for these sub-phases

- **Stop-safe:** after any sub-phase, the app remains shippable. Existing note editing, folder listing, and notebook navigation do not regress.
- **Behavior first:** favor user-visible slices (editable notebook index, folder page, search filtering) over broad backend restructuring.
- **One source of truth:** scoped defaults live in index-note frontmatter. Persisted `index_note_id` fields are pointers/caches, not duplicate config blobs.
- **JPA for pointers in code:** map notebook/folder index pointers with entity associations (for example `Notebook` / `Folder` to `Note`) and exercise them through normal persistence and test fixtures (e.g. builders that set the association). Avoid ad-hoc `UPDATE … SET index_note_id` in application or test code; reserve SQL for Flyway migrations and documented one-off maintenance.
- **One editor pipeline:** notebook page, folder page, and `/d/n/:noteId` use the same Markdown/frontmatter save path wherever practical.
- **Route owns mode:** pages are either a container page (notebook or folder) or a note page. There is no "active folder + active note" product state.
- **Tests by capability:** name tests/features after behaviors (index editing, folder navigation, search), not sub-phase numbers.

---

## Central design decisions

### Scoped index identity

Notebook and folder index notes are ordinary notes titled `index` by convention. The product resolves hot reads by cached pointer:

```text
notebook.index_note_id -> note.id
folder.index_note_id   -> note.id
```

Title-based lookup remains useful for backfill, import, repair, and compatibility with the existing notebook index behavior, but it should not be the primary read path once a pointer exists.

In backend Java and tests, treat these pointers as JPA associations to `Note` (not as columns you set only via raw SQL).

### Frontmatter carries scoped configuration

The first index-only predefined properties are:

```yaml
titlePattern: "{{date}}"
questionGenerationInstruction: "Focus on definitions; avoid trick wording."
```

These are visible/editable only for designated index notes: notebook page, folder page, and the index note's own `/d/n/:noteId` page.

### Likely implementation touchpoints

- Notebook index page: `frontend/src/pages/NotebookPage.vue`, `frontend/src/pages/NotebookPageView.vue`, `backend/src/main/java/com/odde/doughnut/controllers/dto/NotebookPageClientView.java`, `backend/src/main/java/com/odde/doughnut/services/NotebookCatalogService.java`, `NotebookService`.
- Editor/frontmatter: `frontend/src/components/form/RichMarkdownEditor.vue`, `RichFrontmatterProperties.vue`, `frontend/src/utils/noteContentFrontmatter`.
- Folder model/API: `backend/src/main/java/com/odde/doughnut/entities/Folder.java`, `NotebookController`, `FolderListing`, `NotebookFolderIndexRow`, folder controller tests.
- Sidebar/routes: `frontend/src/components/notes/SidebarInner.vue`, `FolderSelector.vue`, `NoteSidebarToolbar.vue`, `frontend/src/composables/useCurrentNoteSidebarState.ts`.
- Breadcrumbs: `frontend/src/components/toolbars/BreadcrumbWithCircle.vue`, `frontend/src/components/notes/NoteShow.vue`.
- Search exclusion: `backend/src/main/java/com/odde/doughnut/controllers/SearchController.java`, `services/search/NoteSearchService.java`, `NoteRepository`, `NoteEmbeddingJdbcRepository`, frontend search components if result fixtures change.

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

### 10.5 — Behavior: folder page opens from sidebar and lazily edits its index

**Why now:** This is the core new Phase 10 navigation behavior: folders become pages, not only filters.

**Pre-condition:** User has a notebook with at least one folder.

**Trigger:** User clicks the folder in the sidebar.

**Post-condition:** App navigates to the folder page, clears active note mode, shows an index editor, and saves to that folder's `index` note (creating it if missing).

**Scope:**
- Add nullable `folder.index_note_id` FK and API fields needed by the folder page.
- Add a folder page route and page/component that embeds the shared index editor.
- Sidebar folder click navigates to the folder page instead of only selecting active folder.
- Preserve existing folder listing/organize behavior.

**Tests:**
- Backend tests: folder API returns/maintains `indexNoteId`; creating a folder index sets `folderId` to the folder.
- Frontend test: sidebar click routes to folder page and does not leave a note page active.
- E2E scenario in a folder/navigation capability feature: click folder, edit folder index, reload/open folder page, body remains.

### 10.6 — Behavior: folder page auto-scrolls the sidebar to the active folder

**Why now:** It is a small user-visible behavior that depends on the folder page route from 10.5.

**Pre-condition:** User opens a deeply nested folder page whose sidebar row is outside the visible scroll area.

**Trigger:** Folder page route loads.

**Post-condition:** The sidebar tree scrolls so the active folder row is visible.

**Scope:**
- Add `scrollIntoView`/equivalent behavior keyed by the active folder route.
- Avoid changing note-page scroll behavior unless the current implementation already uses the same active-folder marker.

**Tests:**
- Frontend component test for invoking scroll behavior on active folder row if practical.
- Manual/targeted E2E only if reliable scroll assertions already exist; otherwise keep unit/component coverage and record the manual verification in the phase notes.

### 10.7 — Behavior: breadcrumb folder links navigate to folder pages

**Why now:** Breadcrumbs should match the new folder-page route before more index-specific properties are added.

**Pre-condition:** User views a note inside nested folders and sees breadcrumb folder segments.

**Trigger:** User clicks a folder segment in the breadcrumb.

**Post-condition:** App navigates to that folder's folder page.

**Scope:**
- Update breadcrumb link construction for folder ancestors.
- Keep the note title/current note breadcrumb behavior unchanged.

**Tests:**
- Frontend test around breadcrumb link destinations.
- E2E extension if an existing note-navigation feature already clicks breadcrumbs.

### 10.8 — Behavior: index notes show index-only predefined properties everywhere they are edited

**Why now:** Once notebook and folder pages both have index editors, the user should see the first scoped configuration fields consistently.

**Pre-condition:** User opens a notebook index, folder index, or the same index note through `/d/n/:noteId`.

**Trigger:** User edits index properties.

**Post-condition:** UI exposes index-only fields for `titlePattern` and `questionGenerationInstruction`, serializes them into leading YAML frontmatter, and normal notes do not show those predefined fields.

**Scope:**
- Add index-note detection by cached pointer/id where available, with safe fallback during transition.
- Add UI rows or metadata for the two predefined properties.
- Keep arbitrary frontmatter editing behavior unchanged.

**Tests:**
- Frontend editor tests: index note shows the two predefined fields and saves YAML.
- Frontend editor tests: normal note does not show those index-only predefined rows.
- Backend test only if the API needs to expose an explicit "isIndexNote" field.

### 10.9 — Behavior: new notes can use scoped `titlePattern`

**Why now:** This is the first concrete behavior powered by index frontmatter, and it is simpler than AI prompt inheritance.

**Pre-condition:** Notebook or folder index frontmatter contains `titlePattern`.

**Trigger:** User creates a note in that scope.

**Post-condition:** The initial title follows the nearest scoped title pattern; existing explicit-title creation remains explicit.

**Scope:**
- Define inheritance order: folder → parent folders → notebook index.
- Apply the pattern only where the creation flow lacks an explicit user title, or clearly document the chosen overwrite policy.
- Start with a small supported pattern set (e.g. date) rather than a broad template language.

**Tests:**
- Backend/controller or frontend creation-flow test depending on where title defaulting lives.
- E2E: create a note in a configured folder and observe the patterned title.
- Unit tests for pattern rendering edge cases (unknown token, invalid pattern) if rendering is non-trivial.

### 10.10 — Behavior: question generation uses scoped instruction

**Why now:** It delivers the second planned property after the scoped config and inheritance mechanism are proven by `titlePattern`.

**Pre-condition:** Notebook or folder index frontmatter contains `questionGenerationInstruction`.

**Trigger:** User invokes question generation for a note in that scope.

**Post-condition:** The generation request includes the nearest scoped instruction without requiring that instruction on the normal note.

**Scope:**
- Resolve instruction via folder → parent folders → notebook index.
- Wire the instruction into the existing question-generation service prompt/options.
- Do not redesign question generation UI in this slice.

**Tests:**
- Backend service/controller test proving the resolved instruction is included in the generation request.
- Existing AI/question generation tests stay green.

### 10.11 — Behavior: folder index notes are excluded from default search

**Why now:** After folder index pointers exist, search can apply the full Phase 10 exclusion rule.

**Pre-condition:** Notebook contains a folder with a designated index note and normal notes.

**Trigger:** User runs default search for text present in the folder index and normal notes.

**Post-condition:** Folder index note is omitted; normal notes still appear.

**Scope:**
- Extend default search exclusion from notebook index ids to folder index ids.
- Ensure the rule is based on pointers, not title `index` alone, so ordinary non-designated notes are not accidentally hidden.

**Tests:**
- Backend search test for folder index exclusion.
- Existing notebook-index exclusion test from 10.3 stays green.

### 10.12 — Cleanup and Phase 10 closeout

**Why last:** Cleanup is meaningful only after all user-facing Phase 10 behaviors are present.

**Scope:**
- Remove interim title-lookup code paths that are no longer needed for normal reads; keep only import/repair/backfill paths.
- Audit navigation state so container page vs note page is consistent across notebook, folder, and note routes.
- Update `ongoing/doughnut_wiki_migration_plan.md` Phase 10 status when complete.
- Add implementation notes for any deferred follow-up, such as "include index notes" search toggle or richer template syntax.

**Tests:**
- Targeted backend test set for notebook/folder index services and search.
- Targeted frontend/page tests for notebook page, folder page, breadcrumbs, sidebar.
- Targeted E2E features touched by the phase.

---

## Mapping back to parent Phase 10 scope

| Parent Phase 10 behavior | Sub-phase(s) |
|---|---|
| Notebook index page editable with lazy create | 10.1 |
| `notebook.index_note_id` cached pointer | 10.2 |
| Default search excludes notebook index | 10.3 |
| Shared notebook/folder index handling | 10.4 |
| Folder page and `folder.index_note_id` | 10.5 |
| Sidebar scrolls to active folder page | 10.6 |
| Breadcrumb folder links open folder pages | 10.7 |
| Index-only predefined properties | 10.8 |
| `titlePattern` applies to note creation | 10.9 |
| `questionGenerationInstruction` applies to question generation | 10.10 |
| Default search excludes folder index | 10.11 |
| Interim cleanup and plan refresh | 10.12 |

## Stop-safety check per sub-phase

| After… | Main branch state |
|---|---|
| 10.1 | Notebook index editing works; no folder-page work yet |
| 10.2 | Notebook index has a cached pointer; notebook editing still works |
| 10.3 | Default search no longer returns the notebook index |
| 10.4 | Shared index service exists; external behavior unchanged from 10.3 |
| 10.5 | Folders have pages with lazy index editing |
| 10.6 | Folder page reveals its folder in the sidebar |
| 10.7 | Breadcrumb folder links use folder pages |
| 10.8 | Index-only properties are visible and saved consistently |
| 10.9 | Scoped title pattern affects new note titles |
| 10.10 | Scoped question instruction affects question generation |
| 10.11 | Default search excludes all designated index notes |
| 10.12 | Interim code and planning notes are cleaned up |

## Commit checklist per sub-phase

1. Add or update tests for the behavior first where practical; for structure slices, keep existing tests green.
2. Run focused checks through Nix, for example:
   - Backend: `CURSOR_DEV=true nix develop -c pnpm backend:test_only`
   - Frontend single file: `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/path/to/TestFile.spec.ts`
   - E2E single feature: `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/path/to.feature`
3. Regenerate frontend API only after backend controller/DTO signature changes: `CURSOR_DEV=true nix develop -c pnpm generateTypeScript`.
4. Keep generated changes scoped to the phase that required the API change.
5. Update this plan as discoveries change the remaining slices; remove obsolete interim notes instead of preserving history.
