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

<a id="story-5"></a>

### Finished transitions leave no trace in code and docs

**Identity:** SEED-050#story-5
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/024-finished-transitions-leave-no-trace/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"8c1ce1fcb03eaee1efd74d124358d2827e6d3c6e61222c91a80a429fea279874","plan":"322c7e1dd275cce99f3be4aa17e2694f5861ef63583f3f7996c8b416f8822e00"}}
```

**Goal:** Developers see only what the notebook Git model does today, and
Git alone decides what a pull replays.

**Scope:**

- Delete the unused `NotebookAttachment.getContent`/`setContent` aliases.
- Name `NotebookGitCutoverService` after what it does now (start and reset a
  notebook's history); a new notebook's first commit reads "Create notebook"
  instead of "Cutover: snapshot existing notebook content into Git".
- Docs describe creation and history reset in the present tense; the finished
  cutover and legacy-picture passages go (`notebook-git-synchronization.md`,
  `note-content-saving.md`, `notebook-git-attachments.md`,
  `notebook-git-lfs.md`). The sentence claiming the web editor and the server
  edit frontmatter "the same way" states instead which side makes which edit.
- Remove the pull's final-newline conflict absorber
  (`cli/src/commands/notebook/notebookPullRebase.ts`): local files are written
  by whatever tool the owner uses, so web and local final newlines never
  become uniform; a final-newline-only difference becomes an ordinary Git
  conflict (owner, 2026-09-27).
- Unchanged: existing commit messages in history; ADR 0002's text.
- Deferred: making web note bodies always end with a newline.

**Key examples:**

1. Create a notebook → its first commit reads "Create notebook" by the Donut
   System author.
2. From one base, the local `note.md` is "Same authored body.\n" and the
   accepted one "Same authored body." → pull → the rebase pauses on `note.md`
   with the usual guidance.
3. A pull whose local patch accepted history already contains → reported as
   already published, as today.

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
