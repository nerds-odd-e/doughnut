# Delete a notebook file on the web

## Source

- Identity: SEED-035#story-2.
- Source: [refined story](../../seeds/SEED-035-ai-workspace-supporting-files.md#story-2)
  (refined 2026-09-25; owner decisions: the purpose is only to remove the
  file; a file a note's `image:` names may be deleted and leaves a broken
  picture; delete outright, no web Trash; backlog order kept).
- Governing direction: North Star
  [one accepted-change boundary](../../NORTH-STAR.md#one-accepted-change-boundary)
  (the deletion is one accepted web change) and
  [moving and retiring](../../NORTH-STAR.md#moving-and-retiring) ("never
  delete an object because its current attachment row disappeared": the LFS
  object stays).

## Goal and scope

An owner deletes a non-Markdown file from its file page on the web, after a
confirmation. One accepted change removes it, so a pull no longer has it.
Notes, other files and learning history are untouched. A file a note's
`image:` names can be deleted (the note keeps its `image:` value and shows a
broken picture). A Book's source file is refused with the local-publish
message.

Excluded (seed): web Trash or restore, deleting LFS objects or rewriting
history, cleaning up old files when a picture is replaced, rewriting
`image:` values, deleting several files at once, a sidebar delete action,
renaming or moving files, Markdown files.

Assumptions (checked on main `ecf5c531a1`, 2026-09-25; rechecked on `a4e27f4a22`, 2026-09-26):

- `NotebookAttachmentController` (`/api/notebooks/{notebook}/attachments/{attachment}`)
  has only the page `GET` and the `…/content` download. `NotebookAttachment.requireInNotebook`
  exists.
- `AcceptedWebChangeService.apply(notebookId, operation, commitMessage, updatedAt)`
  commits one accepted change when the notebook has a Git binding.
  `FolderRelocationService.applyLiveFolderChange` is the pattern: reload the
  live rows inside the operation, `authorizationService.assertAuthorization`,
  `requireInNotebook`, then mutate through `EntityPersister`.
- Removing an attachment row inside `apply` already drops its path from the
  derived tree (`NotebookGitChangedFiles.forgetStalePaths`); permanently
  deleting a trashed folder with files relies on this
  (`NotebookGitWebFolderPermanentDeleteControllerTest`).
- `NotebookGitBookSourceFileProtection.refuseChanging` owns the local-publish
  refusal and its message; `BookRepository.findByNotebook_Id` gives the one
  Book; `Book.getSourceFilePath()` is a root-relative path, comparable with
  `NotebookGitPortablePath.ofAttachment(folderPath(folder), filename)`.
- Frontend: `frontend/src/pages/AttachmentPage.vue` shows filename, size and
  Download; `NotebookRealm.readonly` marks a notebook the viewer cannot edit;
  `popups.confirm` is the confirmation idiom (`useNoteRemovalFlow.ts`); routes
  `folderPage` and `notebookPage` exist. E2E: `notebooks/notebook_files.feature`.

PFE: reuse `AcceptedWebChangeService`, the live-reload operation pattern,
the tree derivation's removal of stale paths, the Book protection's rule and
message, the file page, and `popups.confirm`. Gap: an endpoint and operation
that removes one attachment row, and a Delete action on the file page.

## Outside-in proof

| Promise (seed example) | Slice | Observable proof |
| --- | --- | --- |
| Owner deletes `sketch.png` on the web; it leaves the folder (1) | 1 | E2E scenario in `notebook_files.feature`: open the file, Delete, confirm → back on its folder, the sidebar no longer lists it |
| After pull the file is gone; the note and other files are unchanged (1) | 1 | backend controller test on a Git-backed notebook (in the shape of `NotebookGitWebFolderPermanentDeleteControllerTest`): one new accepted commit whose parent is the previous head; the downloaded bundle (what pull fetches) lacks `physics/sketch.png` and still has `physics/Force.md` and another file unchanged; the row is gone |
| A file a note's `image:` names can be deleted; the note keeps `image:` (2) | 1 | same test class: note with `image: force.png` → delete `force.png` succeeds; the note's content still says `image: force.png` |
| Only people who can edit the notebook delete | 1 | same class: a user without edit rights → `UnexpectedNoAccessRightException`, no commit; the page hides Delete when `readonly` (frontend `AttachmentPage.spec.ts`) |
| A Book's source file is refused with the publish message; nothing changes (3) | 2 | same class: notebook whose Book names `paper.pdf` → delete → CONFLICT with the message `refuseChanging` gives; no new commit; row stays |

The broken picture after deletion is existing behavior (the picture address
answers 404) and is not proved again.

## Slices

### 1. An owner deletes a file from its page on the web
Type: Behavior
Status: done
Proof: slice 1 rows above. Accepted: `NotebookGitWebAttachmentDeleteControllerTest`
(one child commit, bundle lacks the file and keeps the rest, `image:` kept,
no-edit-rights refused with no commit); `AttachmentPage.spec.ts` (readonly hides
Delete); `notebook_files.feature` "Delete a file from its page".

Learnings: the sidebar needs `refreshSidebarStructuralListings()` after the
delete (the E2E fails without it). The service is
`notebookGit/WebAttachmentDeleteService`; the page lands through
`routes/containingLocation.ts`, now shared with folder removal and dissolve.
`StoredApiCollection`'s private `containingLocation` makes the same decision for
notes and could move there later.

Behavior: `physics/` holds the note `Force` and `sketch.png` → the owner opens
`sketch.png`, chooses Delete and confirms → one accepted change removes
`physics/sketch.png`; the owner lands on the `physics` folder page (the
notebook page for a root file) and the sidebar no longer lists the file.

Change: `DELETE /api/notebooks/{notebook}/attachments/{attachment}` in
`NotebookAttachmentController`, calling a service method that runs
`AcceptedWebChangeService.apply` with the live-reload pattern, removes the
row, and commits "Delete file: <path>". Regenerate the API client. Delete
button on `AttachmentPage.vue` (hidden when `notebookRealm.readonly`), with
`popups.confirm` asking plainly "Delete <filename>?". Check during the slice
whether the sidebar listing needs an explicit refresh after the delete; reuse the refresh folder operations use.

Sizing: at the upper end of one slice (endpoint, API regeneration, page
action, one E2E scenario), but one proof loop; splitting by layer would not
give a usable result.

### 2. A Book's source file cannot be deleted on the web
Type: Behavior
Status: done
Proof: slice 2 row above. Accepted:
`NotebookGitWebAttachmentDeleteControllerTest.aBooksSourceFileCannotBeDeleted`
(CONFLICT with the literal message, no commit, row stays);
`NotebookGitBookSourceFileProtectionControllerTest` still passes for local publish.

Learnings: `NotebookGitBookSourceFileProtection` is now a component owning the
Book lookup and the message; local publish and the web delete both call it.

Behavior: notebook whose Book names `paper.pdf` → the owner deletes
`paper.pdf` on the web → refused (CONFLICT) with "\"paper.pdf\" is the source
file of the Book \"…\". Remove the Book on the web before deleting, renaming
or changing it."; no commit, the file stays.

Change: give `NotebookGitBookSourceFileProtection` one rule for "this path is
the Book's source" used by both local publish and the web delete, so the
message lives in one place. The file page shows the refusal through the
existing API error display.

## Current decisions

- Delete removes the row outright; no Trash, no LFS object deletion.
- The confirmation is plain ("Delete <filename>?") and says nothing about
  history (owner, 2026-09-26).
- Delete stays visible on a Book's source file; the refusal comes from the
  delete request (owner, 2026-09-26).
- Slice 1 stays whole; if it overruns, split off the sidebar refresh first.
- No check for notes whose `image:` names the file, matching local publish.
- Proof of "gone after pull" is the downloaded bundle in a backend test, not a
  CLI E2E scenario.
