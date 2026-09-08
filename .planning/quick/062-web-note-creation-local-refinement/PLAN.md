# Web note creation followed by local refinement

## Source and status

[SEED-009, story 13](../../seeds/SEED-009-git-backed-local-notebook-workflow.md#story-13)
— Create a note on the web and continue refining it locally.

Status: complete. All ten leaves delivered.

## Goal and scope

An owner creates an ordinary note on the web, finishes writing, pulls it into
a local Git checkout, edits it, and explicitly publishes onto the same Donut
identity without losing learning data or creating projection drift.

Include title-only creation, valid initial ordinary-note content, matching empty
notebooks, root and existing represented nested folders, immutable accepted
history, subsequent normal web saves, and the existing clean-main CLI workflow.
Creation and Git acceptance are one atomic outcome. Preserve existing files,
private associations, title validation/warnings, and local-work readiness gates.

Exclude new notebooks/folders, unrepresented destinations, README authoring,
restore/deleted-name reuse, relationship notes, external-data-assisted creation,
AI extraction, bulk import, attachments, web delete/rename/move, earlier drift
repair, divergent receipt across additions, multiple local commits, direct Git
transport, and commit batching. Unsupported existing web flows keep their current
behavior and do not silently advance accepted history. No new UI or API shape.

## Ordered execution leaves

### 1. Reuse accepted snapshot persistence without changing web saves
Type: Structure
Status: done
Proof: existing web-save / drift / late-binding-save suite via `pnpm backend:test_only`.
`AcceptedSnapshotPersistence` holds append/write/binding-save in the caller's
transaction.

### 2. Accept the first title-only root note atomically
Type: Behavior
Status: done
Proof: `NotebookGitNoteCreationAtomicControllerTest` (canonical `type: Note`
child of old head; late binding-save rolls back note/creator/binding).

### 3. Accept a root addition without absorbing earlier drift
Type: Behavior
Status: done
Proof: matching append vs unsynchronized create in
`NotebookGitNoteCreationControllerTest`.

### 4. Accept initial ordinary Markdown as the creation content
Type: Behavior
Status: done
Proof: canonical authored file plus reference-row rollback; Wikidata and
relationship creates stay unsynchronized.

### 5. Accept creation in an existing represented folder
Type: Behavior
Status: done
Proof: `NotebookGitNoteCreationFolderControllerTest` (nested without README,
README-only, unrepresented empty folder).

### 6. Publish onto the web-created identity after a web content save
Type: Behavior
Status: done
Proof: `NotebookGitWebCreatedNotePublicationControllerTest`; no product change.
Ancestry refusals: `NotebookGitProposalAncestryControllerTest`.

### 7. Preserve unpublished work when accepted history adds a note
Type: Behavior
Status: done
Proof: `addition` row in `cli/tests/notebookPull.structuralHistory.suite.ts`.

### 8. Receive a title-only web-created note with the installed CLI
Type: Behavior
Status: done
Proof: title-only scenario in
`e2e_test/features/cli/cli_notebook_web_created_note.feature`.

### 9. Receive the completed web text after creation
Type: Behavior
Status: done
Proof: finished-text receipt (extended by leaf 10) in the same feature.

### 10. Publish the locally refined web capture through the installed CLI
Type: Behavior
Status: done
Proof: same feature — local commit/publish after receipt; Donut shows the
refined text. Cypress:
`pnpm cypress run --spec e2e_test/features/cli/cli_notebook_clone.feature,e2e_test/features/cli/cli_notebook_web_created_note.feature`.
