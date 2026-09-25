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

<a id="story-19"></a>

### Remove the legacy raw file storage
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../quick/029-remove-raw-file-storage/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"8540ff502bdff2da0d7d495e8c2bdfebd84f29af1ab79035694033e7a68c8ef0","plan":"356d4251bd2d303c41989deebcf158db1c628a59dbc6f28e51abfb7e15f3b01c"}}
```

- **Identity:** SEED-035#story-19
- **Goal:** Maintainers keep one file representation in code. Once production
  has no raw binding, no code asks how a notebook stores its files. This is
  maintainer value only, so it follows the user-visible picture work
  (story 4).
- **Scope (refined 2026-09-24, owner accepted):**
  - The size check on publish, the file reader, and the publisher have a
    single LFS path. The `RAW` value, the entity default and the
    `attachment_representation` column go, over two releases (db-migration
    release safety: migrations run after the new instance serves). The first
    release removes the raw code and stops mapping the column; its migration
    refuses while any raw binding remains, otherwise it makes the column
    default `LFS`. The next release drops the column. The check is a plain
    part of that migration, not a gate left behind.
  - Delete story 14's startup conversion (`NotebookGitLfsConversionService`,
    the `NotebookGitLfsConversionOnStartup` trigger, and their tests); it
    exists only for raw notebooks. With it go the
    `convert_raw_notebook_to_lfs_for_testability` endpoint (generated client,
    E2E step, page object and scenario), the repository query
    `findNotebookIdsByAttachmentRepresentation`, and the `@Order` on
    `FlyWayFreeVersionRealMigration` that only orders it after migration.
    Delete the raw demotion endpoint, its E2E step and the raw backend fixture.
  - Keep: raw blobs already in accepted history, from before a notebook's
    conversion, stay valid. Cloning or pulling full history still works and an
    old commit still reads its raw bytes. The publish checks that walk
    history (server size admission and the CLI's LFS selection) keep accepting
    them. The removal is of raw *bindings*, not of raw history.
  - Confirmation: this ships in a release after the one that ran story 14's
    conversion in production (`v1.3.23`). Before deploying the raw-free code,
    check production for `RAW` bindings and defer the release until none remain.
    The refusing migration rechecks during deployment: if a raw binding appears,
    the deploy fails loudly (ADR 0006) without changing the column. This matters
    because that migration runs after the new instance becomes ready.
- **Key examples:**
  1. No raw binding in production → both deploys succeed and the column is
     gone. Publishing a picture to any notebook uses the LFS size check, and
     the web download reads through the pointer.
  2. One raw binding remains at the pre-deploy check → defer the raw-free
     release. If one remains at migration time, the migration fails, naming
     the notebook ids, with nothing dropped.
  3. A converted notebook whose history holds raw `physics/diagram.png` from
     before the conversion: a fresh clone succeeds, the old commit still has
     the raw bytes, and publishing a new picture is accepted.
- **Deferred promises:** Legacy picture, `attachment_blob` and Book storage
  (story 18); shrinking bundles; rewriting history.
- **Depends on:** Story 14 released and its conversion run in production;
  story 20, so no test needs raw notebooks.
- **Effort hypothesis:** S–M, medium confidence now that the fixture move is
  story 20.
- **Safe stopping point:** Until it runs, the unused raw paths remain harmless.

<a id="story-5"></a>

### Move existing uploaded note pictures into their notebooks
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **Identity:** SEED-035#story-5
- **Goal:** Owners use their accumulated pictures in a local checkout without
  downloading and reattaching each one. Every picture then lives in the one
  attachment model, which allows the legacy picture storage to be removed
  (story 18).
- **Evaluation:** A notebook has notes with web-uploaded pictures
  (`image: /attachments/images/{id}/{name}`). After the move, pull gives each
  picture as a file in its note's folder. Each note's `image:` names that file,
  and `image_mask:` is unchanged. The web shows the same pictures, and the notes
  keep their learning identities. Running the move again changes nothing.
- **Scope / value (owner direction 2026-09-24):** A one-time move for every
  notebook, not triggered by the owner. There is one Donut System commit per
  notebook that has pictures. It follows the North Star's
  [one attachment content model](../NORTH-STAR.md#one-attachment-content-model):
  LFS objects, Donut-chosen free filenames, and legacy rows kept until story 18.
  History is not rewritten. Remote URLs and Books are not converted.
- **Current state:** An uploaded picture is an `image` row owned by exactly one
  note, with its bytes in MySQL (`attachment_blob`), unrelated to
  `notebook_attachment`. The note refers to it only through frontmatter `image:`,
  optionally with `image_mask:` rectangles. Export writes no picture bytes, so
  each uploaded picture is a broken server path in a local checkout. Display is
  only `NoteShow` (note page, recall, conversations).
- **Depends on:** Story 14, so the move writes LFS only. Story 4, so no new
  legacy pictures appear once the move has run. A moved `image:` uses the
  note-relative spelling Web Donut already displays
  ([attachment references](../../docs/notebook-git-attachments.md#classification-and-references)).
- **Effort hypothesis:** L, low confidence until the counts and edge cases are
  known.
- **Safe stopping point:** Moved and not-yet-moved pictures both display; the
  only copy of each picture is kept until its move is accepted.
- **Open decisions:** A note whose `image:` points at another note's upload.
  `image` rows that no `image:` refers to any more. Owners with unpublished
  local edits to the same note's frontmatter get an ordinary rebase conflict;
  is that acceptable? The legacy address keeps the notebook read rule while it
  remains.
- **Legacy row cleanup:** Orphan cleanup
  (`NoteService.deleteOrphanImagesForPersistedContent`) deletes a note's legacy
  rows only when `image:` is blank or an `/attachments/images/...` path; a
  note-relative value is classed `InvalidPathPresent` and skips cleanup. So
  rewriting `image:` leaves the old row in place, which story 18 relies on to
  keep the backup copy until removal. Rename that classification if the
  relative spelling stays out of cleanup.

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

### Remove the legacy picture and Book storage
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **Identity:** SEED-035#story-18
- **Goal:** Maintainers keep one attachment implementation. Owners keep every
  picture and Book, because each one already works through notebook files.
- **Evaluation:** After stories 5 and 17 are confirmed in production, the
  following are gone: the `image` table, `attachment_blob`, the
  `/attachments/images/...` address, the separate Book storage, and the code that
  used them. All pictures and Books still display.
- **Scope / value:** Deletes code and tables; no new behaviour. Runs in a
  release after the moves are verified, never in the release that moves the
  bytes.
- **Depends on:** Stories 5 and 17 delivered and verified in production.
- **Effort hypothesis:** S–M, medium confidence.
- **Safe stopping point:** Until it runs, the legacy stores are unused leftovers
  that still hold a backup copy.

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
   Story 19 removes the raw representation after production confirms none is
   left; it gives maintainer value only, so it follows story 4.
3. Story 5: move existing pictures.
4. Story 17: Book files.
5. Story 18: remove the legacy stores, after production verification.

"Present after clone or pull" is not a story: it is how each of these stories
is proven. Web deletion (story 2), then dissolve/merge (story 11) and the rarer
cross-notebook move (story 10) follow; the last two are conveniences whose
absence loses nothing.

The split is by usable outcome, not backend/frontend layers. Reject a
publish-now/preserve-on-web-later split: it would expose accepted files to
loss. Operations that would rehome files refuse until their story delivers.

## Open Refinement Details

- Stories 5 and 17 each need key examples and their move/retry behaviour
  before planning.
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
- [Near-future direction](../PRODUCT-BACKLOG.md#near-future-direction).
- [SEED-009](SEED-009-git-backed-local-notebook-workflow.md): prior local/web note
  workflow. This seed owns non-Markdown attachment continuity.
