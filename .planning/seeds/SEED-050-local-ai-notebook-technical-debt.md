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

<a id="story-1"></a>

### Local publish checks the Markdown it changes once, before applying anything

**Identity:** SEED-050#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/020-validate-changed-markdown-once/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"4213a2e511a6f2eb833b7ac740f7a70b392778f81879a2a7d7d13405045cee2d","plan":"41d31f7e226e32b0d264e16211f9374d3c935d8977955273f5fb3f144b565416"}}
```

**Goal:** A notebook owner publishing from a local checkout gets the same
Markdown refusal at the same step whether or not the proposal moves a folder,
and publishing costs in proportion to what changed rather than to the
notebook's size. Developers find proposal validation and the drift guard once
each in the publisher instead of inside the apply steps.

**Scope:**

- Strict typed-Markdown validation runs once, right after the publisher reads
  the changed files and before any shape detection or change, on the `.md`
  files the proposal adds or changes. Today it walks the whole proposed tree
  from up to three places (`NotebookGitProposalPublisher`,
  `NotebookGitProposalDocumentApplication`,
  `NotebookGitProposalFolderRelocation`), so a relocation publish parses every
  note three times.
- Unchanged accepted Markdown is not judged again, consistent with
  SEED-009#story-48's decision.
- The check that the live projection matches accepted main runs once in the
  publisher before any change; the relocation's second mode
  (`applyAfterMatchedAcceptedTree`) goes away.
- Unchanged: refusal messages; where authored properties and the Readme type
  are checked (each already has one owner where the blob is read); the final
  check against the proposed tree.
- Dropped (owner, 2026-09-27): rewriting note and folder publication as one
  final-state step "like files". Notes and folders carry identity (learning
  data, links, folder ids) that files do not, so that rewrite would keep all
  the correspondence machinery and only move it.
- Depends on SEED-009#story-48 (plan 019), which rewrites the same Markdown
  check and adds a classifier this check reuses.

**Key examples:**

1. Accepted `Physics/` is moved to `Science/Physics/` and the proposal adds
   `Science/Physics/Waves.md` without `type` → publish → refused "Invalid
   Markdown" naming `Science/Physics/Waves.md`; nothing changes.
2. The proposal edits only `Notes.md`, adding a duplicate YAML key → refused
   naming `Notes.md`. An untouched accepted note without frontmatter no
   longer blocks a publish that changes other notes.
3. The live projection has drifted from accepted main and the proposal moves
   a folder or adds a note → refused "refresh the checkout"; nothing changes.
4. The proposal only adds a file → it still publishes.

<a id="story-2"></a>

### Adding a relationship on the web places its note through the server in one accepted change

**Identity:** SEED-050#story-2
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/021-relationship-notes-accepted-in-one-change/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"4213a2e511a6f2eb833b7ac740f7a70b392778f81879a2a7d7d13405045cee2d","plan":"bc06f312f565517c394c884a6beacef1a97e5aefa2c00794fed6b906c266505f"}}
```

**Goal:** A notebook owner who adds a relationship on the web gets the note
in the chosen folder, also when that folder's name differs only in letter
case, and the note and any new folder reach accepted Git as one commit, so
clone, pull and publish see them. Today web-created relationship notes and
Wikidata-assisted notes never reach accepted Git, and the next local publish
is refused as drift.

**Scope:**

- Web creation of relationship notes and Wikidata-assisted notes goes through
  the accepted change like every other note creation
  (`WebNoteCreationService` bypasses it for any non-`Note` type and for
  Wikidata). The Wikidata lookup happens before the notebook lock.
- Note creation takes an optional child-folder name, found ignoring case or
  created in the same change, through the one name owner. The server stays
  free of relationship-specific placement (as decided when
  `RelationshipNotePlacement` was removed).
- The relationship dialog stops listing and creating folders itself
  (`frontend/src/utils/relationshipFolderResolve.ts`).
- The 10 MiB limit on new attachment payloads is defined once
  (`PictureFile`, `NotebookGitAttachmentSizeAdmission`).
- Unchanged: the audio upload limit (a different concept) and the Book
  source limit (a documented exception).
- Dropped (owner, 2026-09-27): moving the picture upload and Book attach
  checks under the lock (a check inside the lock still reads the snapshot the
  transaction already took, and the database already refuses the same-name
  race loudly); one "first free name" operation (there is one numbering
  algorithm; the four callers' predicates differ legitimately).
- Deferred: AI "create extracted note" also bypasses the accepted change;
  undo removing a folder created for the note; retiring the exact-match
  folder sibling check on folder creation and publish.

**Key examples:**

1. `Europe` holds folder `Relations` → add a relationship from
   `Europe/Paris` with the default placement → the note is created in
   `Relations`; no new folder; one commit.
2. `Europe` holds no `relations` folder → the folder and the note are created
   and the accepted head advances once.
3. `Europe` holds a file named `relations` → refused naming
   `Europe/relations`; no folder, no note, head unchanged.
4. A relationship note created at the root → the accepted head contains it,
   and a following local publish is not refused as drift.

<a id="story-4"></a>

### The notebook tree has one model in code

**Identity:** SEED-050#story-4
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/023-one-notebook-tree-model/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"e6161163959b0e22e3bce2e0d325b35ac717097dfd44eadf96c221ed26b4db1b","plan":"76ee1b124492c58e07dbee2cf33510d9fa97a5f4cbe45d316249da7008decf23"}}
```

**Goal:** Developers read a folder's ancestry from one method on `Folder`
and read what a container (the notebook root or a folder) holds through one
query per kind, so root and folder code paths stop diverging, with no
performance loss.

**Scope:**

- `Folder` answers its trail from the root; the move-into-descendant check
  and the health rule's depth derive from it instead of their own loops
  (`FolderMoveDestinationRules`, `FolderSubtreeOccupancy`), and services stop
  depending on a controller DTO helper (`FolderTrail`) for a domain rule.
- One container query per kind (folders, files, notes) taking the notebook
  and the folder or the root, as the name checks already do. The paired
  root/folder queries, the listing's branch and `MovedNotePicture`'s own
  root/folder branch go away.
- Constraints: the same number of queries per request; ancestry compared by
  id; listing response and move messages unchanged.
- Unchanged: `Folder.isTrashed` and `NotebookGitPortablePath.folderPath`
  (short recursions already reading as definitions, on hot paths).
- Dropped (owner, 2026-09-27): resolving a Book's source file one way (it is
  always a plain filename at the root, so both readings agree).

**Key examples:**

1. Folder `A/B/C`, move `A` into `C` → "Cannot move folder into its
   descendant.", as today.
2. Empty nested folders `X/Y/Z` → the empty-folder purge removes `Z`, then
   `Y`, then `X`, as today.
3. The root holds a note, a folder and a file, and so does a folder → each
   listing shows only its own entries, as today.
4. A root note whose picture another root note also uses is moved into a
   folder → the picture is copied, not moved, as today.

<a id="story-5"></a>

### Finished transitions leave no trace in code and docs

**Identity:** SEED-050#story-5
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/024-finished-transitions-leave-no-trace/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"4213a2e511a6f2eb833b7ac740f7a70b392778f81879a2a7d7d13405045cee2d","plan":"322c7e1dd275cce99f3be4aa17e2694f5861ef63583f3f7996c8b416f8822e00"}}
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
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/025-one-word-for-image/PLAN.md","assessment":"not-ready","reasons":["Waits for SEED-050#story-2 (plan 021) and SEED-050#story-4 (plan 023), which change PictureFile, the picture upload and MovedNotePicture; re-read the starting facts afterwards."],"basis":{"document":"4213a2e511a6f2eb833b7ac740f7a70b392778f81879a2a7d7d13405045cee2d","plan":"f3029c04ddbe47d94ba6f7126596694cf8845022771158ea06bca40f5a1d49e2"}}
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
