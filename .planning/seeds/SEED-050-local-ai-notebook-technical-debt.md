---
id: SEED-050
status: dormant
planted: 2026-09-27
planted_during: owner filtering of the local AI notebook closing review's not-now findings
trigger_when: selected from the product backlog
scope: large
---

# SEED-050: Pay down the local AI notebook effort's technical debt

## Why This Matters

The closing review of the local AI notebook effort (local AI IDEs, images and
other files in notebooks) left findings that were not urgent on their own.
The owner kept those that grow into technical debt if left: rules written in
more than one place, which each new feature copies again, and failures that
pass silently. The stories below group them by importance, most important
first. Evidence paths are as of `99ebce0587`.

## Story Decomposition

<a id="story-1"></a>

### Local publish validates and applies a proposal once

**Identity:** SEED-050#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

**Goal:** Developers adding a new kind of local publish change extend one
final-state step instead of another branch, so publication stays simple and
correct as the notebook format grows.

**Scope:**

- Publishing notes and folders derives the final note and folder identities
  once and applies them once, the way files already work
  (`projectAttachments` in `NotebookGitProposalAcceptance`). Today
  `NotebookGitProposalPublisher` sorts changes into relocation and residual,
  can apply documents twice (`:147`, `:185`) and checks the tree against the
  projection up to three times.
- Proposed Markdown is validated in one pass before any branching: strict
  format (`NotebookGitProposalMarkdownFormat`, now called from three places),
  authored properties (`AuthoredNoteContent.assertValidForSave`) and the Readme
  type (`NotebookGitProposalTypedPath.requireReadme`).

<a id="story-2"></a>

### Web placement rules live with the server's owners

**Identity:** SEED-050#story-2
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

**Goal:** Every web action that places an entry gets its name, size and
uniqueness answer from one server owner, inside the accepted change, so a
clash is refused clearly and never becomes a constraint error or a silent
duplicate.

**Scope:**

- Creating a relationship note lets the server find or create its child
  folder, through the one name owner and in one accepted change. Today
  `frontend/src/utils/relationshipFolderResolve.ts:24` matches the folder name
  case-sensitively on the client and creates the folder as a separate change.
- The picture upload's free-filename check and Book attach's "no Book yet"
  check run inside the accepted-change operation, under its lock
  (`WebNoteImageUploadService`, `AttachBookService`).
- One "first free name" operation on the name owner replaces the five
  separately assembled versions (`firstFreeFolderName`, `NoteMotionService`,
  `MovedNotePicture`, `BookSourceFilePlacement`, `NumberedNameSelection`).
- The 10 MiB limit is defined once (`PictureFile.java:16`,
  `NotebookGitAttachmentSizeAdmission.java:29`,
  `ValidateMultipartFile.java:22`).

<a id="story-3"></a>

### Notebook content failures are loud

**Identity:** SEED-050#story-3
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

**Goal:** A notebook owner sees an error when content cannot be loaded or
kept, instead of an empty folder, an endless spinner, or notes silently moved
to the root (ADR 0006).

**Scope:**

- `fk_note_folder` becomes `ON DELETE RESTRICT`, like the other folder-content
  keys, so a folder removal that forgets its notes fails loudly
  (`V100000000__baseline.sql:446`).
- The sidebar stops turning a failed listing into an empty folder
  (`SidebarInner.vue`, the `catch` after `applyListing`), and a file's page
  shows a failure instead of loading forever
  (`useNotebookSidebarRouteRealms.ts`).
- A notebook without a Git binding is treated as impossible: the accepted-change
  owner fails loudly, and the optional-binding branches and the picture
  upload's own missing-binding refusal go away (`AcceptedWebChangeService`,
  `WebNoteImageUploadService`).

<a id="story-4"></a>

### The notebook tree has one model in code

**Identity:** SEED-050#story-4
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

**Goal:** Developers reason about folders, the notebook root and a Book's file
through one representation each, so tree rules are not rewritten per feature.

**Scope:**

- Folder ancestry has one method on `Folder`, and the trail, depth, the
  containment check and the Portable path derive from it. Today five places
  walk it by hand (`Folder.isTrashed`, `FolderTrail.fromRootToFolder`,
  `FolderMoveDestinationRules`, `FolderSubtreeOccupancy.folderDepth`,
  `NotebookGitPortablePath.folderPath`).
- The folder listing reads the root and a folder with one container query per
  kind, as the name checks already do (`NotebookFolderQuerySupport`, paired
  root/folder queries in `FolderRepository` and `NotebookAttachmentRepository`).
- A Book's source file is resolved one way. Today `Book.sourceFilePath` is
  matched as a path by `NotebookGitBookSourceFileProtection` but looked up as a
  root filename by `BookSourceFile.java:27`.

<a id="story-5"></a>

### Settle the file and image vocabulary and remove leftovers

**Identity:** SEED-050#story-5
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

**Goal:** Code, API and docs name each concept one way and carry nothing left
over from finished transitions, so new work copies the right names.

**Scope:**

- One name each for Attachment and Image across code and API, with one image
  endpoint family. Today the code also says "file" and "picture", and there are
  `/api/notes/{note}/attachment-image` and
  `/api/notebooks/{n}/attachments/{a}/picture`. Refinement settles with the
  owner whether ADR 0001 adopts "file" and "picture" or the code is renamed.
- Remove the unused `NotebookAttachment.getContent`/`setContent` aliases, name
  `NotebookGitCutoverService` and its commit message after what they do now
  (create or reset a binding), and delete the legacy picture passage in
  `docs/notebook-git-attachments.md` ("Pictures uploaded before pictures became
  notebook files…").
- Remove the pull's final-newline conflict absorber
  (`cli/src/commands/notebook/notebookPullRebase.ts`) once web saves and local
  files encode the final newline the same way, so only Git decides what
  replays.

<a id="story-6"></a>

### One owner edits frontmatter in place

**Identity:** SEED-050#story-6
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

**Goal:** A new note property edit is implemented once, so the web editor and
the server cannot drift in which lines they change.

**Scope:** "Change only the affected lines" is implemented in both
`frontend/src/utils/noteContentInPlaceEdit.ts` and
`backend/.../algorithms/FrontmatterInPlaceEdit.java`, and
`docs/note-content-saving.md` only says they behave the same. Choose one owner,
or make the rule one shared contract with cross-language cases, keeping live
editing as responsive as today.
