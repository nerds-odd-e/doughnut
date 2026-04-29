# Doughnut Wiki Migration Plan - Phase 3 Sub-Phases

## Purpose

This document decomposes **Phase 3 - Replace Head Note with Optional Index Note** from `ongoing/doughnut_wiki_migration_plan.md` into small, closed sub-phases.

Each sub-phase should be small enough to complete in about 5 minutes and commit independently. Do not start the next sub-phase until the current one is green, cleaned up, and commit-ready.

## Phase 3 Target

After Phase 3:

- the notebook keeps its own name independently of note title/content
- the former head note is represented as a normal root note titled `index` with `note.slug = "index"`
- a notebook may have no `index` note
- a notebook has editable plain-text `shortDetails` for a short notebook settings message
- opening a notebook shows a notebook page with an editor for optional index content
- saving notebook page content creates or updates the normal root `index` note
- first-layer notes no longer need to be children of a head note
- new notebooks are created without pre-created head notes or index notes
- head-note-specific schema, API contracts, route assumptions, and UI affordances are removed or isolated for later legacy cleanup only where still required by Phase 4/5 dependencies

## Key Decisions

- Preserve the former head note ID during migration where feasible. The migrated record becomes the ordinary `index` note so existing stable note identity is not thrown away.
- Do not add a notebook content field. Notebook page body content belongs to the optional root `index` note.
- Store the notebook's short settings message on `Notebook.shortDetails`, not on the optional `index` note.
- Use the current notebook get endpoint and the generated `Notebook` object for notebook page data; do not introduce a separate notebook-page get endpoint or DTO.
- Treat `index` as a normal note after migration: `title = "index"`, `slug = "index"`, `folderId = null`, content copied from the former head note.
- Assume production has no existing user note titled/sluggified as `index`, as stated in the main migration plan.
- Later sub-phases that need the notebook index page or the optional root `index` note load it via the notebook-scoped slug path **`/notebooks/:notebookId/index`** (matching `note.slug = "index"` per the north-star routing rule). Do not add **`indexNoteId`** (or similar) on the notebook API for that purpose.
- `NoteRealm` identifies the notebook boundary with **`notebookId`** only; it does not embed the full `Notebook` object once **3.17** is done—notebook fields come from the notebook get path or store.
- Introduce nullable/optional runtime behavior before removing required head-note assumptions, so deployed code can read both migrated and not-yet-migrated notebooks during rollout.
- Keep tests named by capability, such as notebook page, notebook creation, note navigation, and Obsidian export. Do not create phase-named permanent test files.

## Status

All sub-phases are **planned**.

## Sub-Phases

### 3.1 Characterize Current Notebook Opening Behavior

**Type:** Behavior

Lock down the user-visible behavior before replacing the head note route.

**Commit includes:**

- add or extend an existing notebook/navigation E2E scenario proving that clicking a notebook opens the current notebook entry content
- assert the visible notebook title/content that currently comes through the head note
- keep the scenario capability-named, for example in a notebook navigation or notebook page feature
- no production behavior change

**Verification:**

- `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/<notebook-capability>.feature`

### 3.2 Add Optional Index Note Lookup

**Type:** Structure

Add a small backend capability for finding a notebook's root `index` note without changing observable behavior.

**Commit includes:**

- repository/service method that resolves the root note with `slug = "index"` or title `index` according to the current Phase 2 slug state
- black-box backend test through a high-level service/controller-adjacent path where practical
- no route, DTO, or UI changes yet

**Verification:**

- `CURSOR_DEV=true nix develop -c pnpm backend:test_only --tests "*Notebook*"`

### 3.3 Add Notebook Short Details

**Type:** Behavior

Users can edit a short plain-text notebook message from notebook settings.

**Commit includes:**

- add `shortDetails` to the notebook entity, persistence, API object, and generated frontend type
- notebook settings lets the user edit `shortDetails` as a plain-text short message
- database migration copies existing head note details into `notebook.short_details`, truncated to the field limit
- backend tests cover migration and notebook settings update/read behavior
- frontend test covers editing and saving the short details field from notebook settings
- no new notebook-page get endpoint or notebook-page DTO

**Verification:**

- targeted backend migration/controller tests
- `CURSOR_DEV=true nix develop -c pnpm generateTypeScript`
- targeted frontend notebook settings test

### 3.4 Save Notebook Page Content Into Existing Index Note

**Type:** Behavior

Users can edit notebook page content when an index note already exists.

**Commit includes:**

- backend save/update path for notebook page content updates the existing root `index` note
- controller test proves the notebook name is unchanged and only index content changes
- frontend API client regeneration if needed

**Verification:**

- targeted backend controller tests
- `CURSOR_DEV=true nix develop -c pnpm generateTypeScript` if OpenAPI changes

### 3.5 Save Notebook Page Content Creates Index Note

**Type:** Behavior

Users can save content on a notebook that does not yet have an index note.

**Commit includes:**

- backend save path creates a normal root note with `title = "index"`, `slug = "index"`, and `folderId = null`
- created index note belongs to the notebook and uses the normal note persistence rules
- controller test covers first save and later update using the same note

**Verification:**

- targeted backend controller tests

### 3.6 Notebook Page Component Shows Empty Editor

**Type:** Behavior

The frontend can render the notebook page with an empty editor when the current notebook has no index note.

**Commit includes:**

- notebook page component or page-level view loads the current `Notebook` object through the existing get endpoint
- empty editor renders for missing index note
- component test uses `mockSdkService()` and asserts notebook name plus empty editable body
- no route replacement yet

**Verification:**

- `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/<notebook-page-test>.spec.ts`

### 3.7 Notebook Page Component Saves Content

**Type:** Behavior

The frontend saves notebook page editor content while loading notebook identity through the current `Notebook` object.

**Commit includes:**

- save action calls the notebook page save endpoint
- component test covers:
  - saving when an `index` note already exists (same identity as resolving **`/notebooks/:notebookId/index`**)
  - saving when the notebook has no `index` note yet
- user-visible loading/error handling follows existing `apiCallWithLoading()` patterns where applicable

**Verification:**

- `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/<notebook-page-test>.spec.ts`

### 3.8 Clicking a Notebook Opens the Notebook Page

**Type:** Behavior

Users reach the notebook page directly from notebook lists/cards instead of being routed to the head note.

**Commit includes:**

- notebook list row/card links target the notebook page route
- router has a notebook page path without the `edit` segment
- frontend tests update current route expectations
- E2E scenario from 3.1 is updated to expect the notebook page route and still sees the same migrated content

**Verification:**

- targeted frontend route/component tests
- `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/<notebook-capability>.feature`

### 3.9 Notebook Page Uses Note Show Sidebar Behavior

**Type:** Behavior

The notebook page keeps the navigation/sidebar experience users already get on note pages.

**Commit includes:**

- notebook page uses the same sidebar container or layout behavior as note show
- frontend component test or E2E assertion covers sidebar presence and basic navigation
- no unrelated sidebar refactor

**Verification:**

- targeted frontend component test
- relevant Cypress spec only if the behavior is covered E2E

### 3.10 Remove Edit Notebook Settings Entry Point

**Type:** Behavior

The old "edit notebook settings" path is no longer offered as the normal notebook page entry.

**Commit includes:**

- remove toolbar/menu/buttons that send users to the notebook edit path
- update tests that expected the edit settings affordance
- preserve any truly separate subscription or notebook management behavior that is not notebook page content editing

**Verification:**

- targeted frontend toolbar/menu tests
- relevant Cypress spec if touched

### 3.11 Migrate Existing Head Notes to Index Notes

**Type:** Behavior

Existing notebooks get their former head note converted into the ordinary root `index` note.

**Commit includes:**

- database migration that renames each current head note to `index`
- sets `note.slug = "index"` and `folder_id = null` for the migrated index note
- preserves former head note IDs
- migration test or backend test fixture proving existing notebook content still opens on the notebook page

**Verification:**

- targeted backend migration/controller tests

### 3.12 New Notebooks Do Not Precreate Index Notes

**Type:** Behavior

Creating a notebook creates the notebook boundary only; index content appears only after the user saves notebook page content.

**Commit includes:**

- backend notebook creation stops creating a head note or automatic index note
- notebook creation controller/service tests assert no note is created until content is saved
- frontend creation flow still navigates to the notebook page successfully

**Verification:**

- targeted backend notebook creation tests
- targeted frontend notebook creation tests if the route changes affect the flow

### 3.13 First-Layer Note Creation Uses Notebook Root or Folder

**Type:** Behavior

Creating a top-level note no longer requires attaching it under a head note.

**Commit includes:**

- note creation path for top-level notebook notes writes `folderId = null` and no head-note parent dependency
- tests cover creating a top-level note in a notebook with no index note
- existing child/folder creation behavior remains intact

**Verification:**

- targeted backend note creation tests
- relevant note creation Cypress spec if touched

### 3.14 Moving Notes No Longer Restores Head Notes

**Type:** Behavior

Move behavior stops treating any note as a special head note to be restored or protected.

**Commit includes:**

- remove or bypass head-note restoration behavior from note moves where the notebook page no longer depends on it
- tests cover moving the former index note and moving ordinary root notes without recreating head-note coupling
- preserve current folder alignment behavior from Phase 1/2

**Verification:**

- targeted backend note move tests

### 3.15 Export Uses Optional Root Index Note

**Type:** Behavior

Obsidian export emits `index.md` only when the notebook has an index note.

**Commit includes:**

- export starts from notebook/folders/notes rather than a required head note
- tests cover:
  - notebook with index note exports `index.md`
  - notebook without index note omits `index.md`
  - ordinary notes/folders still export as before under the Phase 2 slug model

**Verification:**

- `CURSOR_DEV=true nix develop -c pnpm backend:test_only --tests "*ObsidianFormat*"`

### 3.16 Remove Head Note From Notebook API Contracts

**Type:** Structure

Permanent API contracts stop exposing `headNoteId` / `headNote` as notebook identity.

**Commit includes:**

- remove head-note fields from notebook DTOs used by frontend/generated clients
- frontend switches remaining notebook links/display logic to notebook ID, notebook name, `shortDetails`, and the **`/notebooks/:notebookId/index`** path where the index note or notebook page body is needed
- regenerate TypeScript client
- update tests and fixtures that build notebooks with `headNoteId` only for routing

**Verification:**

- targeted backend controller tests
- `CURSOR_DEV=true nix develop -c pnpm generateTypeScript`
- targeted frontend tests for notebook cards/lists/routes

### 3.17 NoteRealm Exposes notebookId Instead of Embedded Notebook

**Type:** Structure

Align show-note payloads with the north-star model: the notebook is a **collection boundary** identified by id, while `NoteRealm` stays note-centric (note graph, topology, children, references). The API stops embedding the full `Notebook` on `NoteRealm`; callers resolve notebook name, circle, settings, and index routing via `notebookId` and the existing notebook get endpoint or cached store entry.

**Commit includes:**

- backend DTO / OpenAPI: `NoteRealm` carries `notebookId` matching the note’s notebook; nested `notebook` is removed from the documented contract (use a single rollout step rather than indefinitely returning both shapes)
- every endpoint that returns `NoteRealm` (`showNote`, slug/basename resolution paths, batch note loads, etc.) is updated consistently; controller tests assert `notebookId` and no longer depend on embedded notebook fields for the same assertions
- frontend replaces `noteRealm.notebook` reads with data loaded by `notebookId` (storage refresh, parallel fetch, or route context—follow existing patterns and avoid redundant hot-path fetches where the notebook is already in memory)
- `makeMe.aNoteRealm`, mocks, and generated client types updated; `pnpm generateTypeScript` after OpenAPI change

**Verification:**

- targeted backend controller tests for note-realm payloads
- `CURSOR_DEV=true nix develop -c pnpm generateTypeScript`
- targeted frontend note-show, sidebar, and toolbar tests that previously relied on embedded `notebook` on the realm

**Sequencing:** Prefer landing after **3.16** so slimmed notebook contracts are not duplicated inside every note response; complete before **3.18** if embedded notebook was the last carrier of `headNoteId` in client code paths.

### 3.18 Remove Notebook Head Note Mapping

**Type:** Structure

After all runtime behavior no longer needs it, remove the `notebook_head_note` association from persistence.

**Commit includes:**

- migration drops `notebook_head_note`
- remove `NotebookHeadNote` entity and `Notebook.headNote` mapping
- delete or update factories/builders that create notebooks through head notes
- update backend tests to create notebooks and optional index notes explicitly

**Verification:**

- targeted backend tests for notebook creation, notebook page, note creation, note movement, and export

### 3.19 Clean Up Head-Note Test Language and Fixtures

**Type:** Structure

Remove permanent test and fixture vocabulary that still describes notebooks as having head notes.

**Commit includes:**

- rename E2E setup steps from "notebook with head note" to capability language such as notebook with title/content or notebook with index content
- update test fixture builders to express notebooks and optional index notes directly
- no product behavior change

**Verification:**

- targeted E2E specs touched by renamed steps
- targeted frontend/backend tests touched by fixture renames

### 3.20 Remove Legacy Notebook Edit Route

**Type:** Structure

Delete the obsolete notebook edit route once notebook page content editing is fully served by the notebook page.

**Commit includes:**

- remove `/d/notebooks/:notebookId/edit` route and stale route metadata
- update menu/highlight tests that referenced the edit route
- ensure direct notebook page path is the only normal page-content editing route

**Verification:**

- targeted frontend route/menu tests

### 3.21 Final Head-Note Reference Sweep

**Type:** Structure

Remove remaining head-note references that are no longer justified by Phase 4/5 dependencies.

**Commit includes:**

- search and delete dead head-note services, methods, DTO fields, generated types, mocks, comments, and copy
- keep only references that explicitly belong to old migrations or historical baseline SQL
- update the main migration plan with any discoveries that affect Phase 4

**Verification:**

- targeted backend and frontend tests for touched areas
- `CURSOR_DEV=true nix develop -c pnpm generateTypeScript` if API surface changed

## Completion Criteria

Phase 3 is complete when:

- notebooks can be opened, displayed, and edited without a head note
- notebooks can exist without an index note
- notebooks expose editable `shortDetails`, backfilled from truncated existing head note details
- optional root `index` note identity is reached via **`/notebooks/:notebookId/index`**, not via an `indexNoteId` field on the notebook API; the current `Notebook` object stays the existing get shape without a separate notebook-page get DTO
- show-note and related payloads use **`notebookId` on `NoteRealm`**, not an embedded `notebook`, per **3.17**
- saving notebook page content creates or updates the optional root `index` note
- migrated notebooks preserve their old head-note content through the normal `index` note
- new notebooks are not created with head notes or pre-created index notes
- top-level notes are not children of a head note
- Obsidian export treats `index.md` as optional root note output
- permanent API, frontend route, UI, fixture, and domain vocabulary no longer expose head-note identity
- remaining references to head notes are limited to old migrations or intentionally deferred legacy cleanup documented for later phases
