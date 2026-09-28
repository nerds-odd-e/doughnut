# Show references on a file page

## Source

- Story: [Show references on a file page](../../seeds/SEED-053-file-page-references.md#story-1)
- **Identity:** SEED-053#story-1

## Goal and scope

A notebook reader on a file's page sees a **References** section listing the
notes whose frontmatter `image:` resolves to that file, and selecting one opens
that note. `image:` is the only file-reference form (owner confirmed during
refinement), resolved literally relative to the note's own folder, so
referrers are always in the file's notebook. Trashed notes are not listed; a
file with no referrers shows no References section.

Excluded (see the seed): new file-reference forms, reference editing or
rewriting, warnings on delete/rename/move/merge, and a stored note-to-file
index.

## Approach

One rule, one owner: **a note references a file when
`NoteFolderAttachment.at(note, image)` returns that file.** This is the same
rule that serves the note's image, so the file page and the note page cannot
disagree. `NoteFolderAttachment` gains the inverse lookup (the notes whose
image is a given file). It sits next to `at` so the relation lives in one
class.

- `NotebookAttachmentRealm` gains `references: NoteTopology[]` (ordered by note
  id, like note-page references), filled by `getAttachmentPage`.
- `AttachmentPage.vue` renders the existing `NoteReferences` component with
  it. `NoteReferences` already hides itself when the list is empty, and its
  `Card`s are router links to the note.
- Absolute (`/…`) and URL `image:` values never match a stored file under the
  literal rule, so they need no special handling.
- Candidates: slice 1 uses the notes in the file's own folder (the existing
  `NoteRepository.findNotesInContainer`). Slice 2 widens this to the available
  notes in the notebook whose content contains the filename, through one new
  repository query (every resolving `image:` path ends with the filename, so
  this is a superset; `%`/`_` in a filename only widen it further). The
  candidates are then filtered by the rule above. This avoids loading every
  note in the notebook for each file page.
- No North Star topic or ADR governs file references; the change stays within
  the file page, its DTO, and `NoteFolderAttachment`.

Expected complexity delta: roughly +40 lines of backend product code (one
query, one lookup method, one DTO field), +3 lines in `AttachmentPage.vue`, and
regenerated API types.

## Decisive premises

| Premise | Observation | Result |
| --- | --- | --- |
| File-page tests are green on trunk | `CURSOR_DEV=true nix develop -c pnpm backend:test_only --tests 'com.odde.donut.controllers.NotebookAttachmentControllerTest'`; `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/pages/AttachmentPage.spec.ts` | BUILD SUCCESSFUL, 14 tests, 0 failures; 5 tests passed |
| The file page payload has no references and is built in one place | `NotebookAttachmentController.getAttachmentPage` → `NotebookAttachmentRealm.of(chrome, attachment, size)` | Confirmed; the only constructor call |
| `image:` resolution is literal and folder-relative | `NoteFolderAttachment.at` builds `folderPath(note.folder) + path`, matches filename and directory exactly, within `note.getNotebook()` | Confirmed; used by `NoteImageController.showNoteImage` and `MovedNoteImage` |
| No existing query finds notes referencing a file | Explore search of backend services and repositories | Only `MovedNoteImage.anotherNoteNames`, same folder, plain filename |
| `findNotesInContainer` does not skip trashed notes; `Note.JPA_AVAILABLE` is `n.trashedInDatabase = false` | `NoteRepository.java:92-99`, `Note.java:34` | Confirmed; slice 1 lists trashed same-folder notes until slice 2 |
| `NoteReferences.vue` hides itself when empty and cards link to the note | `NoteReferences.vue:2` (`v-if="noteTopologies.length > 0"`), `Card.vue:8` (`router-link :to="noteShowLocation"`) | Confirmed |
| `Note.getNoteTopology()` needs only id, title, timestamps and a non-null notebook | `Note.java:202-210` | Confirmed; notes loaded by a notebook query qualify |
| E2E can seed a note with `image:` and a notebook file | `NotesTestData.java:88-89` writes `Image Url` via `NoteContentMarkdown.withNoteImage`; `notebook_files.feature` has "the notebook {string} has files:" | Confirmed |
| E2E References and note-page assertion steps exist | `e2e_test/step_definitions/wiki_link.ts:126-138`; "I should still be on the note page for {string}" | Confirmed; no step yet clicks a reference card |

## Proof entry points

- Backend: `NotebookAttachmentControllerTest.FilePage` (`controller.getAttachmentPage`), with `makeMe.aNote().folder(…).content("---\nimage: …\n---\n")`, `.trashed()`, and `makeMe.anAttachment(…).in(folder)`.
- Frontend: `frontend/tests/pages/AttachmentPage.spec.ts`; `mountPage` gains a `references` option, defaulting to `[]`.
- E2E: a new scenario in `e2e_test/features/notebooks/notebook_files.feature`.

## Slices

### 1. A file page lists the notes in its folder that show it as their image, and opens them
Type: Behavior
Status: planned
Proof: new E2E scenario in `notebook_files.feature`; `NotebookAttachmentControllerTest.FilePage` and `AttachmentPage.spec.ts` extended; API regenerated; frontend typecheck per the `frontend` skill.

Behavior:
- Notebook "Lab Notebook" has the file `docs/diagram.png` and notes `Design` and
  `Review` in folder `docs` with `Image Url` `diagram.png`. The reader opens the
  file `diagram.png` in sidebar folder path `docs` → "I should see "Design" in
  the References section" and the same for "Review". The reader selects `Design`
  there (new step "I open {string} from the References section") → "I should
  still be on the note page for "Design"".
- Controller: a note in `docs` with `image: diagram.png` is in `references`,
  and one with `image: other.png` is not. A file no note names has empty
  `references`.
- Frontend: with two references, the References heading and both titles are
  shown; with none, there is no References heading.
- Implementation: the `NoteFolderAttachment` inverse lookup over
  `findNotesInContainer(file's notebook, file's folder)`. Interim: trashed notes
  in the folder are listed, and notes in other folders are not; slice 2 removes
  both limits.

### 2. References follow the folder-relative path across the notebook and skip trashed notes
Type: Behavior
Status: planned
Proof: `NotebookAttachmentControllerTest.FilePage` extended; slice 1's E2E scenario and `AttachmentPage.spec.ts` stay green.

Behavior:
- `Overview` at the notebook root has `image: docs/diagram.png` → it is in the
  references of `docs/diagram.png`.
- `docs/diagram.png` and `archive/diagram.png` both exist and only
  `docs/Design` has `image: diagram.png` → `docs/diagram.png` lists `Design`,
  and `archive/diagram.png` has empty `references`.
- A trashed note in `docs` with `image: diagram.png` is not listed.
- Implementation: replace the candidate source with one `NoteRepository` query
  for available notes in the notebook whose content contains the filename
  (`Note.JPA_AVAILABLE`, ordered by id), keeping the same filter.

## Promise → proof

| Promise | Slice | Proof |
| --- | --- | --- |
| A referencing note is listed and opens | 1 | E2E scenario |
| Two referencing notes are both listed | 1 | E2E scenario, controller test |
| No referrers → no References section | 1 | controller empty list + `AttachmentPage.spec.ts` |
| Path is relative to the note's folder, not the file's | 2 | controller test (root `Overview`) |
| Same filename in another folder is not confused | 2 | controller test (`archive/diagram.png`) |
| Trashed notes are not listed | 2 | controller test |

## Considered and excluded

- Reusing `AuthoredNoteReferenceInboundFacade` or `authored_note_reference`:
  those rows index wiki links and note-ID URLs to notes, not `image:`, and
  nothing there resolves to files.
- A stored note-to-file index: not needed for one notebook-scoped query.
- Cross-notebook visibility filtering: `image:` never resolves outside the
  note's notebook, and the page already requires read access to that notebook.
- E2E cases for slice 2: they are edges of one rule and are covered by
  focused controller tests.
