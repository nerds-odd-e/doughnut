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

<a id="story-17"></a>

### Keep a Book's source file as an ordinary notebook file
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../quick/034-book-source-as-notebook-file/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"6b062cb7514d647ba7859941df5c0d0ccc28aaff583afe376afad6a60e3761bf","plan":"aed569850f55fd2e84240db3cd293116ad7b64af2cb77c36cefef8d0ede27235"}}
```

- **Identity:** SEED-035#story-17
- **Goal:** Owners who read a PDF or EPUB Book in Donut also have its source
  file in the notebook: listed and downloadable on the web, and present after
  `donut notebook pull`, for example for an AI IDE working beside the notes.
  The main driver is the North Star's one byte store: once every Book reads
  from a notebook file, the separate Book storage can be removed (story 21).
  The owner benefit is modest, since the CLI attaches a PDF the owner already
  has locally; the owner kept the priority (2026-09-25).
- **Current state:** At most one Book per notebook (unique key); no replace,
  only remove and attach again. `book.source_file_ref` names bytes in the
  separate Book storage: GCS in production (`GcsBookStorage`), and
  `attachment_blob` elsewhere (`DbBookStorage`). Books appear nowhere in the
  notebook's Git tree, export or clone. PDFs are attached through the CLI
  `/attach` (MinerU layout with page numbers and boxes, so the layout depends
  on those exact bytes); the web attaches EPUB only. Production holds three
  or four Books (owner, 2026-09-25).
- **Scope (owner decisions 2026-09-25):**
  - The Book refers to its source file by its path in the notebook, like an
    `image:` value, not to hidden stored content. Book reading goes through
    the one attachment reader.
  - Attaching a Book (CLI or web) stores the verified LFS object first, then
    accepts, in one accepted change, the file at the notebook root and the
    Book that refers to it (North Star "one way in").
  - Donut chooses the name: `<book name>.<pdf|epub>`, or the next free name
    under the existing numbering rule when taken; a book name that is not a
    plain filename gets a Donut-chosen plain name.
  - Existing Books move once, on their own at startup, following story 5's
    pattern: one Donut System commit per notebook, layout and reading
    progress unchanged, safe to run again, a failing notebook reported
    loudly without stopping the others. The old copy stays until story 21.
  - The existing web "Remove" of a Book removes the Book and its reading
    data and leaves the file in the notebook. Deleting the file is story 2's
    job or a local change.
  - The 100 MB Book limit applies only to attaching a Book. Local publish
    keeps the 10 MiB limit for every new file.
- **Rejection constraint (owner decision 2026-09-25, consistency):** A local
  publish that deletes, renames or changes the file a Book refers to is
  refused, naming the path and saying to remove the Book on the web first;
  nothing is accepted. The Book's layout depends on those exact bytes, so
  accepting the change would leave a Book that silently points at the wrong
  pages or at nothing.
- **Deferred:** Following a local rename. Replacing a Book in place. Making a
  PDF already in the notebook into a Book, or CLI `/attach` reusing a file in
  the checkout. PDF attach on the web. Streaming or range requests for large
  files (whole-file reads, as today). Any history rewrite.
- **Key examples:**
  1. The owner attaches "Physics Primer" (PDF) to notebook `physics` with the
     CLI. The notebook gains one accepted change: root `Physics Primer.pdf`
     as an LFS file and the Book referring to it. Reading on the web is
     unchanged; the file is listed and downloadable in the web root folder;
     after `donut notebook pull` the checkout has `Physics Primer.pdf` with
     the same bytes.
  2. The root already holds `Physics Primer.pdf`: the Book's file becomes
     `Physics Primer (2).pdf`, and the existing file is untouched.
  3. A 60 MB PDF is accepted through attach. The same 60 MB PDF added and
     published from a local checkout is refused by the 10 MiB limit.
  4. An existing Book at startup: its file appears at the notebook root in
     one Donut System commit; its layout, reading records and last-read
     position are unchanged. Running again adds no commit.
  5. The owner removes the Book on the web: the Book and its reading data are
     gone; `Physics Primer.pdf` stays in the notebook.
  6. The owner deletes `Physics Primer.pdf` locally while the Book
     exists and publishes: refused, naming `Physics Primer.pdf`.
- **Depends on:** Story 14 (delivered).
- **Effort hypothesis:** M–L, low confidence. Not split: with three or four
  Books, moving the existing ones is small and reuses story 5's move.
- **Safe stopping point:** Books read correctly from either store during the
  move; the old copy stays until the move is verified in production.
- **Note for story 2:** Web deletion of a file a Book refers to follows the
  same rule as example 6.

<a id="story-18"></a>

### Remove the legacy picture storage
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"unselected","assessment":"not-ready","reasons":["Waiting for a release containing story 5 and its verification in production","No execution approach selected yet"],"basis":{"document":"2441cebb0a1cfb4a0618af77298fb91f5725ebbd65fdadb9ab36cffc79e4853a"}}
```

- **Identity:** SEED-035#story-18
- **Goal:** Maintainers keep one way to store and serve note pictures, and
  note saves and application startup stop doing legacy picture work. Owners
  keep every moved picture, because each one already works as a notebook
  file. Picture bytes leave MySQL, as the
  [Git LFS contract](../../docs/notebook-git-lfs.md) requires.
- **Why it waits:** Story 5 (the picture move) is done on main but not yet in
  a release. This story starts only after a release containing story 5 has
  run in production and the move is verified: the startup log shows no
  notebook whose move failed, the count of left-over references in the
  startup warning is recorded here, and one moved picture displays on the web
  and arrives on clone. Dropping the table deletes the backup copy of every
  moved picture and cannot be undone; the production database backup is the
  only safety net, and no special export is made.
- **Scope:** No new behaviour. Remove, in one release (code and table
  together):
  - the `image` table, with its picture bytes in `attachment_blob`, through a
    new Flyway migration;
  - the dead `note.image_id` column and its `fk_note_image_id` foreign key
    (no code uses them). This is required, not optional: that key is
    `ON DELETE CASCADE`, so the column and key are dropped before `image`, and
    the removal never deletes `image` rows one by one;
  - the `/attachments/images/...` address and its controller;
  - the legacy orphan-picture cleanup on note save and its call sites, and the
    legacy path parsing that only it and the move use;
  - story 5's startup move, and the testability seeding and move endpoints
    that exist only for legacy pictures;
  - tests and the end-to-end scenario that exist only for legacy pictures;
    tests that only use legacy pictures as fixtures keep their purpose without
    them;
  - the legacy picture rules in the attachment documentation.
- **Excluded:**
  - `attachment_blob` itself and its entity stay: non-production Book storage
    still uses them until story 21.
  - The `/attachments/` development proxy and GCP route stay; they are
    harmless and go with story 21 or not at all.
  - `image:` values the move left unchanged (another notebook's upload, an
    upload without a note, a missing upload) and `/attachments/images/...`
    links in note bodies are not rewritten or cleared (owner decision
    2026-09-25). They show as a broken picture. Copying another notebook's
    upload stays rejected because it would put one notebook's possibly
    private bytes into another notebook's history.
- **Key examples:**
  1. A note whose picture story 5 moved beside it → after the removal, the
     picture still displays on the web and arrives on `donut notebook clone`
     and `pull`.
  2. A note whose `image:` still names another notebook's legacy upload →
     after the removal, it shows a broken picture; the note, its content and
     its learning history are unchanged.
  3. A note that still has a `note.image_id` value → the migration drops the
     column and the note is still there with its learning history.
  4. An owner saves a note → no legacy picture cleanup runs. The application
     starts → no legacy picture move runs.
  5. An `image:` value that is an external `https://` address → still
     displays as before.
- **Depends on:** Story 5 (delivered on main) released and verified in
  production as above.
- **Effort hypothesis:** S–M, medium confidence; mostly deletion (about 8
  whole files and small edits in about 20), with test fixture edits as the
  main cost.
- **Safe stopping point:** Until it runs, the legacy picture store is an unused
  leftover that still holds a backup copy.

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
- Owner decisions, 2026-09-25 (story 17 refinement): keep the priority; the
  Book refers to its file by path; removing a Book leaves the file; refuse a
  local publish that deletes, renames or changes a Book's file; three or four
  production Books, so moving them stays in the story.
- Owner decisions, 2026-09-25 (story 5 refinement): move a picture only into
  the notebook that owns it; leave other-notebook references unchanged; accept
  an ordinary rebase conflict for unpublished local frontmatter edits; keep
  learning state and last-updated time; split legacy removal into pictures
  (story 18) and Books (story 21).
- Owner decisions, 2026-09-25 (story 18 refinement): leave unmoved legacy
  references as broken pictures; verify story 5 in production (no failed
  notebook, recorded left-over count, one moved picture displays and clones)
  before starting; remove code and the `image` table in one release; dropping
  the dead `note.image_id` column is important.
- [Near-future direction](../PRODUCT-BACKLOG.md#near-future-direction).
- [SEED-009](SEED-009-git-backed-local-notebook-workflow.md): prior local/web note
  workflow. This seed owns non-Markdown attachment continuity.
