# Doughnut Wiki Migration Plan - Phase 7 Sub-Phases

## Parent Phase

Phase 7 of `ongoing/doughnut_wiki_migration_plan.md`: Remove Note Parent (Folders Replace Containment).

## Entry Conditions

- Phase 5 is complete: relationship notes no longer need the parent pointer as their structural source.
- Phase 6 is complete: user-facing containment is folder-first.
- The slug/path retirement boundary before Phase 7 is complete: canonical note URLs are `/d/n/:noteId`, folder identity is by id, and no Phase 7 work preserves slug paths.

If any entry condition is false in the codebase when starting implementation, stop and split the missing boundary work outside this Phase 7 plan.

## Goal

Remove the structural note parent concept from tests, frontend code, backend APIs, backend domain services, and persistence. A note belongs to a notebook and may belong to a folder; it does not have a parent note.

By the end of Phase 7:

- E2E fixtures use folders, not `Parent Title`, when the behavior under test is structural placement.
- **Production** note creation and movement never create folders as a side effect; folders exist as explicit entities first. **Testability** may still expose one cohesive setup (folder tree + notes) in a single call or step, as long as it resolves to explicit folders then places notes—no structural reliance on `Parent Title`.
- public note wire shapes do not expose `parentId`
- frontend behavior does not branch on `note.parentId`
- note creation and movement use notebook/folder placement, not parent-note containment
- graph, restore/delete, export/import, and testability paths do not query note children through `parent_id`
- the `note.parent_id` column and `Note.parent` / `Note.children` mappings are gone
- any remaining semantic parent meaning is represented through links/frontmatter content, not a structural DB edge

## Design Decisions

- **No optional parent shim:** Phase 7 removes the parent field instead of making it nullable-but-unused.
- **Folder placement is the source:** When a note needs placement, use `folderId`; notebook root is represented by no folder.
- **Folders and notes are separate concepts in the product model:** A note references `folderId` (or notebook root); it is not contained by another note. **Production** flows create folders through folder APIs, not as a side effect of creating a note. **Testability** may bundle “ensure this folder path, then place these notes” in one surface for ergonomics, as long as the implementation still materializes folders as real rows and does not reintroduce parent-note containment for structure.
- **Keep behavior green:** Most sub-phases are structure phases guarded by existing E2E or focused controller/frontend tests. If a cleanup makes the relevant test fail, revert that cleanup and leave it to a later sub-phase with a smaller production change.
- **Small-batch rule:** Each sub-phase is scoped so it can be completed and verified in about five minutes. If implementation exceeds that, stop and split the sub-phase. **Git commits** are not tied one-to-one to sub-phases; developers batch commits as they prefer (same idea as the **Sizing Rule** in `ongoing/doughnut_wiki_migration_plan-phase-5-sub-phases.md`).
- **Human-owned git and deploy:** Automated assistants (including IDE agents) must **not** commit, push, open pull requests, or trigger production deploy when a sub-phase or the whole of Phase 7 is finished, unless the maintainer **explicitly** asks for that step. After the sub-phase verification commands pass, stop and hand off; the developer owns version control and CD.

## E2E fixtures: folder path vs note title collisions

Folder placement is expressed by path segments that become folder **names** in the same **notebook** as the fixture notes. If a segment equals another note’s **title** in that setup, the scenario is hard to read and can mask mistakes (or interact badly with any name-based resolution during migration).

**When:** While touching E2E data in sub-phases **7.2**, **7.4**, and **7.14** (and any other fixture edit in Phase 7), actively look for this overlap in the same notebook block: compare each `Folder` path segment to every other row’s note title and to titles introduced in the same scenario (including anchor notes from the surrounding `Given`).

**How to find:** There is no single grep for semantic equality; use feature-wide review. A practical pass is: open each `.feature` you modify, list note titles and folder path segments for one notebook at a time, flag duplicates across the two lists.

**Resolve each collision** (pick the smallest change that keeps the scenario’s intent):

1. **Remove the note** if nothing in the feature depends on that note (no step clicks it, searches for it, or asserts on it).
2. **Rename the note** if the behavior under test needs that note but not that exact title string.
3. **Rename the folder** (change the `Folder` path segment) if the test is about the folder shape or navigation label and the note title must stay as written.

Apply the same rule to **new** fixtures: avoid introducing the same string as both a folder segment and an unrelated note title unless the test explicitly means to assert that edge case.

## Sub-Phase 7.1 - Easy Folder-Only E2E Fixture Cleanup

**Type:** Structure.

**Pre-condition:** `note_tree_view.feature` passes with the current fixture data.

**Trigger:** The fixture step `I have a notebook "LeSS training" with a note "LeSS in Action" and notes:` is used with explicit `Folder` values.

**Post-condition:** The easy rows in `e2e_test/features/note_topology/note_tree_view.feature` no longer set unnecessary `Parent Title`; the sidebar tree behavior is unchanged.

**Work:** First run the targeted spec as a baseline. If it fails, do not edit this fixture. If it passes, remove the unnecessary `Parent Title` values/column only where folder placement already describes the desired setup. Leave any row that proves hard to delete for a future sub-phase. If you add or change `Folder` paths, apply [folder path vs note title collisions](#e2e-fixtures-folder-path-vs-note-title-collisions).

**Verify:** `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/note_topology/note_tree_view.feature`.

## Sub-Phase 7.2 - Remove Obvious Parent Titles from Folder-Backed Fixtures

**Type:** Structure.

**Pre-condition:** Sub-phase 7.1 proves folder-only injection works for at least one easy tree fixture.

**Trigger:** E2E fixtures already provide a `Folder` column and still duplicate the same containment through `Parent Title`.

**Post-condition:** The next small batch of folder-backed E2E fixtures no longer duplicates structural placement through `Parent Title`.

**Work:** Remove `Parent Title` from one capability file at a time where `Folder` is already present, starting with the smallest specs such as note creation or Wikidata setup. If a spec fails after removing parent data, restore that fixture and record it as needing a later production change. Apply [folder path vs note title collisions](#e2e-fixtures-folder-path-vs-note-title-collisions) in the same edit when you touch a file.

**Verify:** Run the targeted `pnpm cypress run --spec ...` for only the touched feature file.

## Sub-Phase 7.3 - Testability: Cohesive Folder + Note Setup Without Production Side Effects

**Type:** Structure for the next behavior.

**Pre-condition:** Some E2E setup can create notes with `Folder` and no `Parent Title`, but it is unclear whether folder path handling is only a testability convenience or leaks into production note creation as implicit folder creation.

**Trigger:** Testability setup needs a note inside a folder (often expressed as a folder path in one fixture).

**Post-condition:** Testability may keep **one** setup path that establishes the folder tree and places notes together (same request, same step, or thin wrapper—team preference). What must **not** happen: production note-create or note-move APIs grow or keep “create missing folders from a path string” behavior. Folder path resolution for tests, if any, runs in testability only and ends with explicit folder ids before notes are persisted. `Parent Title` remains only for fixtures that still need legacy structural parent until later sub-phases remove it.

**Work:** Clarify or adjust testability so cohesive setup is implemented as “ensure folders (explicit entities), then inject notes with `folderId` / path resolved to ids,” not as parent-note simulation. Add a focused backend test only if needed to lock the boundary (testability may create folders while setting up a scenario; production note endpoints do not). Do **not** require callers to split into two unrelated HTTP steps unless that already matches the codebase.

**Verify:** Focused backend test for the production vs testability boundary if the code change is non-obvious; otherwise the smallest E2E spec from 7.1 plus any existing testability tests you touch.

## Sub-Phase 7.4 - Convert Remaining Simple E2E Setup to Folder Paths

**Type:** Structure.

**Pre-condition:** Sub-phase 7.3 clarified cohesive test setup vs production (no implicit folder creation on note create/move).

**Trigger:** E2E fixtures use `Parent Title` only to place notes under a root note or folder-shaped note.

**Post-condition:** More fixtures create folders explicitly, then place notes in those existing folders; only genuinely legacy semantic-parent scenarios still mention `Parent Title`.

**Work:** Convert one small capability area at a time, such as assessment, recall, search, or notebook deletion. Do not bundle unrelated feature files. Do not introduce new note setup that creates folders implicitly. Apply [folder path vs note title collisions](#e2e-fixtures-folder-path-vs-note-title-collisions) so folder segments do not duplicate unrelated note titles in the same notebook.

**Verify:** Run the targeted `pnpm cypress run --spec ...` for each touched feature file.

## Sub-Phase 7.5 - Breadcrumbs from NoteRealm Ancestor Folders

**Type:** Behavior.

**Pre-condition:** Sub-phase 7.3 holds (production note create/move does not implicitly create folders) and folder placement via `folderId` is the structural source for notes that live in folders.

**Trigger:** Breadcrumbs still walk or infer placement from the note parent chain, `parentId`, or ad hoc client topology instead of an explicit folder ancestry supplied with the note realm.

**Post-condition:** `NoteRealm` includes an ordered list of **ancestor folders** (from notebook root toward the note’s folder; empty when the note is at notebook root). Breadcrumb UI renders the trail from that list plus notebook identity, not from structural `note.parentId` for folder placement. OpenAPI and generated TypeScript reflect the new shape.

**Work:** Extend `NoteRealm` with the ancestor-folder list (ids and display names as needed for the UI, aligned with folder naming elsewhere). Populate it on the server from the folder hierarchy for the note’s `folderId`. Update breadcrumb component(s) to consume `noteRealm`’s ancestor list. Run `pnpm generateTypeScript` after OpenAPI changes.

**Verify:** Targeted frontend test for breadcrumb rendering, or the smallest E2E spec that asserts a folder path in the breadcrumb bar; add a focused backend or DTO test if folder-chain population is non-trivial.

## Sub-Phase 7.6 - Remove Frontend Reads of `note.parentId`

**Type:** Structure.

**Pre-condition:** Folder-first sidebar and note creation behavior are already covered; breadcrumbs use ancestor folders from `NoteRealm` (sub-phase 7.5).

**Trigger:** Frontend components inspect `note.parentId`.

**Post-condition:** Frontend code uses `note.noteTopology.folderId`, notebook root state, or existing route/context instead of `note.parentId`; user behavior is unchanged.

**Work:** Replace the smallest frontend parent-id branch first, such as refinement/relationship/root checks or sidebar drag guards. Keep each change to one component or one cohesive flow.

**Verify:** Focused frontend test for the touched component, or the targeted E2E spec that covers the flow when no focused test exists.

## Sub-Phase 7.7 - Remove Unused Parent-Based Frontend API Calls

**Type:** Structure.

**Pre-condition:** Frontend creation flows can create notes at notebook root or inside a folder without a parent note.

**Trigger:** Frontend code still exposes or calls `createNoteUnderParent`.

**Post-condition:** Frontend creation uses notebook-root or existing-folder placement APIs only. Creating a note does not create or infer folders.

**Work:** Remove one unused parent-based store method/call path at a time. If a call path is still used, convert that flow to pass `folderId` or notebook-root context first.

**Verify:** Focused frontend tests for note creation plus the smallest relevant E2E note creation spec.

## Sub-Phase 7.8 - Remove `parentOrSubjectNoteTopology` from `NoteTopology`

**Type:** Structure.

**Pre-condition:** Sub-phase 7.5 holds (breadcrumbs and structural placement consume `NoteRealm` ancestor folders where needed). Any UX that still depended on the nested topology chain—breadcrumb fallback that walks `parentOrSubjectNoteTopology`, relationship-note subject affordances, recall/CLI context, or store keys derived from the nested id—has an explicit replacement (same `NoteRealm` fields, folder placement, links, or a small dedicated DTO surface agreed in earlier phases).

**Trigger:** `NoteTopology` still declares `parentOrSubjectNoteTopology`, OpenAPI still nests it, generated TypeScript or test fixtures still build or read the tree.

**Post-condition:** The field is removed from the Java `NoteTopology` DTO, serializers, and OpenAPI; generated types and `makeMe` / fixture builders no longer set it; no production code walks nested topology for the behaviors above.

**Work:** Remove the field from `NoteTopology` and delete call-site fallbacks that only existed for the parent/subject chain (for example legacy breadcrumb ancestry in `Breadcrumb.vue` when the folder trail is empty). Update `StoredApiCollection`, recall UI (`NoteRefinement.vue` and related), CLI recall context, and any other topology walks to use the replacement shape. Run `pnpm generateTypeScript` after OpenAPI changes.

**Verify:** Focused tests for breadcrumb, recall/refinement, and any touched store paths; `CURSOR_DEV=true nix develop -c pnpm generateTypeScript`; smallest E2E spec if only integration surfaces change.

## Sub-Phase 7.9 - Remove `parentId` from Public Note Wire Shapes

**Type:** Structure.

**Pre-condition:** Frontend no longer reads `note.parentId` for structural placement or breadcrumbs. `NoteTopology` no longer exposes `parentOrSubjectNoteTopology` (sub-phase 7.8).

**Trigger:** Backend OpenAPI still exposes `parentId` on note DTO/entity responses.

**Post-condition:** Generated TypeScript no longer contains `parentId` on public note read/write shapes.

**Work:** Remove the backend JSON/OpenAPI exposure of `parentId`, regenerate the TypeScript API client, and fix compile errors caused only by the removed field.

**Verify:** Focused backend API/controller test, `CURSOR_DEV=true nix develop -c pnpm generateTypeScript`, and focused frontend type/test command for touched frontend files.

## Sub-Phase 7.10 - Replace Parent-Based Note Creation Endpoint

**Type:** Structure.

**Pre-condition:** Clients use notebook-root/folder note creation APIs.

**Trigger:** Backend still has `createNoteUnderParent` or construction code whose required placement input is a parent note.

**Post-condition:** Production note creation no longer requires a parent note and only accepts notebook-root placement or an existing folder. It does not create folders as part of note creation.

**Work:** Remove or narrow the parent-based creation endpoint and simplify construction to notebook/existing-folder placement. Keep temporary testability compatibility separate from production API behavior.

**Verify:** `NoteCreationControllerTests` focused cases and the targeted note creation E2E spec.

## Sub-Phase 7.11 - Convert Note Motion Away from Parent Edges

**Type:** Structure.

**Pre-condition:** Folder-first UI is the supported structural navigation surface.

**Trigger:** Backend motion code updates `Note.parent`, child lists, or parent-derived folders.

**Post-condition:** Note movement updates folder placement/order without parent-note edges.

**Work:** Convert one motion behavior at a time: top-level move, same-folder ordering, then cross-folder placement if currently exposed. Remove `NoteChildContainerFolderService` only after no production path needs parent-to-folder alignment.

**Verify:** Focused `NoteMotionServiceTest` / `NoteControllerMotionTests` plus the targeted sidebar/motion E2E spec if the behavior is exposed there.

## Sub-Phase 7.12 - Replace Parent-Based Restore/Delete Traversal

**Type:** Structure.

**Pre-condition:** Note containment is folder-based in creation and motion.

**Trigger:** Restore/delete logic traverses descendants with `parent_id`.

**Post-condition:** Restore/delete behavior does not depend on note children. Any folder subtree behavior is expressed through folders, not notes.

**Work:** Convert one traversal path at a time, starting with focused backend tests that describe the observable restore/delete result.

**Verify:** Focused backend controller/service tests for delete/restore.

## Sub-Phase 7.13 - Remove Parent-Based Graph Relationships

**Type:** Structure.

**Pre-condition:** Relationship and graph behavior already reads semantic links/cache from Phase 5 (including **5.23**: graph uses wiki-title cache; **`NoteRealm.inboundReferences`** / **`relationshipsDeprecating`** are gone in favor of **`NoteRealm.references`** where applicable) and structural peers from folders from Phase 6.

**Trigger:** GraphRAG relationship handlers still add parent/child/sibling context from note parent edges.

**Post-condition:** Graph context uses folder peers and wiki references only; no parent/child relationship handler relies on `Note.parent`.

**Work:** Remove or replace one graph relationship handler at a time, keeping prompt/context assertions at the public GraphRAG service boundary.

**Verify:** Focused `GraphRAGServiceTest`.

## Sub-Phase 7.14 - Remove Parent From Test Builders and Testability Fixtures

**Type:** Structure.

**Pre-condition:** Production code no longer requires parent edges.

**Trigger:** Backend builders, E2E testability DTOs, and remaining tests still use `Parent Title` for structural placement.

**Post-condition:** Test setup uses folder-first structural placement; cohesive testability setup is allowed where it still resolves to explicit folders before notes. Any old semantic-parent fixture is rewritten as frontmatter/link content or deleted if it no longer represents product behavior.

**Work:** Remove remaining easy `Parent Title` paths in one test area at a time. Where note injection used to create folders implicitly in production, that path must already be gone; testability may keep a single bundled setup. Leave import/export or book-layout cases to a dedicated sub-phase if they encode a different capability. Re-check [folder path vs note title collisions](#e2e-fixtures-folder-path-vs-note-title-collisions) for any fixture you change.

**Verify:** Focused backend/frontend tests and targeted E2E specs for the touched area.

## Sub-Phase 7.15 - Remove Parent From Import/Export Transitional Paths

**Type:** Structure.

**Pre-condition:** General testability and production note placement no longer use parent edges.

**Trigger:** Import/export code still translates nested notes into `parent_id` rather than folders.

**Post-condition:** Import creates folders explicitly, then creates notes in those existing folders; export reads folder placement. Existing observable import/export behavior remains covered.

**Work:** Convert one import/export capability at a time, starting with the smallest notebook import or export feature.

**Verify:** Targeted import/export E2E spec and focused backend tests.

## Sub-Phase 7.16 - Drop `Note.parent` and `Note.children` From Domain Code

**Type:** Structure.

**Pre-condition:** No production or test-support code needs note parent edges.

**Trigger:** `Note` still maps `parent_id` and `children`.

**Post-condition:** The Java entity no longer has `parent`, `children`, `setParentNote`, parent-order helpers, or `getParentId`.

**Work:** Remove the entity fields and helpers, then fix compile errors by deleting now-dead callers rather than adding compatibility shims.

**Verify:** Focused backend test package that compiles the touched services/controllers.

## Sub-Phase 7.17 - Drop `note.parent_id` and Parent Indexes

**Type:** Structure.

**Pre-condition:** The application compiles and tests pass without the `Note.parent` mapping.

**Trigger:** The database still contains `note.parent_id` and any parent-specific indexes/constraints.

**Post-condition:** Persistence has no structural parent-note column.

**Work:** Add a Flyway migration dropping `note.parent_id` and obsolete parent indexes/constraints. Do not modify committed migrations.

**Verify:** Focused backend migration/controller test or `CURSOR_DEV=true nix develop -c pnpm backend:test_only` if no narrower migration check exists.

## Sub-Phase 7.18 - Final Phase 7 Sweep

**Type:** Structure.

**Pre-condition:** Schema, domain, API, frontend, and testability no longer use note parents.

**Trigger:** Search finds remaining parent-note structural terms.

**Post-condition:** No remaining production reference to structural note parents; plan and north-star docs reflect what was learned.

**Work:** Search for `parentId`, `parent_id`, `parentOrSubjectNoteTopology`, `Parent Title`, `getParent`, `children`, and parent relationship handlers. Remove dead code or explicitly document any non-structural use that belongs outside Phase 7.

**Verify:** Targeted tests touched in this sweep, plus generated API typecheck/tests if API files changed.

## Notes for Future Splitting

- Folder segment vs note title overlap in a fixture is a common footgun; treat collision cleanup as part of the same PR when you already have that feature file open (see [E2E fixtures: folder path vs note title collisions](#e2e-fixtures-folder-path-vs-note-title-collisions)).
- If any sub-phase touches more than one observable capability, split it before coding.
- If a fixture cleanup fails because production still requires parent data, restore the fixture and add a narrower production sub-phase immediately before retrying that cleanup.
- Do not remove ignored or hard-to-delete parent setup in the first cleanup. Keep the first change intentionally small and reversible.
