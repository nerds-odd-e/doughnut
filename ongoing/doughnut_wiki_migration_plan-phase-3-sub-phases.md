# Doughnut Wiki Migration Plan - Phase 3 Sub-Phases

## Purpose

This document decomposes **Phase 3 - Replace Head Note with Optional Index Note** from `ongoing/doughnut_wiki_migration_plan.md` into small, closed sub-phases.

Each sub-phase should be small enough to complete in about 5 minutes and commit independently. Do not start the next sub-phase until the current one is green, cleaned up, and commit-ready.

## Phase 3 Target

After Phase 3:

- the notebook keeps its own name independently of note title/content
- the former head note is represented as a normal root note titled `index` with `note.slug = "index"`
- a notebook may have no `index` note
- a notebook has editable plain-text `description` for a short notebook settings message
- opening a notebook shows a notebook page without head-note identity (name, `description`, navigation); **3.19** refines how the notebook page treats the optional root `index` note (loading UX and a link to edit it on the normal note route); integrated notebook-page body editor (**formerly optional 3.19–3.22**) is **skipped**—users edit `index` on **`/notebooks/:notebookId/index`** after data migration (**3.8**)
- after production head-note migration (**3.8**), the root `index` note is an ordinary note; notebook page chrome does not treat it as a head note
- first-layer notes no longer need to be children of a head note
- new notebooks are created without pre-created head notes or index notes
- head-note-specific schema, API contracts, route assumptions, and UI affordances are removed or isolated for later legacy cleanup only where still required by Phase 4/5 dependencies

## Key Decisions

- Preserve the former head note ID during migration where feasible. The migrated record becomes the ordinary `index` note so existing stable note identity is not thrown away.
- Do not add a notebook content field. Notebook page body content belongs to the optional root `index` note.
- Store the notebook's short settings message on `Notebook.description`, not on the optional `index` note.
- Use the current notebook get endpoint and the generated `Notebook` object for notebook page data; do not introduce a separate notebook-page get endpoint or DTO.
- Treat `index` as a normal note after migration: `title = "index"`, `slug = "index"`, `folderId = null`, content copied from the former head note.
- Assume production has no existing user note titled/sluggified as `index`, as stated in the main migration plan.
- Later sub-phases that need the notebook index page or the optional root `index` note load it via the notebook-scoped slug path **`/notebooks/:notebookId/index`** (matching `note.slug = "index"` per the north-star routing rule). Do not add **`indexNoteId`** (or similar) on the notebook API for that purpose.
- `NoteRealm` identifies the notebook boundary with **`notebookId`** only; it does not embed the full `Notebook` object once **3.14** is done—notebook fields come from the notebook get path or store.
- Integrated notebook-page body editing (save into `index` from the notebook page without opening note show) was planned as optional **3.19–3.22** and is **skipped**; **3.19** instead adds correct loading assumptions for optional index content and an edit affordance to the index note page.
- Introduce nullable/optional runtime behavior before removing required head-note assumptions, so deployed code can read both migrated and not-yet-migrated notebooks during rollout.
- Keep tests named by capability, such as notebook page, notebook creation, note navigation, and Obsidian export. Do not create phase-named permanent test files.

## Status

- **3.18** (final head-note reference sweep): **complete**. Runtime code and generated clients contain no `headNote` / `head_note` surface; literal wire keys `headNoteId` / `headNote` appear only in tests that assert those keys are absent from JSON. Historical SQL in Flyway migrations and the baseline schema unchanged.

All other sub-phases are **planned**.

### Discoveries from 3.18 (for Phase 4)

- No dead services, DTO fields, or generated types remained to remove; the sweep was vocabulary and test naming plus [ongoing/obsidian_sync.md](ongoing/obsidian_sync.md) terminology.
- Keep JSON key assertions in controller tests: they guard against accidentally reintroducing removed notebook identity fields on the wire.

## Sub-Phases

### 3.1 Characterize Current Notebook Opening Behavior

**Type:** Behavior

Lock down the user-visible behavior before replacing the head note route.

**Commit includes:**

- add or extend an existing notebook/navigation E2E scenario proving that clicking a notebook opens the current notebook entry content
- assert the visible notebook name/content that currently comes through the head note
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

### 3.3 Add Notebook Description

**Type:** Behavior

Users can edit a short plain-text notebook message from notebook settings.

**Commit includes:**

- add `description` to the notebook entity, persistence, API object, and generated frontend type
- notebook settings lets the user edit `description` as a plain-text short message
- database migration copies existing head note details into `notebook.description`, truncated to the field limit
- backend tests cover migration and notebook settings update/read behavior
- frontend test covers editing and saving the description field from notebook settings
- no new notebook-page get endpoint or notebook-page DTO

**Verification:**

- targeted backend migration/controller tests
- `CURSOR_DEV=true nix develop -c pnpm generateTypeScript`
- targeted frontend notebook settings test

### 3.4 Remove Head-Note Semantics From the Notebook Page

**Type:** Behavior

The notebook page no longer depends on or surfaces head-note identity: no head-note title, body, or routing as the notebook’s “main” note.

**Commit includes:**

- remove `headNoteId` / `headNote` usage from notebook page layout, loaders, and copy where that page is the entry surface
- notebook page reads notebook name and `description` from the `Notebook` object and normal navigation; link or navigate to the optional root `index` note only via the normal note path **`/notebooks/:notebookId/index`** when showing index content, not via head-note coupling
- tests updated so the notebook page scenario does not assert head-note-specific UI or data shapes
- no requirement in this sub-phase for an embedded notebook-page body editor or a notebook-page save endpoint (skipped former optional integrated editor; see **3.19** for index UX on the notebook page)

**Verification:**

- targeted frontend notebook page tests
- relevant Cypress spec if touched

### 3.5 Clicking a Notebook Opens the Notebook Page

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

### 3.6 Notebook Page Uses Note Show Sidebar Behavior

**Type:** Behavior

The notebook page keeps the navigation/sidebar experience users already get on note pages.

**Commit includes:**

- notebook page uses the same sidebar container or layout behavior as note show
- frontend component test or E2E assertion covers sidebar presence and basic navigation
- no unrelated sidebar refactor

**Verification:**

- targeted frontend component test
- relevant Cypress spec only if the behavior is covered E2E

### 3.7 Remove Edit Notebook Settings Entry Point

**Type:** Behavior

The old "edit notebook settings" path is no longer offered as the normal notebook page entry.

**Commit includes:**

- remove toolbar/menu/buttons that send users to the notebook edit path
- update tests that expected the edit settings affordance
- preserve any truly separate subscription or notebook management behavior that is not notebook page content editing

**Verification:**

- targeted frontend toolbar/menu tests
- relevant Cypress spec if touched

### 3.8 Migrate Existing Head Notes to Index Notes

**Type:** Behavior

Existing notebooks get their former head note converted into the ordinary root `index` note.

**Commit includes:**

- database migration that renames each current head note to `index`
- sets `note.slug = "index"` and `folder_id = null` for the migrated index note
- preserves former head note IDs
- migration test or backend test fixture proving existing notebook content still opens on the notebook page

**Verification:**

- targeted backend migration/controller tests

### 3.9 New Notebooks Do Not Precreate Index Notes

**Type:** Behavior

Creating a notebook creates the notebook boundary only; no automatic root `index` note. Index body exists only when the user creates or edits that note like any other note (integrated notebook-page create/save **3.19–3.22** skipped).

**Commit includes:**

- backend notebook creation stops creating a head note or automatic index note
- notebook creation controller/service tests assert no head note or precreated index note
- frontend creation flow still navigates to the notebook page successfully

**Verification:**

- targeted backend notebook creation tests
- targeted frontend notebook creation tests if the route changes affect the flow

### 3.10 First-Layer Note Creation Uses Notebook Root or Folder

**Type:** Behavior

Creating a top-level note no longer requires attaching it under a head note.

**Commit includes:**

- note creation path for top-level notebook notes writes `folderId = null` and no head-note parent dependency
- tests cover creating a top-level note in a notebook with no index note
- existing child/folder creation behavior remains intact

**Verification:**

- targeted backend note creation tests
- relevant note creation Cypress spec if touched

### 3.11 Moving Notes No Longer Restores Head Notes

**Type:** Behavior

Move behavior stops treating any note as a special head note to be restored or protected.

**Commit includes:**

- remove or bypass head-note restoration behavior from note moves where the notebook page no longer depends on it
- tests cover moving the former index note and moving ordinary root notes without recreating head-note coupling
- preserve current folder alignment behavior from Phase 1/2

**Verification:**

- targeted backend note move tests

### 3.12 Export Uses Optional Root Index Note

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

### 3.13 Remove Head Note From Notebook API Contracts

**Type:** Structure

Permanent API contracts stop exposing `headNoteId` / `headNote` as notebook identity.

**Commit includes:**

- remove head-note fields from notebook DTOs used by frontend/generated clients
- frontend switches remaining notebook links/display logic to notebook ID, notebook name, `description`, and the **`/notebooks/:notebookId/index`** path where the index note or notebook page body is needed
- regenerate TypeScript client
- update tests and fixtures that build notebooks with `headNoteId` only for routing

**Verification:**

- targeted backend controller tests
- `CURSOR_DEV=true nix develop -c pnpm generateTypeScript`
- targeted frontend tests for notebook cards/lists/routes

### 3.14 NoteRealm Exposes notebookId Instead of Embedded Notebook

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

**Sequencing:** Prefer landing after **3.13** so slimmed notebook contracts are not duplicated inside every note response; complete before **3.15** if embedded notebook was the last carrier of `headNoteId` in client code paths.

### 3.15 Remove Notebook Head Note Mapping

**Type:** Structure

After all runtime behavior no longer needs it, remove the `notebook_head_note` association from persistence.

**Commit includes:**

- migration drops `notebook_head_note`
- remove `NotebookHeadNote` entity and `Notebook.headNote` mapping
- delete or update factories/builders that create notebooks through head notes
- update backend tests to create notebooks and optional index notes explicitly

**Verification:**

- targeted backend tests for notebook creation, notebook page, note creation, note movement, and export

### 3.16 Clean Up Head-Note Test Language and Fixtures

**Type:** Structure

Remove permanent test and fixture vocabulary that still describes notebooks as having head notes.

**Commit includes:**

- rename E2E setup steps from "notebook with head note" to capability language such as notebook with title/content or notebook with index content
- update test fixture builders to express notebooks and optional index notes directly
- no product behavior change

**Verification:**

- targeted E2E specs touched by renamed steps
- targeted frontend/backend tests touched by fixture renames

### 3.17 Remove Legacy Notebook Edit Route

**Type:** Structure

Delete the obsolete notebook edit route when nothing in the shipped product path needs it for notebook body content (index is edited on the normal note route; **3.19** links there from the notebook page).

**Commit includes:**

- remove `/d/notebooks/:notebookId/edit` route and stale route metadata
- update menu/highlight tests that referenced the edit route
- ensure settings and index body are reachable without the legacy edit path

**Verification:**

- targeted frontend route/menu tests

### 3.18 Final Head-Note Reference Sweep

**Type:** Structure

**Status:** Complete.

Remove remaining head-note references that are no longer justified by Phase 4/5 dependencies.

**Commit includes:**

- search and delete dead head-note services, methods, DTO fields, generated types, mocks, comments, and copy
- keep only references that explicitly belong to old migrations or historical baseline SQL
- update the main migration plan with any discoveries that affect Phase 4

**Verification:**

- targeted backend and frontend tests for touched areas
- `CURSOR_DEV=true nix develop -c pnpm generateTypeScript` if API surface changed

### 3.19 Notebook Page Index Loading and Edit Entry Point

**Type:** Behavior

The notebook page reflects the **optional** root `index` note honestly while async loads are in flight, and gives a clear path to edit that note on the normal note route.

**Commit includes:**

- do **not** assume the index note (or index page) is absent **before** the relevant async resolution returns; avoid UI that implies “no index” or an incorrect empty index state while loading when an index may exist once the call completes (use explicit loading / neutral chrome, or preserve prior resolved state per existing app patterns—minimal code, no spurious flash)
- in the notebook page region that shows **index** details (summary, excerpt, or whatever the page already surfaces for optional index content), add an **edit** icon in the **upper right** of that index-details block; the control navigates to the index note on **`/notebooks/:notebookId/index`** (same slug route as the north star)
- component test and/or Cypress scenario asserts stable loading behavior where an index exists, and that the edit control reaches the index note route

**Verification:**

- targeted frontend notebook page tests
- relevant Cypress spec if touched

## Skipped sub-phases (formerly optional 3.19–3.22)

The following integrated notebook-page body editor work is **not** pursued: saving index body from the notebook page without opening the note route, dedicated notebook-page save API, and empty embedded editor for notebooks without an index. Users rely on sub-phase **3.19** (index loading and edit entry point) plus normal navigation to **`/notebooks/:notebookId/index`** for editing the optional `index` note.

## Completion Criteria

Phase 3 is complete when:

- notebooks can be opened, displayed, and edited without a head note
- notebooks can exist without an index note
- notebooks expose editable `description`, backfilled from truncated existing head note details
- optional root `index` note identity is reached via **`/notebooks/:notebookId/index`**, not via an `indexNoteId` field on the notebook API; the current `Notebook` object stays the existing get shape without a separate notebook-page get DTO
- show-note and related payloads use **`notebookId` on `NoteRealm`**, not an embedded `notebook`, per **3.14**
- the optional root `index` note is created and updated like any other note on the normal note path; **3.19** improves notebook-page handling of optional index presence and links to that note for editing; integrated notebook-page save (**skipped** former optional **3.19–3.22**) is out of scope
- migrated notebooks preserve their old head-note content through the normal `index` note
- new notebooks are not created with head notes or pre-created index notes
- top-level notes are not children of a head note
- Obsidian export treats `index.md` as optional root note output
- permanent API, frontend route, UI, fixture, and domain vocabulary no longer expose head-note identity
- remaining references to head notes are limited to old migrations or intentionally deferred legacy cleanup documented for later phases
