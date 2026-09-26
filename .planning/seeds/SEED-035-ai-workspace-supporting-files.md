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

<a id="story-2"></a>

### Delete unwanted supporting files from Web Donut
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../quick/035-delete-notebook-file-on-web/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"627ac528518d10abcb66fa29337e993531392fde866f113b9a71f33cc8fe939a","plan":"e9eb6a56dc80e0cb677ecaedf539420d4fb86e2d77da5321242f88fc3fa2c556"}}
```

- **Identity:** SEED-035#story-2
- **Slice plan:** [Delete a notebook file on the web](../quick/035-delete-notebook-file-on-web/PLAN.md)
- **Goal:** An owner removes a non-Markdown file from a notebook on the web.
  Today only a local checkout can delete a file; the web can only open and
  download it.
- **Scope:**
  - A **Delete** action on the file's page, with a plain confirmation that
    names the file (for example "Delete sketch.png?"); it does not mention
    history (owner decision 2026-09-26). The same behavior for every file type.
  - Delete removes the file outright, through the existing accepted-change
    boundary; after `donut notebook pull` the file is gone from the checkout.
    Notes, other files and learning history are untouched.
  - Deleting a file that a note's `image:` names is allowed: the note shows a
    broken picture and its `image:` value stays as it was. Local publish
    behaves the same way (owner decision 2026-09-25).
  - Deleting a Book's source file is refused after Delete is chosen, with the
    same message local publish gives, and nothing changes; the page does not
    hide Delete for it (owner decision 2026-09-26). A Book reads from that file
    ([story 17 decision](#breadcrumbs): refuse changes to a Book's file).
  - Who may delete follows the existing notebook edit rule.
- **Excluded:**
  - No web Trash or restore for files (owner decision 2026-09-25). Earlier
    versions stay in accepted Git history.
  - Stored file contents (LFS objects in GCS) are not deleted; the North
    Star defers object garbage collection. Git history is not rewritten.
  - No automatic removal of the old file when a note's picture is replaced
    or cleared on the web, and no rewriting or clearing of `image:` values.
  - Deleting several files at once, a delete action in the sidebar, and
    renaming or moving files.
  - Markdown files: they already have ordinary note operations.
- **Key examples:**
  1. `physics/` holds the note `Force` and `sketch.png`. The owner opens
     `sketch.png` on the web, chooses Delete and confirms → `sketch.png` is no
     longer in the folder. After pull, `physics/sketch.png` is absent and
     `physics/Force.md` and its learning history are unchanged.
  2. The note `Force` has `image: force.png`. The owner deletes `force.png` →
     the delete succeeds; `Force` shows a broken picture and still says
     `image: force.png`.
  3. `paper.pdf` is a Book's source file. The owner tries to delete it → the
     delete is refused, saying the file is the source of that Book and to
     remove the Book on the web first; the file stays.
- **Effort hypothesis:** S–M, medium confidence. Building the accepted commit
  already drops the path of a removed file row; the work is an endpoint, the
  Book check, the page action and tests.
- **Depends on:** Delivered web file browsing and the Book source file.
- **Safe stopping point:** If never delivered, files are still deleted
  locally.

<a id="story-11"></a>

### Dissolve and merge folders that contain files
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../quick/007-dissolve-merge-folders-with-files/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"2bbe69530ee4d3f43b503cb3c17572fd1d0043ecdb771bcc51a339a45d309d35","plan":"5bf4d7947c2bda7e3d1618482ec9873fb6cd4897eb28a126c7fdea00cbecce54"}}
```

- **Identity:** SEED-035#story-11
- **Slice plan:** [Dissolve and merge folders that contain files](../quick/007-dissolve-merge-folders-with-files/PLAN.md)
- **Goal:** An owner tidying folders on the web can dissolve or merge a folder
  that contains files; the files move with the notes instead of the operation
  being refused. Since web-uploaded and migrated pictures became files in
  their notes' folders, the refusal blocks any folder holding a picture note.
  Along the way no web operation may silently lose a file or put two entries on
  one path, so a clone or pull on any operating system matches what the web
  shows.
- **Scope:**
  - Dissolve, dissolve with merge of same-named subfolders, and a same-notebook
    folder move with merge carry every file of the moved folders, exactly as
    they carry notes. The temporary "Folders containing files cannot be
    dissolved, merged, or moved to another notebook yet" refusal goes away for
    these operations.
  - **One set of names per folder** (and at the notebook root): a note's
    `Title.md`, a subfolder's name and a file's filename share one set of
    entry names, compared without regard to letter case. Every web placement
    asks this one rule, reading live rows: a name the user chose is refused
    when taken (note create, rename and move; folder create, rename and move;
    picture upload; dissolve and merge); a name Donut chooses is the first free
    one (note and folder trash, a Book's source file). This follows the North
    Star rule that Donut-chosen names take a free name and user-placed ones
    refuse, and replaces today's separate per-kind checks.
  - **Dissolve and merge check first, then change.** They work out every
    destination path before changing anything: same-named folders merge (a
    case variant such as `Diagrams` merges into the existing `diagrams`); any
    other taken path — file against file, note against note, or one kind
    against another, even with identical bytes — refuses the whole operation,
    names the first clashing path, and changes nothing (owner decision
    2026-09-21; first path only, 2026-09-26). This also closes the known gap
    that dissolve and merge never checked note titles.
  - **No database cascade on folder contents** (owner direction 2026-09-26):
    the foreign keys from a file to its folder (`CASCADE`), a folder to its
    parent folder (`CASCADE`) and a note to its folder (`SET NULL`) stop acting
    on delete. Code that means to remove a folder's contents removes them
    explicitly, so the accepted-change capture sees every removal; a forgotten
    file fails loudly ([ADR 0006](../../docs/adrs/0006-failure-handling-accepted.md))
    instead of vanishing. This covers permanently deleting a trashed folder,
    local-publish acceptance removing a folder, and the "remove empty folders"
    health fix, which today counts only notes and so deletes a folder holding
    only files, together with those files.
  - Existing content is never judged again; the rule applies to new placements.
- **Excluded:**
  - Moving or merging into another notebook (story 10). That refusal stays,
    including a merge into another notebook's same-named folder, which today
    reaches the same merge code.
  - Renaming on a clash, and rewriting file references. A note outside the
    dissolved folder whose `image:` points into it (`physics/intro.md` with
    `image: old/sketch.png`) shows a broken picture afterwards, as story 2
    accepted for deletion. Web uploads always place a picture in its note's own
    folder, so only locally written references can cross folders.
  - Rewriting path wiki links on a same-notebook merge move: that existing bug
    is [SEED-042#story-1](SEED-042-folder-merge-keeps-path-wiki-links.md#story-1).
  - Local-publish acceptance keeps its own validation; the shared name rule is
    for web placements. A Git tree cannot hold a file and a folder on one path.
  - Notebook-level cascades (notebook to folder, file, Book): notebooks are
    only soft-deleted and nothing triggers them.
  - Any other change to the health fix, such as committing its folder removal
    to accepted history.
- **Key examples:**
  - `physics/old/` holds `sketch.png`; `physics/` has no `sketch.png`. The owner
    dissolves `old` and pulls → `physics/sketch.png`, same bytes, no `old/`.
  - `physics/diagrams/a.png` and `physics/old/diagrams/b.png`: dissolving `old`
    with merge → both files in `physics/diagrams/`.
  - `archive/diagrams/c.png` moved into `physics/`, which has `diagrams/`, with
    merge → `physics/diagrams/c.png`.
  - `physics/force.png` and `physics/old/force.png` (different or identical
    bytes): dissolving `old` is refused naming `physics/force.png`; folders,
    notes, files and the accepted head are unchanged.
  - `physics/Energy.md` and `physics/old/energy.md`: dissolving `old` is
    refused naming `physics/Energy.md`, instead of today's generic conflict.
  - `physics/` holds `Force.png`: creating a folder `force.png` in `physics/`
    is refused naming `physics/Force.png`.
  - `refs/` holds only `paper.pdf`: "remove empty folders" keeps `refs/` and
    the file.
  - A trashed folder holding `paper.pdf` is permanently deleted → the file is
    gone after pull, now removed explicitly rather than by the database.
  - Moving `refs/` holding a file into another notebook's `refs/` with merge is
    still refused.
- **Effort hypothesis:** L (nine planned slices), medium confidence. A natural
  later split, if execution overruns: (a) no cascade plus one set of names per
  folder, then (b) dissolve and merge carry files.
- **Depends on:** delivered nested attachment continuity.
- **Safe stopping point:** if never delivered, the current refusal stays safe.
  The name rule and cascade removal each leave the product safer on their own.

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
3. Story 5: move existing pictures. Its startup run in v1.3.26 moved nothing;
   the owner's manual trigger completed it (verified 2026-09-25).
4. Story 17: Book files.
5. The legacy picture storage, the separate Book storage, `attachment_blob`,
   the `/attachments/` address and the Book bucket are removed.

"Present after clone or pull" is not a story: it is how each of these stories
is proven. Web deletion (story 2), then dissolve/merge (story 11) and the rarer
cross-notebook move (story 10) follow; the last two are conveniences whose
absence loses nothing.

The split is by usable outcome, not backend/frontend layers. Reject a
publish-now/preserve-on-web-later split: it would expose accepted files to
loss. Operations that would rehome files refuse until their story delivers.

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
  and Books (both now done).
- Owner decisions, 2026-09-25 (after the production check of the picture
  move): the manual trigger completed the move; the production
  check is a read-only database query, not the startup log; the two notes
  with dead picture links are fixed manually, outside any story.
- Owner decisions, 2026-09-25 (story 2 refinement): the purpose is only to
  remove the file; deleting a file a note's `image:` names is allowed and
  leaves a broken picture; delete outright with no web Trash; keep the
  backlog order.
- Owner decisions, 2026-09-26 (story 2 review): the confirmation is plain and
  does not mention that earlier versions stay in history, since most users do
  not need to know; refuse a Book's source file after Delete is chosen rather
  than hiding Delete; keep the plan's first slice whole and let execution
  escalate if it overruns.
- Owner decisions, 2026-09-26 (story 11 refinement): widen the story into a
  cohesive fix — one set of entry names per folder, compared without letter
  case, for every web placement; dissolve and merge check first and refuse
  naming the first clash; no database cascade on folder contents, with code
  removing contents explicitly. Local-publish acceptance keeps its own
  validation; notebook-level cascades stay. The same-notebook merge move's
  stale path wiki links become their own story, queued right after story 11.
- [Near-future direction](../PRODUCT-BACKLOG.md#near-future-direction).
- [SEED-009](SEED-009-git-backed-local-notebook-workflow.md): prior local/web note
  workflow. This seed owns non-Markdown attachment continuity.
