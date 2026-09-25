# Fold the picture attach step back into the upload

**Identity:** quick/037-fold-picture-attach-step-into-upload/PLAN.md
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"d045b2a91e1be7b25dfde50574a85c128496baa674574a3a6af4d6cc04d19723"}}
```

## Source

- Kind: bounded retrospective correction; no seed.
- Corrects the execution of SEED-035#story-18 ("Remove the legacy picture
  storage"), plan `.planning/quick/036-remove-legacy-picture-storage/PLAN.md`
  at commit `db601a2e42` (deleted at wrap-up). Reviewed commits on
  `story/036-remove-legacy-picture-storage`: `89e3f8a67e`, `0d57de8778`,
  `be34f48347`, `c9c8c4a17b`, `a90c95cc92`.
- Provenance of the residue: `4dad58408f` (SEED-035#story-5, plan
  `quick/033-move-legacy-note-pictures`) split `NoteImageFileAttachment` out
  of `WebNoteImageUploadService` "between the upload and the coming legacy
  picture move". Story 18 deleted that move in `0d57de8778`.
- Governing direction: North Star
  [moving and retiring](../../NORTH-STAR.md#moving-and-retiring): once a move
  is retired, what existed only to serve it goes too.
- Finding re-verified at `a90c95cc92` (2026-09-25):
  `backend/src/main/java/com/odde/donut/services/notebookGit/NoteImageFileAttachment.java`
  (a `@Service` with one public method, `attach`) has one caller,
  `WebNoteImageUploadService.upload`, and no test or doc names it. Story 18's
  slice 2 refactor pass proposed inlining it and deleting the file. The host
  permission check blocked the file deletion, so the plan records the change
  as deferred for an owner decision.

## Goal and scope

Beneficiary: maintainers of the note picture upload. They read one service
for "an uploaded picture becomes a file beside its note", with no separate
step class left over from a retired move.

Included: move `NoteImageFileAttachment.attach` back into
`WebNoteImageUploadService` as a private step, the shape it had before
`4dad58408f`: the file row is saved, then `image:` is set through the
ordinary content save. Move its three dependencies
(`NotebookAttachmentRepository`, `AuthoredNoteDocumentPersistence`,
`CanonicalDonutOrigin`) into the upload service. Delete
`NoteImageFileAttachment.java`.

Excluded: any change in upload behaviour, refusal rules, commit message or
API; frontend tests that use `/attachments/images/...` as an example of an
absolute `image:` value (the story 18 plan kept them); the `/attachments/`
development proxy and GCP route (SEED-035 story 18 exclusion).

Preserved promises: a web upload stores the bytes first, then accepts the
pointer and the note's `image:` together as one web change; a name that is
not a plain filename, or that is already taken, is refused before anything
is stored; replacing a picture keeps `image_mask:`.

## Outside-in proof

| Promise | Slice | Observable proof |
| --- | --- | --- |
| Uploads, refusals and mask behave as before | 1 | `NoteControllerUploadNoteImageTests` (15 tests at `a90c95cc92`) green, unchanged |
| No leftover step class | 1 | `NoteImageFileAttachment.java` gone; `git grep NoteImageFileAttachment -- backend` finds nothing |

## Slices

### 1. The upload service owns its attach step again
Type: Structure
Status: done
Proof: both rows above. Command: `pnpm backend:test:worktree --tests
'com.odde.donut.controllers.NoteControllerUploadNoteImage*'` (one `--tests`
pattern per run; local MySQL must be running).

Change: inline `attach` into `WebNoteImageUploadService` (private helper or
the edit lambda, whichever reads best), take over its dependencies, and
delete `NoteImageFileAttachment.java`. No test edits are expected.

Sizing: one file inlined and one deleted; well under five minutes.

Accepted proof: `attach` is a private method of `WebNoteImageUploadService`,
called from the edit lambda with an unchanged body; `NoteImageFileAttachment.java`
deleted. `NoteControllerUploadNoteImageTests` 15/15 green with no test edits;
`git grep NoteImageFileAttachment -- backend` empty. The refactor pass found
nothing further. The file deletion was not blocked this time.

## Current decisions

- This correction is optional cleanup, not a defect. The owner may decline
  it; then close it without execution.
- If a file deletion is blocked by the host permission check again, stop and
  ask the owner rather than leave the inlined copy beside the old class.
