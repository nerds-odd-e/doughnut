---
id: SEED-035
status: dormant
planted: 2026-09-20
planted_during: owner shift toward parallel AI IDE work alongside Web Donut
trigger_when: selecting work for the shared local and web notebook direction
scope: large
---

# SEED-035: Keep AI working context and learning material together

## Why This Matters

Notebook owners use local AI IDEs alongside Web Donut. The desired outcome is
continuity of notebook content: Markdown notes and guidance follow Donut's
existing rules; images and other non-Markdown attachments travel with the notes.

The owner clarified that there is no special Markdown category for IDE guidance.
`AGENTS.md`, `SKILL.md`, and other Markdown use ordinary note/Readme behavior.
Invalid Markdown remains a publication error. Existing web-save normalization,
reserved Readme behavior, and valid unknown concept types remain unchanged.
AI guidance can use ordinary note refinement without special treatment.
Private learning history stays server-side and remains associated with notes.

The benefit is less manual copying between local authoring and web use. No time-saving or learning-improvement
measurements were supplied.
The priority remains a value hypothesis, not proof that all users need the same
workflow. Compliant Markdown guidance already has the ordinary note path;
non-Markdown acceptance is the remaining file-support gap selected here.

## Alternatives and Decision

- **Defer:** existing Markdown workflows continue, but non-Markdown material
  still cannot accompany the notebook through publication.
- **Keep files outside the notebook or in another repository:** adequate for
  machine-wide preferences or occasional use, but owners must assemble and
  maintain the supporting context separately.
- **Add a special Markdown attachment classifier:** rejected by owner direction.
  Purpose, IDE name, and file history do not exempt Markdown from current rules.
- **Recommended:** retain the cohesive Markdown behavior and add non-Markdown
  attachment continuity.
  Then deliver web retrieval and visual-authoring journeys through one file model.

The strongest workaround is a separate repository plus manual image downloads.
The first story earns priority only through the independent value of carrying
those files with the notebook, not because later stories need infrastructure.

## Architectural Constraints

- [ADR 0001](../../docs/adrs/0001-ubiquitous-language.md#notebook--note-structure)
  distinguishes Notes, container Readmes, and non-Markdown Attachments. Image
  presentation uses the same Attachment ownership and folder placement.
- [ADR 0004](../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md#validation)
  applies the current Markdown format without IDE exceptions. Missing/invalid
  type or malformed Markdown frontmatter rejects publication; invalid content
  is not an attachment fallback. Import, export, publication, and lint share
  the format contract. AI-authored note results use the same contract.
- [ADR 0002](../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md)
  makes the accepted tree authoritative for Portable content. Web operations
  preserve accepted files and apply complete content/reference changes through
  the same publication boundary. Learning identities remain private to Donut.
- [ADR 0005](../../docs/adrs/0005-web-routes-accepted.md) keeps authored content
  addresses distinct from private web identities. Attachment references do not
  become semantic note/property Wiki links.
- [ADR 0006](../../docs/adrs/0006-failure-handling-accepted.md) permits loud
  failures; do not conceal invalid output or lost files as successful acceptance.
- Accepted web changes derive their commit from the accepted head's tree and
  the projection rows the change touched (`NotebookGitTreeEncoder.derive`);
  today only the full assembly adds attachment entries. A web operation that
  inserts, renames or moves an attachment row must add that entry in the
  derivation (bytes for an insert, the accepted blob id at the previous path
  for a rename or move); the derived-tree oracle tests catch a missing entry.

The [North Star](../NORTH-STAR.md) owns the concise direction and the
[synchronization contract](../../docs/notebook-git-synchronization.md#attachments-in-the-portable-tree)
owns details. Ordinary local rebase reconciles unpublished work against the
latest accepted history; the remote never merges or rebases.

The accepted [Git LFS storage contract](../../docs/notebook-git-lfs.md), linked
from ADRs 0002 and 0004, specifies standard Git LFS, its client and protocol, and
immutable GCS payloads; accepted Git pointers select exact file versions. The
North Star's [one attachment content model](../NORTH-STAR.md#one-attachment-content-model)
governs every remaining story here: one byte store, every notebook on LFS, one
way in and one way out, roles (note picture, Book) that refer to Attachments,
and moving before retiring. Architecture acceptance does not mean
implementation is complete. Accepted history is preserved, so old binaries stay
in historical bundles.

## Story Decomposition

S = 30–60 minutes, M = 1–2 hours, L = 2–4 hours, including delivery. Estimates
are hypotheses; refine/split work that exceeds L before execution planning.
Root-file and nested-file continuity are delivered. Stories 11 and 10 own the
dissolve/merge and cross-notebook operations that the delivered behavior
refuses for now. The [product backlog](../PRODUCT-BACKLOG.md) owns global order.
No executable plan or implementation is authorized by this seed.

<a id="story-5"></a>

### Move existing uploaded note pictures into their notebooks
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../quick/033-move-legacy-note-pictures/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"65fc87ce405bdd382d2ee0c5e50dd6b30f2475ea4c42b7af801bc19f88faabf2","plan":"1d227b2c5363c8b25cea9f8a31fd2e1e62b7f7ef35d5dfbf9bfcb86f34dc0cf9"}}
```

- **Identity:** SEED-035#story-5
- **Goal:** Owners who work on a notebook in a local checkout see the pictures
  they uploaded before web uploads became files, without downloading and
  reattaching each one. The main driver is the North Star's one byte store:
  once every referenced picture is a notebook file, the picture half of the
  legacy storage can be removed (story 18). The owner value is a hypothesis;
  no production counts were supplied.
- **Current state:** An uploaded picture from before story 4 is an `image`
  row owned by one note, with its bytes in MySQL (`attachment_blob`). The note
  refers to it only through frontmatter `image: /attachments/images/{id}/{name}`,
  optionally with `image_mask:` rectangles. Export writes no bytes for it, so it
  is a broken server path in a local checkout. No code creates `image` rows
  since story 4, so the set is final.
- **Scope (owner decisions 2026-09-24 and 2026-09-25):**
  - A one-time move Donut runs by itself for every notebook, following the
    precedent of story 14's startup conversion: no owner action, button, or
    progress display.
  - A note is moved when its `image:` is `/attachments/images/{id}/…` and row
    `{id}` belongs to a note in the same notebook, whether that is the note
    itself or another note. Trashed notes are moved like any other: trash is
    an ordinary folder.
  - The move stores the picture bytes as a verified LFS object in the
    notebook's content store, then accepts, in one Donut System commit per
    notebook, a file in each moved note's folder and that note's `image:`
    rewritten to the file name. `image_mask:` and the rest of the note are
    unchanged.
  - The file takes the row's stored name when it is a plain filename that
    nothing in the folder uses; otherwise Donut takes a free name derived from
    it (North Star: Donut-chosen names never overwrite or refuse).
  - Moved pictures are not held to the 10 MiB limit: the limit is for new
    payloads, and this is existing content.
  - Running the move again, after success or interruption, changes only notes
    still holding a movable legacy value. A notebook whose move fails is
    reported loudly and does not stop the others; its pictures keep
    displaying from the legacy store.
  - Recall state, memory trackers, and the note's last-updated time stay as
    they were; the move is not an owner edit.
  - The legacy `image` rows and bytes stay as the backup copy until story 18.
- **Rejection constraint:** A note whose `image:` names a row owned by a note
  in *another* notebook is left unchanged and counted in the move's report.
  Copying would place bytes from one notebook, possibly private, into another
  notebook's history (owner decision 2026-09-25).
- **Deferred:** Unreferenced `image` rows (story 18 drops them with the
  table). `image:` values whose row no longer exists (nothing to move). Legacy
  addresses in note body text rather than `image:`. Remote URLs. Books (story
  17). Any history rewrite.
- **Key examples:**
  1. Note "Tokyo Tower" in folder `travel/` has
     `image: /attachments/images/7/tower.jpg` and `image_mask: 10 10 20 20`.
     After the move, `travel/tower.jpg` is an LFS file with the original bytes,
     the note has `image: tower.jpg` and the same mask, and the web shows the
     same picture with the same mask. The owner runs `donut notebook pull` and
     finds `travel/tower.jpg` beside the note, with those bytes. The notebook
     gained one Donut System commit.
  2. `travel/` already holds a file or note named `tower.jpg`: the picture
     takes the next free name under the product's existing numbering rule,
     such as `tower (2).jpg`, which `image:` names; the existing file is
     untouched. A stored name that is not a plain filename
     (empty, containing `/`, starting with `.`) also gets a Donut-chosen plain
     name.
  3. Notes "A" and "B" in the same notebook both refer to A's upload: each
     note gets a file in its own folder naming the same bytes, stored once.
  4. A note refers to an upload owned by a note in another notebook: the note
     is unchanged and still displays the picture from the legacy store while
     it remains.
  5. The move runs a second time: no commit is added to any notebook. It was
     interrupted after some notebooks: the rest move and the finished ones
     are untouched.
  6. A notebook with no legacy picture references gains no commit.
  7. The owner has an unpublished local edit to the same note's frontmatter:
     `donut notebook pull` reports an ordinary rebase conflict, which the owner
     resolves as usual (accepted by the owner).
- **Depends on:** Story 14 (every notebook uses LFS) and story 4 (no new
  legacy pictures), both delivered. A moved `image:` uses the note-relative
  spelling Web Donut already displays
  ([attachment references](../../docs/notebook-git-attachments.md#classification-and-references)).
- **Effort hypothesis:** L, low confidence until production counts are known.
- **Safe stopping point:** Moved and not-yet-moved pictures both display; the
  only copy of each picture is kept until its move is accepted.
- **Legacy row cleanup:** Orphan cleanup
  (`NoteService.deleteOrphanImagesForPersistedContent`) deletes a note's legacy
  rows only when `image:` is blank or an `/attachments/images/...` path; a
  note-relative value is classed `InvalidPathPresent` and skips cleanup. So
  rewriting `image:` leaves the old row in place as the backup story 18
  removes.

<a id="story-17"></a>

### Keep a Book's source file as an ordinary notebook file
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **Identity:** SEED-035#story-17
- **Goal:** Owners who read a PDF or EPUB Book in Donut also have its source
  file in the notebook: browsable and downloadable on the web, and present in
  a local checkout. Books stop needing their own file storage.
- **Evaluation:** Attach a Book on the web, then pull: the source file is in the
  notebook, and Book reading on the web is unchanged. An existing Book is moved
  the same way, keeping its layout and reading progress.
- **Scope / value (owner direction 2026-09-24):** A Book is private reading
  structure over one source Attachment (see the North Star). New Book attach
  writes the Attachment and the Book's reference in one accepted change, and
  existing Books are moved once. The Book source file is the one exception to
  the 10 MiB limit and keeps the Book upload limit (currently 100 MB).
- **Current state:** `book.source_file_ref` names bytes in the separate Book
  storage: GCS in production (`GcsBookStorage`), and `attachment_blob`
  elsewhere (`DbBookStorage`).
- **Depends on:** Story 14.
- **Effort hypothesis:** L, low confidence; split new-attach from moving
  existing Books if refinement finds it larger.
- **Safe stopping point:** Books read correctly from either store during the
  move; the old copy stays until the move is verified.
- **Open decisions:** Where the file is placed and named. What happens when the
  owner renames, deletes or replaces the Book's file locally (the Book's layout
  depends on those exact bytes). How the size exception is recognised when the
  file arrives through a local publish.

<a id="story-18"></a>

### Remove the legacy picture storage
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **Identity:** SEED-035#story-18
- **Goal:** Maintainers keep one way to store and serve note pictures. Owners
  keep every moved picture, because each one already works as a notebook file.
- **Evaluation:** After story 5 is confirmed in production, the following are
  gone: the `image` table and its bytes in `attachment_blob`, the
  `/attachments/images/...` address, legacy-picture orphan cleanup, story 5's
  startup move, and the code that used them. All moved pictures still display.
- **Scope / value:** Deletes code and data; no new behaviour. Runs in a release
  after the move is verified, never in the release that moves the bytes.
  `attachment_blob` itself stays for the non-production Book storage until
  story 21. Split from the combined legacy-removal story (owner direction
  2026-09-25) so the picture half need not wait for Books.
- **Depends on:** Story 5 delivered and verified in production.
- **Effort hypothesis:** S–M, medium confidence.
- **Safe stopping point:** Until it runs, the legacy picture store is an unused
  leftover that still holds a backup copy.
- **Open decisions:** What owners see for the `image:` values story 5 leaves
  unchanged (another notebook's upload, or a row already gone) once the
  address is removed.

<a id="story-21"></a>

### Remove the separate Book storage
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **Identity:** SEED-035#story-21
- **Goal:** Maintainers keep one attachment implementation. Owners keep every
  Book, because each one already reads from a notebook file.
- **Evaluation:** After story 17 is confirmed in production, the separate Book
  storage (`GcsBookStorage`, `DbBookStorage`), `attachment_blob`, and the code
  that used them are gone. All Books still read and keep their progress.
- **Scope / value:** Deletes code and tables; no new behaviour. Runs in a
  release after the Book move is verified. Split from story 18 (owner
  direction 2026-09-25).
- **Depends on:** Story 17 delivered and verified in production; story 18, so
  `attachment_blob` holds no picture bytes when it is dropped.
- **Effort hypothesis:** S–M, medium confidence.
- **Safe stopping point:** Until it runs, the separate Book storage is an
  unused leftover that still holds a backup copy.

<a id="story-2"></a>

### Delete unwanted supporting files from Web Donut

- **Identity:** SEED-035#story-2
- **Goal:** Owners away from a checkout remove obsolete non-Markdown attachments
  without switching tools. Markdown guidance retains ordinary note operations.
- **Evaluation:** Delete an attachment from its web folder, then receive the
  accepted result locally: it is absent, with unrelated notes, files, and
  learning histories intact.
- **Scope / value:** One deletion behavior covers image and other attachments.
  Local deletion is an available workaround, so web convenience follows the
  authoring handoffs. Removal from the current tree is not Git history erasure.
- **Effort hypothesis:** M, low confidence until referenced-file behavior is chosen.
- **Depends on:** Stories 6 and 1; specialized image display is not required.
- **Safe stopping point:** Current-tree cleanup works without deleting referring
  notes or learning data. No new Trash, restoration, or automatic reference
  rewriting is assumed.

<a id="story-11"></a>

### Dissolve and merge folders that contain files

- **Identity:** SEED-035#story-11
- **Slice plan:** [Mapped dissolve and merge with files](../quick/007-dissolve-merge-folders-with-files/PLAN.md)
  — awaiting story refinement; not ready for slice-plan refinement or execution.
- **Resplit trace:** Receives the dissolve, merge and filename-clash scope from
  the delivered nested-attachment work (owner-requested resplit, 2026-09-21).
  Nothing was implemented for this story.
- **Goal:** An owner tidying folders on the web can dissolve or merge a folder
  that contains supporting files, instead of being refused and having to
  reorganize in a local checkout.
- **Evaluation:** `physics/old/` holds `sketch.png` and `physics/` has no
  `sketch.png`. The owner dissolves `old` on the web and pulls →
  `physics/sketch.png`, same bytes. With different pictures at
  `physics/force.png` and `physics/old/force.png`, the dissolve is refused,
  naming `physics/force.png`, and nothing changes.
- **Scope / value:** Replaces the current dissolve/merge refusal: files move
  with the notes, including files of merged same-name subfolders. A convenience
  — the refusal loses nothing and a local workaround exists.
- **Clash rule (owner decision 2026-09-21):** When a web dissolve or merge
  would put a file on a path that is already taken, refuse the whole operation,
  name the clashing path, and change nothing. This is a Web Donut folder
  operation, not a Git merge: Git content merges stay the local user's problem
  because Donut accepts only forward linear history. Overwriting would lose a
  file; renaming would break local references while reference rewriting is
  deferred. Known related gap, not this story's to fix: dissolve and merge do
  not check note-title clashes today, although a single-note move does.
- **Effort hypothesis:** S–M, medium confidence; the clash rule is decided.
- **Depends on:** Delivered nested attachment continuity.
- **Safe stopping point:** If never delivered, the current refusal stays safe.
- **Refinement needed:** Confirm key examples for merge arrangements (dissolve
  with merge, move with merge) and whether the refusal message lists every
  clashing path or the first.

<a id="story-10"></a>

### Carry a folder's files along when it moves to another notebook

- **Identity:** SEED-035#story-10
- **Goal:** An owner reorganizing notebooks on the web can move a folder that
  contains supporting files to another notebook, instead of first removing or
  relocating those files locally.
- **Evaluation:** Folder `refs/` holds a note and `paper.pdf`. The owner moves
  `refs` to another notebook on the web, then pulls both notebooks: the source no
  longer has `refs/`, the destination has `refs/paper.pdf` with the same bytes,
  and the note keeps its learning identity.
- **Scope / value:** Replaces the current refusal with the move notes already get.
  Both notebooks' accepted trees change through the existing accepted-change
  boundary. The refusal loses nothing and a local workaround exists (copy the
  files between two checkouts), so this is a convenience, ranked after the
  retrieval, image and deletion journeys.
- **Effort hypothesis:** S–M, low confidence until the existing cross-notebook
  note move's publication path is inspected.
- **Depends on:** Delivered nested attachment continuity.
- **Safe stopping point:** If never delivered, the refusal stays safe and clear.
- **Open decisions for refinement:** Destination filename clashes follow the
  clash rule recorded in story 11. Confirm whether a cross-notebook merge needs anything
  beyond the plain move.

## Ordering and Scope Reduction

The [product backlog](../PRODUCT-BACKLOG.md) owns global order. Root and nested
file continuity, the size boundary, LFS for new notebooks, and web browsing and
download are delivered.

The owner's end state (2026-09-24) is one attachment model for every file:
existing and new pictures and Book files as LFS notebook files, the legacy
stores removed, and every file present after clone or pull. The order that
avoids a chicken-and-egg problem is:

1. Story 14: every notebook converted to LFS, and no path creates a raw one;
   then story 20 moves the tests to LFS notebooks.
2. Story 4: new uploads become files, so the set of legacy pictures stops growing.
3. Story 5: move existing pictures.
4. Story 17: Book files.
5. Story 18: remove the legacy picture storage, after story 5 is verified in
   production; story 21: remove the separate Book storage, after story 17 is
   verified.

"Present after clone or pull" is not a story: it is how each of these stories
is proven. Web deletion (story 2), then dissolve/merge (story 11) and the rarer
cross-notebook move (story 10) follow; the last two are conveniences whose
absence loses nothing.

The split is by usable outcome, not backend/frontend layers. Reject a
publish-now/preserve-on-web-later split: it would expose accepted files to
loss. Operations that would rehome files refuse until their story delivers.

## Open Refinement Details

- Story 17 needs key examples and its move/retry behaviour before planning.
- Story 11 needs story refinement and plan realignment before slice
  refinement/execution.
- File deletion needs an observable outcome for remaining references; warning,
  refusal, dangling-reference presentation, and Trash are different promises.

## When to Surface

Now, under the revised near-future direction. No implementation is authorized.
Ready-made AI skills, arbitrary document previews, file editors, and new Git
integration need their own selected outcomes.

## Breadcrumbs

- Owner direction, 2026-09-20: AI IDEs alongside Web Donut; portable attachments
  and image files; web browsing, download, and deletion.
- Owner clarification: all Markdown retains the existing cohesive behavior,
  with no guidance exception or refinement changes. Remove the format-guarantee
  story.
- Owner direction, 2026-09-23: cap attachments at approximately 10 MB, keep
  attachment payloads out of MySQL and ordinary Git history, reduce bundle size,
  and store payloads in bucket-backed object storage. Git commits use a
  Git-LFS-style content-addressed pointer, never a raw GCS URL. The owner accepted
  standard Git LFS and the preserved-history transition, directing concise rules
  into existing ADRs and the Git LFS details into a regular document. Rollout
  belongs in the North Star and stories; implementation remains separate work.
- Owner direction, 2026-09-24: the end state is (1) existing pictures moved to
  LFS/GCS in the new structure, (2) new uploads on the same path, (3) the old
  implementation removed, and (4) files present on clone and pull. Convert
  existing notebooks by forward commit (option A); do not reset history, and do
  not move old-history bytes. Drop the story for skipping AI guidance folders
  during assimilation entirely: it is not important and kept drawing attention.
  Books become ordinary attachments, and their
  source file is the one exception to the 10 MiB limit. One cohesive
  architecture governs these stories (North Star).
- Owner decisions, 2026-09-25 (story 5 refinement): move a picture only into
  the notebook that owns it; leave other-notebook references unchanged; accept
  an ordinary rebase conflict for unpublished local frontmatter edits; keep
  learning state and last-updated time; split legacy removal into pictures
  (story 18) and Books (story 21).
- [Near-future direction](../PRODUCT-BACKLOG.md#near-future-direction).
- [SEED-009](SEED-009-git-backed-local-notebook-workflow.md): prior local/web note
  workflow. This seed owns non-Markdown attachment continuity.
