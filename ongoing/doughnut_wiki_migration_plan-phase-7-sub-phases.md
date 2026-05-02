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
- note setup and folder setup are distinct; creating a note never implicitly creates folders after the migration.
- public note wire shapes do not expose `parentId`
- frontend behavior does not branch on `note.parentId`
- note creation and movement use notebook/folder placement, not parent-note containment
- graph, restore/delete, export/import, and testability paths do not query note children through `parent_id`
- the `note.parent_id` column and `Note.parent` / `Note.children` mappings are gone
- any remaining semantic parent meaning is represented through links/frontmatter content, not a structural DB edge

## Design Decisions

- **No optional parent shim:** Phase 7 removes the parent field instead of making it nullable-but-unused.
- **Folder placement is the source:** When a note needs placement, use `folderId`; notebook root is represented by no folder.
- **Folders and notes are separate concepts:** From sub-phase 7.3 onward, testability and production flows should create folders explicitly before placing notes in them. A note creation request may reference an existing folder, but must not create folders as a side effect.
- **Keep behavior green:** Most sub-phases are structure phases guarded by existing E2E or focused controller/frontend tests. If a cleanup makes the relevant test fail, revert that cleanup and leave it to a later sub-phase with a smaller production change.
- **Commit-sized rule:** Each sub-phase is planned as one closed commit that can be completed, verified, and committed in about five minutes. If implementation exceeds that, stop and split the sub-phase.

## Sub-Phase 7.1 - Easy Folder-Only E2E Fixture Cleanup

**Type:** Structure.

**Pre-condition:** `note_tree_view.feature` passes with the current fixture data.

**Trigger:** The fixture step `I have a notebook "LeSS training" with a note "LeSS in Action" and notes:` is used with explicit `Folder` values.

**Post-condition:** The easy rows in `e2e_test/features/note_topology/note_tree_view.feature` no longer set unnecessary `Parent Title`; the sidebar tree behavior is unchanged.

**Work:** First run the targeted spec as a baseline. If it fails, do not edit this fixture. If it passes, remove the unnecessary `Parent Title` values/column only where folder placement already describes the desired setup. Leave any row that proves hard to delete for a future sub-phase.

**Verify:** `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/note_topology/note_tree_view.feature`.

**Commit boundary:** One passing E2E fixture cleanup commit, or no code commit if the baseline was red.

## Sub-Phase 7.2 - Remove Obvious Parent Titles from Folder-Backed Fixtures

**Type:** Structure.

**Pre-condition:** Sub-phase 7.1 proves folder-only injection works for at least one easy tree fixture.

**Trigger:** E2E fixtures already provide a `Folder` column and still duplicate the same containment through `Parent Title`.

**Post-condition:** The next small batch of folder-backed E2E fixtures no longer duplicates structural placement through `Parent Title`.

**Work:** Remove `Parent Title` from one capability file at a time where `Folder` is already present, starting with the smallest specs such as note creation or Wikidata setup. If a spec fails after removing parent data, restore that fixture and record it as needing a later production change.

**Verify:** Run the targeted `pnpm cypress run --spec ...` for only the touched feature file.

**Commit boundary:** One feature-file cleanup commit per passing targeted spec.

## Sub-Phase 7.3 - Testability Separates Folder and Note Setup

**Type:** Structure for the next behavior.

**Pre-condition:** Some E2E setup can create notes with `Folder` and no `Parent Title`, but this still risks treating folder paths as something note injection may create implicitly.

**Trigger:** Testability setup needs a note inside a folder.

**Post-condition:** Testability has separate setup for folders and notes. Folder setup creates the folder structure; note setup only creates notes in an existing folder or notebook root. Notes and folders are not created together.

**Work:** Add the smallest explicit folder setup path to testability, then adjust note injection so folder placement means "place this note in an existing folder" rather than "create folders while creating notes." Keep `Parent Title` support temporarily only for fixtures that still need legacy setup.

**Verify:** Focused backend test proving folder setup and note setup are separate, plus the smallest E2E spec from 7.1 if the controller test is not enough to prove UI reachability.

**Commit boundary:** One testability separation commit.

## Sub-Phase 7.4 - Convert Remaining Simple E2E Setup to Folder Paths

**Type:** Structure.

**Pre-condition:** Separate folder setup and note setup are covered.

**Trigger:** E2E fixtures use `Parent Title` only to place notes under a root note or folder-shaped note.

**Post-condition:** More fixtures create folders explicitly, then place notes in those existing folders; only genuinely legacy semantic-parent scenarios still mention `Parent Title`.

**Work:** Convert one small capability area at a time, such as assessment, recall, search, or notebook deletion. Do not bundle unrelated feature files. Do not introduce new note setup that creates folders implicitly.

**Verify:** Run the targeted `pnpm cypress run --spec ...` for each touched feature file.

**Commit boundary:** One capability-area fixture cleanup commit.

## Sub-Phase 7.5 - Remove Frontend Reads of `note.parentId`

**Type:** Structure.

**Pre-condition:** Folder-first sidebar and note creation behavior are already covered.

**Trigger:** Frontend components inspect `note.parentId`.

**Post-condition:** Frontend code uses `note.noteTopology.folderId`, notebook root state, or existing route/context instead of `note.parentId`; user behavior is unchanged.

**Work:** Replace the smallest frontend parent-id branch first, such as refinement/relationship/root checks or sidebar drag guards. Keep each commit to one component or one cohesive flow.

**Verify:** Focused frontend test for the touched component, or the targeted E2E spec that covers the flow when no focused test exists.

**Commit boundary:** One frontend parent-id removal commit.

## Sub-Phase 7.6 - Remove Unused Parent-Based Frontend API Calls

**Type:** Structure.

**Pre-condition:** Frontend creation flows can create notes at notebook root or inside a folder without a parent note.

**Trigger:** Frontend code still exposes or calls `createNoteUnderParent`.

**Post-condition:** Frontend creation uses notebook-root or existing-folder placement APIs only. Creating a note does not create or infer folders.

**Work:** Remove one unused parent-based store method/call path at a time. If a call path is still used, convert that flow to pass `folderId` or notebook-root context first.

**Verify:** Focused frontend tests for note creation plus the smallest relevant E2E note creation spec.

**Commit boundary:** One frontend API cleanup commit.

## Sub-Phase 7.7 - Remove `parentId` from Public Note Wire Shapes

**Type:** Structure.

**Pre-condition:** Frontend no longer reads `note.parentId`.

**Trigger:** Backend OpenAPI still exposes `parentId` on note DTO/entity responses.

**Post-condition:** Generated TypeScript no longer contains `parentId` on public note read/write shapes.

**Work:** Remove the backend JSON/OpenAPI exposure of `parentId`, regenerate the TypeScript API client, and fix compile errors caused only by the removed field.

**Verify:** Focused backend API/controller test, `CURSOR_DEV=true nix develop -c pnpm generateTypeScript`, and focused frontend type/test command for touched frontend files.

**Commit boundary:** One API shape commit including generated client changes.

## Sub-Phase 7.8 - Replace Parent-Based Note Creation Endpoint

**Type:** Structure.

**Pre-condition:** Clients use notebook-root/folder note creation APIs.

**Trigger:** Backend still has `createNoteUnderParent` or construction code whose required placement input is a parent note.

**Post-condition:** Production note creation no longer requires a parent note and only accepts notebook-root placement or an existing folder. It does not create folders as part of note creation.

**Work:** Remove or narrow the parent-based creation endpoint and simplify construction to notebook/existing-folder placement. Keep temporary testability compatibility separate from production API behavior.

**Verify:** `NoteCreationControllerTests` focused cases and the targeted note creation E2E spec.

**Commit boundary:** One production creation cleanup commit.

## Sub-Phase 7.9 - Convert Note Motion Away from Parent Edges

**Type:** Structure.

**Pre-condition:** Folder-first UI is the supported structural navigation surface.

**Trigger:** Backend motion code updates `Note.parent`, child lists, or parent-derived folders.

**Post-condition:** Note movement updates folder placement/order without parent-note edges.

**Work:** Convert one motion behavior at a time: top-level move, same-folder ordering, then cross-folder placement if currently exposed. Remove `NoteChildContainerFolderService` only after no production path needs parent-to-folder alignment.

**Verify:** Focused `NoteMotionServiceTest` / `NoteControllerMotionTests` plus the targeted sidebar/motion E2E spec if the behavior is exposed there.

**Commit boundary:** One motion behavior commit.

## Sub-Phase 7.10 - Replace Parent-Based Restore/Delete Traversal

**Type:** Structure.

**Pre-condition:** Note containment is folder-based in creation and motion.

**Trigger:** Restore/delete logic traverses descendants with `parent_id`.

**Post-condition:** Restore/delete behavior does not depend on note children. Any folder subtree behavior is expressed through folders, not notes.

**Work:** Convert one traversal path at a time, starting with focused backend tests that describe the observable restore/delete result.

**Verify:** Focused backend controller/service tests for delete/restore.

**Commit boundary:** One restore/delete cleanup commit.

## Sub-Phase 7.11 - Remove Parent-Based Graph Relationships

**Type:** Structure.

**Pre-condition:** Relationship and graph behavior already reads semantic links/cache from Phase 5 and structural peers from folders from Phase 6.

**Trigger:** GraphRAG relationship handlers still add parent/child/sibling context from note parent edges.

**Post-condition:** Graph context uses folder peers and wiki references only; no parent/child relationship handler relies on `Note.parent`.

**Work:** Remove or replace one graph relationship handler at a time, keeping prompt/context assertions at the public GraphRAG service boundary.

**Verify:** Focused `GraphRAGServiceTest`.

**Commit boundary:** One graph relationship cleanup commit.

## Sub-Phase 7.12 - Remove Parent From Test Builders and Testability Fixtures

**Type:** Structure.

**Pre-condition:** Production code no longer requires parent edges.

**Trigger:** Backend builders, E2E testability DTOs, and remaining tests still use `Parent Title` for structural placement.

**Post-condition:** Test setup creates folders separately from notes, then uses notebook/existing-folder placement for structure. Any old semantic-parent fixture is rewritten as frontmatter/link content or deleted if it no longer represents product behavior.

**Work:** Remove remaining easy `Parent Title` paths in one test area at a time. Replace implicit folder creation during note injection with explicit folder setup. Leave import/export or book-layout cases to a dedicated sub-phase if they encode a different capability.

**Verify:** Focused backend/frontend tests and targeted E2E specs for the touched area.

**Commit boundary:** One test-support cleanup commit.

## Sub-Phase 7.13 - Remove Parent From Import/Export Transitional Paths

**Type:** Structure.

**Pre-condition:** General testability and production note placement no longer use parent edges.

**Trigger:** Import/export code still translates nested notes into `parent_id` rather than folders.

**Post-condition:** Import creates folders explicitly, then creates notes in those existing folders; export reads folder placement. Existing observable import/export behavior remains covered.

**Work:** Convert one import/export capability at a time, starting with the smallest notebook import or export feature.

**Verify:** Targeted import/export E2E spec and focused backend tests.

**Commit boundary:** One import/export cleanup commit.

## Sub-Phase 7.14 - Drop `Note.parent` and `Note.children` From Domain Code

**Type:** Structure.

**Pre-condition:** No production or test-support code needs note parent edges.

**Trigger:** `Note` still maps `parent_id` and `children`.

**Post-condition:** The Java entity no longer has `parent`, `children`, `setParentNote`, parent-order helpers, or `getParentId`.

**Work:** Remove the entity fields and helpers, then fix compile errors by deleting now-dead callers rather than adding compatibility shims.

**Verify:** Focused backend test package that compiles the touched services/controllers.

**Commit boundary:** One domain model cleanup commit.

## Sub-Phase 7.15 - Drop `note.parent_id` and Parent Indexes

**Type:** Structure.

**Pre-condition:** The application compiles and tests pass without the `Note.parent` mapping.

**Trigger:** The database still contains `note.parent_id` and any parent-specific indexes/constraints.

**Post-condition:** Persistence has no structural parent-note column.

**Work:** Add a Flyway migration dropping `note.parent_id` and obsolete parent indexes/constraints. Do not modify committed migrations.

**Verify:** Focused backend migration/controller test or `CURSOR_DEV=true nix develop -c pnpm backend:test_only` if no narrower migration check exists.

**Commit boundary:** One schema removal commit.

## Sub-Phase 7.16 - Final Phase 7 Sweep

**Type:** Structure.

**Pre-condition:** Schema, domain, API, frontend, and testability no longer use note parents.

**Trigger:** Search finds remaining parent-note structural terms.

**Post-condition:** No remaining production reference to structural note parents; plan and north-star docs reflect what was learned.

**Work:** Search for `parentId`, `parent_id`, `Parent Title`, `getParent`, `children`, and parent relationship handlers. Remove dead code or explicitly document any non-structural use that belongs outside Phase 7.

**Verify:** Targeted tests touched in this sweep, plus generated API typecheck/tests if API files changed.

**Commit boundary:** One final cleanup/documentation commit.

## Notes for Future Splitting

- If any sub-phase touches more than one observable capability, split it before coding.
- If a fixture cleanup fails because production still requires parent data, restore the fixture and add a narrower production sub-phase immediately before retrying that cleanup.
- Do not remove ignored or hard-to-delete parent setup in the first cleanup. Keep the first commit intentionally small and reversible.
