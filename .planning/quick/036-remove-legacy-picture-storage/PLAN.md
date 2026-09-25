# Remove the legacy picture storage

## Source

- Identity: SEED-035#story-18.
- Source: [refined story](../../seeds/SEED-035-ai-workspace-supporting-files.md#story-18)
  (refined 2026-09-25; production check done the same day: every notebook's
  own pictures moved; picture bytes stay in `attachment_blob` for story 21).
- Governing direction: North Star
  [moving and retiring](../../NORTH-STAR.md#moving-and-retiring) (old stores
  go in a release after the move is confirmed in production; v1.3.26 moved,
  confirmed 2026-09-25) and
  [one store for file bytes](../../NORTH-STAR.md#one-store-for-file-bytes).

## Goal and scope

Maintainers keep one way to store and serve note pictures; note saves and
startup stop doing legacy picture work. No new behaviour. Remove the dead
`note.image_id` column and its key, the startup move and its testability
endpoints and E2E scenario, the note-save orphan cleanup and its legacy path
parsing, the `/attachments/images/...` address, the `image` table and
entity, their tests and fixture uses, and the legacy rules in the attachment
documentation.

Excluded (seed): `attachment_blob`, its entity and the picture bytes in it
(story 21 drops the table); the `/attachments/` development proxy and GCP
route; rewriting or clearing `image:` values or body links that still name a
legacy address (two production notes, fixed manually by the owner); history
profiling write-ups in `docs/notebook-publication-profiling.md`.

Assumptions (checked on main `df9b4b65f3`, 2026-09-25):

- `fk_note_image_id` (`note.image_id` → `image.id`) is `ON DELETE CASCADE`
  (baseline). Deleting an `image` row deletes every note naming it. No Java
  code maps `note.image_id`. Production: 357 notes, each naming its own
  picture. So the column and key go first, before any `image` row can be
  removed.
- `image.attachment_blob_id` → `attachment_blob` cascades only from blob to
  image; dropping `image` leaves the blob rows.
- `image.note_id` → `note` is `ON DELETE CASCADE` (V300000325); nothing else
  references `image`.
- Legacy code: `LegacyNotePictureMoveOnStartup`, `LegacyNotePictureMove`,
  `NotebookGitTestabilityController` seed/move endpoints,
  `NoteService.deleteOrphanImagesForPersistedContent` (called from
  `AuthoredNoteDocumentPersistence`, `NoteConstructionService`, and through
  the `NoteReferenceHandling` consumer), `NoteContentMarkdown`'s
  `LeadingFrontmatterImageReference` and `/attachments/images/(\d+)/` pattern
  (used only by the cleanup and the move), `AttachmentController`, `Image`.
- Frontend has no legacy code; its tests use `/attachments/images/...` only as
  an example of an absolute `image:` value used as-is, which stays true.
- Newest migration is `V300000344`; new ones start at `V300000345`.

PFE: nothing to reuse or build; this is deletion. The picture journey that
must keep working is already proved by the existing web-upload and clone
tests and E2E of pictures as notebook files.

## Outside-in proof

| Promise (seed example) | Slice | Observable proof |
| --- | --- | --- |
| A note that still has `note.image_id` survives; no `image` row removal can delete a note (3) | 1 | migration applies on the test DB (backend test run); regenerated `docs/database-erd.md` shows no `note.image_id` and no `image`→`note` cascade line |
| The application starts → no legacy picture move runs (4) | 2 | `LegacyNotePictureMoveOnStartup` and its test are gone; backend tests green; `cli_notebook_lfs.feature` passes without the legacy scenario |
| An owner saves a note → no legacy picture cleanup runs (4) | 3 | cleanup, its call sites and tests are gone; `TextContentControllerUpdateNoteContentTests` and `NotebookGitWebContentSaveAtomicControllerTest` green |
| A note whose `image:` still names a legacy upload shows a broken picture (2) | 4 | the `/attachments/images/...` handler is gone, so the address answers 404; the note and its content are untouched (no code reads it) |
| A moved picture still displays and arrives on clone and pull (1) | 5 | existing picture-as-file tests and E2E stay green after the `image` table and entity are gone |
| An external `https://` `image:` still displays (5) | 5 | existing `NoteShow.spec.ts` case stays green |

## Slices

### 1. No legacy picture row can delete a note
Type: Structure
Status: done
Proof: slice 1 row above. Accepted: `pnpm backend:test:worktree --tests
'com.odde.donut.controllers.TextContentControllerUpdateNoteContentTests'`
migrated the worktree test DB to `300000345` (12 tests pass; no
`note.image_id` in `information_schema`); `DONUT_ERD_SCHEMA=<worktree test
schema> pnpm export:database-erd` removed only the two `image_id` lines.
Learning: in a worktree, regenerate the ERD with `DONUT_ERD_SCHEMA` set to the
worktree test schema (from `.worktree.local.json`); otherwise the exporter
reads `doughnut_development`, which is not migrated with the branch.

Change: `V300000345__drop_note_image_id.sql` drops `fk_note_image_id`, its
index and `note.image_id`. Regenerate `docs/database-erd.md` (database-erd
skill). No Java change.

Enables: slices 3 and 5 remove `image` rows and the table without cascading
into notes. It also removes a live hazard on its own: today, clearing a
moved note's `image:` line and saving makes the cleanup delete that note's
`image` row, and the database cascade then deletes the note. Safe to release
alone.

### 2. Startup no longer moves legacy pictures
Type: Behavior
Status: done
Proof: slice 2 row above. Accepted: `pnpm generateTypeScript`;
`pnpm backend:test:worktree --tests
'com.odde.donut.controllers.NoteControllerUploadNoteImage*'` (main and test
compile, 15 pass); `pnpm cy:run --spec
e2e_test/features/cli/cli_notebook_lfs.feature` 12/12 without the legacy
scenario. The doc keeps the orphan-cleanup and "rows still served" sentences
for slices 3 and 4.
Deferred refactor (owner decision): `NoteImageFileAttachment` was split out of
`WebNoteImageUploadService` only to share it with the move; its sole caller is
now the upload service, so it could be inlined back and deleted. The refactor
agent's file deletion was blocked by the permission classifier, so it was not
done here.
Learning: `pnpm backend:test:worktree` takes exactly one `--tests` pattern.

Behavior: the application starts → no legacy picture move runs, and
testability offers no legacy seeding or move.

Change: delete `LegacyNotePictureMoveOnStartup`, `LegacyNotePictureMove`,
`LegacyNotePictureMoveControllerTest`, the seed/move endpoints and
`SeedLegacyNotePictureRequest` in `NotebookGitTestabilityController`, the
matching methods in `e2e_test/start/testabilityNotebookGit.ts`, the two steps
in `e2e_test/step_definitions/notebook_files.ts`, and the scenario "A picture
uploaded the legacy way arrives in the owner's clone beside its note after the
move" in `e2e_test/features/cli/cli_notebook_lfs.feature`. Regenerate the API
client. In `docs/notebook-git-attachments.md`, replace the move paragraph
(around "Legacy uploaded pictures … are moved into their notebooks") with one
sentence that legacy pictures were moved into their notebooks and the legacy
store is retired.

### 3. Saving a note does no legacy picture work
Type: Behavior
Status: done
Proof: slice 3 row above. Accepted: `pnpm backend:test:worktree --tests
'com.odde.donut.*'` — full backend suite 2,619 tests, 0 failures
(`TextContentControllerUpdateNoteContentTests` 9,
`NotebookGitWebContentSaveAtomicControllerTest` 2, `NoteContentMarkdownTest`
10). `AuthoredNoteDocumentPersistence` and `NoteConstructionService` no longer
depend on `NoteService`. The applied migration `V300000325` keeps a
historical comment naming the removed method.

Behavior: an owner saves a note, from the web or through publication → no
`image` row is looked up or deleted.

Change: delete `NoteService.deleteOrphanImagesForPersistedContent`, its calls
in `AuthoredNoteDocumentPersistence` and `NoteConstructionService`, the
`deleteOrphanImages` consumer of `NoteReferenceHandling`, and
`NoteContentMarkdown.LeadingFrontmatterImageReference` with its legacy
pattern and id extraction (keep whatever frontmatter reading other callers
still use). Delete the orphan tests in `TextContentControllerUpdateNoteContentTests`
and `NoteContentMarkdownTest`, and the `Image`/blob survival assertions in
`NotebookGitWebContentSaveAtomicControllerTest` (keep that test's other
atomicity assertions). Remove the cleanup's sentences from
`docs/notebook-git-attachments.md`.

### 4. The legacy picture address is gone
Type: Behavior
Status: done
Proof: slice 4 row above. Accepted: `pnpm backend:test:worktree --tests
'com.odde.donut.*'` — 2,615 tests, 0 failures; no backend handler maps
`/attachments` any more. `generateTypeScript` changed nothing (the handler was
outside the OpenAPI spec). `Attachment.getResponseEntity` went with it. The
GCP routing doc keeps the `/attachments/*` row (route excluded) but no longer
links the deleted controller. `Attachment` now has only `Image` as a subclass
and no production caller; slice 5 decides its fate with `Image`.

Behavior: a note whose `image:` still names `/attachments/images/<id>/<name>`
→ the address answers 404, so the web shows a broken picture; the note is
unchanged.

Change: delete `AttachmentController` and `AttachmentControllerTests`;
regenerate the API client. Leave the `/attachments/` proxy and GCP route
(excluded). Adjust the `/attachments/images/...` example in
`docs/notebook-git-attachments.md` (absolute values used as-is) to a neutral
absolute path.

### 5. The legacy picture table is gone
Type: Behavior
Status: done
Proof: slice 5 rows above. Accepted: `pnpm backend:test:worktree --tests
'com.odde.donut.*'` — 2,615 tests, 0 failures (`NoteControllerUploadNoteImageTests`
15, incl. `replacingAPictureKeepsItsMask`); worktree schema shows no `image`,
`attachment_blob` present, Flyway at `300000346`; `pnpm cy:run --spec
e2e_test/features/cli/cli_notebook_lfs.feature,e2e_test/features/note_view/note_frontmatter_image.feature`
12/12 and 4/4; `pnpm frontend:test tests/notes/NoteShow.spec.ts` 11/11.
`Attachment` (the mapped superclass of `Image` only) went too, and
`NoteDependentRowsControllerTestBase.DependentCounts` lost its `image` count.
Learning: dependent-row test helpers count tables by raw SQL name, so a
dropped table breaks them only at runtime; grep table-name strings too.

Behavior: after the migration, notes, their learning history, moved picture
files and Books are unchanged; `image` no longer exists; `attachment_blob`
still holds its rows.

Change: `V300000346__drop_image.sql` drops `image`. Delete the `Image`
entity, `ImageBuilder` and `MakeMe.anImage()`. Remove the image fixture lines
from the note-deletion tests (`NoteControllerPermanentDeleteTests`,
`NotebookGitComposedDeletionAdditionControllerTest`,
`NotebookGitComposedRangePublicationAtomicControllerTest`,
`NotebookGitComposedTrashMoveIdentityControllerTest`,
`NotebookGitDeletionPublicationAtomicControllerTest`,
`NotebookGitDeletionPublicationControllerTest`,
`NotebookGitDeletionPublicationRetryControllerTest`,
`NotebookGitDeletionThenRecreationControllerTest`,
`NotebookGitIdempotentPublishControllerTest`); they keep their other
dependents. In `NoteControllerUploadNoteImageTests`, drop the
`legacyImageCount` assertion and rewrite `replacingALegacyPictureKeepsItsMaskAndItsRow`
as "replacing a picture keeps its mask" with an earlier file picture. Remove
`image` from the `NoteService.permanentlyRemove` Javadoc. Regenerate
`docs/database-erd.md`; remove the remaining legacy rules from
`docs/notebook-git-attachments.md`. Update the db-migration skill's "newest
migration" note.

Sizing: the upper end of one slice (about a dozen mechanical test edits),
but one proof loop; splitting by test file would not give a usable result.

## Current decisions

- Drop `note.image_id` before any `image` row or the table can go, and never
  delete `image` rows one by one.
- `attachment_blob` rows stay (story 21).
- The intended release carries all five slices; each slice leaves main
  releasable, so an earlier release is safe.
