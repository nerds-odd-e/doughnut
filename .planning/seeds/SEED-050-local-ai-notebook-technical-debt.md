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
pass silently.

All stories were re-evaluated against the code at `d9abdcc2eb` on 2026-09-27
and cut to what still holds and pays off (owner decisions the same day). Each
must leave the design smaller or more cohesive with no performance loss.
Claims the review got wrong were dropped rather than planned: `fk_note_folder`
is already `ON DELETE RESTRICT` (since `V300000329`); local publish does not
apply a document twice; the "five first-free-name versions" are one algorithm
with small legitimate predicates; a Book's source file is always a plain
filename at the notebook root; and the web editor and the server make
different frontmatter edits, each already with one owner.

## Story Decomposition

<a id="story-7"></a>

### Code, API and docs say "image", and the UI says "File"

**Identity:** SEED-050#story-7
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/025-one-word-for-image/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"bacae6f90ae97410c39bd1affdb92c4c6ee7e2f2ecebfccd12758e6d5b7ad994","plan":"4b2be11412ad113af679736da97f8b106f9496f828c613401a189efe06798189"}}
```

**Goal:** New work copies one name per concept: Attachment and Image in code,
API and docs, and File in the UI, so the crossed image endpoint names stop
misleading developers.

**Scope:**

- ADR 0001's Attachment entry gains "Short UI: **File**" (owner decision
  2026-09-27), legitimizing "File" on the file page, in "File not found." and
  in "Delete file" commit messages.
- "picture" becomes "image" in code, the API and messages: `PictureFile`,
  `InlinePicture`, `MovedNotePicture`, the file page realm's `picture` field,
  and the endpoints — the note's image by path and an attachment's image by
  id get matching image names (today `showAttachmentImage` is the note
  endpoint and `showAttachmentPicture` the attachment one).
- Docs, North Star and E2E phrases say "image".
- Unchanged: Attachment stays the code, API and schema name; the two image
  endpoints stay separate resources; the upload endpoint.
- After SEED-050#story-2, which changes `PictureFile` and the picture upload,
  and SEED-050#story-4, which edits `MovedNotePicture`.

**Key examples:**

1. The file page for `diagram.png` shows it through the attachment's image
   endpoint.
2. A note with `image: moon.jpg` loads it through the note's image endpoint
   with that path.
3. Uploading `a.txt` as a note image → refused "Cannot upload a.txt: an
   image must be a png, jpg, jpeg, gif or webp file." (today "a picture").

<a id="story-9"></a>

### Folder dissolve and merge enter subfolders by the one entry rule

**Identity:** SEED-050#story-9
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/027-dissolve-enters-folders-by-the-one-rule/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"f61c17e5b01f316515c1f60b18096a39cc1dde9a740a90e434a0ac3d9bd1affe","plan":"876b08ab2e545d9cafab89deb82366c270763849229aa48bdfd47033c3436009"}}
```

**Goal:** Correction of SEED-050#story-8 (provenance
`da1e269ba5:.planning/slice-plans/026-one-folder-entry-rule/PLAN.md`, commits
649e162dd4 and cba24a706e). Developers who next change folder naming find
"enter the folder holding this name (ignoring case), else refuse a note or
file holding it" only in `FolderSiblingNameValidation.folderToEnter`;
the dissolve/merge placement check no longer carries its own copy.

**Scope:**

- Required: `FolderContentsPlacementCheck.firstFolderMeetingAFolder` gets each
  subfolder's existing destination folder from `folderToEnter`; net fewer lines.
- Preserved: dissolve and merge-move refusals naming the note or file path,
  case-variant folder merging, and every message and error type.
- Excluded: the folder-only `requireNoConflictingSibling` family; the trash
  path's `findOrCreateFolder`.

**Plan:** [027-dissolve-enters-folders-by-the-one-rule](../slice-plans/027-dissolve-enters-folders-by-the-one-rule/PLAN.md)

**Effort hypothesis:** S — high confidence.

**Depends on:** none (SEED-050#story-8 is delivered).

<a id="story-10"></a>

### Notebook history commits are proven where they happen

**Identity:** SEED-050#story-10
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/028-history-commits-proven-where-they-happen/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"d07d54d7ed0835c3ef7a49632aacea5c47e485106399dd186c8073294d8f548d","plan":"11e47dc69185c0bbc4913f29f26940dd172ee7e521e2085e4c12ea5b59c9e844"}}
```

**Goal:** Correction of SEED-050#story-5 (commits 48d5a2e8f8..5afa318baf): a
developer changing notebook creation or history reset learns from the
entry-point tests when the commit message or author changes, and the
synchronization doc says only what those commits do.

**Scope:**

- Creation through the notebook and circle controllers asserts the root commit
  "Create notebook" by Donut System <system@donut.local>.
- History reset asserts "Reset: restart Git history from the current notebook"
  by the same author.
- The service test drops checks the shared creation assertion already makes.
- `docs/notebook-git-synchronization.md` drops "Repository creation needs no
  owner opt-in…" and the "still" in "may still read a complete tree".
- Unchanged: `notebook-git-lfs.md` decision-record text; the binding-less
  branch in `resetHistory`.

**Key examples:**

1. Create a notebook through the controller → its root commit reads "Create
   notebook" by Donut System.
2. Reset a notebook's history → its root commit reads "Reset: restart Git
   history from the current notebook" by Donut System.
