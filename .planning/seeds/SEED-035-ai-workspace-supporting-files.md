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
AI guidance can use ordinary note refinement without special treatment. A
separate selected story will identify common guidance folders and skip their
contents during assimilation; its exact folder/ignore policy awaits refinement.
Private learning history stays server-side and remains associated with notes.

The benefit is less manual copying between local authoring and web use, and
keeping guidance out of study sessions. No time-saving or learning-improvement
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
- **Recommended:** retain the cohesive Markdown behavior, add non-Markdown
  attachment continuity, and separate folder-based assimilation selection from
  note format and refinement.
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
latest accepted history; the remote never merges or rebases. Story 8 owns the
selected folder-based assimilation outcome; its detailed behavior is not decided here.

The accepted [Git LFS storage contract](../../docs/notebook-git-lfs.md), linked
from ADRs 0002 and 0004, specifies standard Git LFS, its client and protocol, and
immutable GCS payloads; accepted Git pointers select exact file versions. The
[North Star](../NORTH-STAR.md#attachment-storage-transition) owns initial
transport, size-policy, and rollout choices. Architecture acceptance does not
mean implementation is complete. Stories 13 and 14 separate new
notebook adoption from migration; browsing and image behavior keep their own
outcomes. Accepted history is preserved, so old binaries remain in historical
bundles even after their physical storage moves out of MySQL.

## Story Decomposition

S = 30–60 minutes, M = 1–2 hours, L = 2–4 hours, including delivery. Estimates
are hypotheses; refine/split work that exceeds L before execution planning.
Root-file and nested-file continuity are delivered. Stories 11 and 10 own the
dissolve/merge and cross-notebook operations that the delivered behavior
refuses for now. The [product backlog](../PRODUCT-BACKLOG.md) owns global order.
No executable plan or implementation is authorized by this seed.

<a id="story-14"></a>

### Move existing notebook attachments out of MySQL while preserving access and history
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **Identity:** SEED-035#story-14
- **Goal:** Existing notebook owners retain their files, local workflow, and
  history while operators remove attachment payloads from MySQL and stop new
  binary versions enlarging their Git bundles.
- **Evaluation:** Convert a notebook with several historical versions of a PDF
  and image. Current files hydrate with identical bytes, existing web access
  still works, and all earlier commit IDs and historical file bytes remain
  retrievable. A subsequent file update adds a pointer to Git and bytes to GCS;
  no current or historical attachment payload copy remains in MySQL.
- **Scope / value:** Migrate generic attachments by forward conversion of the
  current tree, keep old raw Git blobs readable from GCS under their original
  Git object IDs, and verify copies before removing database payloads. Handle
  interruption and existing local checkouts without losing unpublished work.
  Do not rewrite accepted commits or promise that their full bundles shrink.
  Legacy uploaded-image conversion remains story 5's separate outcome.
- **Depends on:** Stories 13 and 15 for storage and existing-checkout continuity.
- **Effort hypothesis:** L, low confidence until fleet size, migration recovery,
  and historical storage access are understood; resplit if larger than L.
- **Safe stopping point:** Converted notebooks use one LFS write model;
  unconverted notebooks and historical commits remain readable throughout the
  transition. Retain the only accessible copy until verified replacement exists.
- **Refinement remaining:** Confirm migration batching, rollback/retry proof,
  verification of grandfathered files against accepted bytes (never a client
  exemption), and stale-checkout upgrade examples. This is
  an unqueued migration candidate; the backlog still contains the two selected
  size-limit and new-notebook stories.

<a id="story-8"></a>

### Skip common AI guidance folders during assimilation

- **Identity:** SEED-035#story-8
- **Goal:** Learners can keep AI guidance in their notebook without that guidance
  being offered in the assimilation sequence simply because it is Markdown.
- **Scope direction:** Identify the most common AI guidance folders and skip
  their contents during assimilation. The owner explicitly defers how: ignoring
  an entire folder is one possibility, not a chosen implementation or policy.
  Guidance remains ordinary Markdown and can be refined normally.
- **Evaluation:** A notebook contains guidance in a recognized folder alongside
  ordinary study notes. During assimilation, guidance from that folder is skipped
  and the ordinary eligible study notes remain available.
- **Value / why now:** Protects the learner's attention as local IDE material
  joins the notebook. The [product backlog](../PRODUCT-BACKLOG.md) owns its
  position; an earlier explicit second place was superseded there.
- **Effort hypothesis:** M, low confidence until folder identification and the
  intended meaning of ignoring/skipping are refined.
- **Depends on:** No attachment story; compliant Markdown already uses the
  existing notebook path.
- **Safe stopping point:** Learners can retain AI guidance while continuing
  ordinary assimilation. This story does not require special refinement behavior
  or silently deleting authored content or learning history.
- **Open decisions for refinement:** Which folder conventions are common enough
  to recognize? How do nesting, user control, and already-assimilated content
  behave? Is the exclusion limited to assimilation, or does the owner intend a
  broader folder-ignore rule? Decide those boundaries before planning; do not
  turn examples into a fixed IDE list or choose an ignore mechanism now.

<a id="story-5"></a>

### Access existing note images as notebook folder files
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **Identity:** SEED-035#story-5
- **Goal:** Existing notebook owners use accumulated visual knowledge locally
  without downloading and reattaching each image manually.
- **Evaluation:** Acquire a notebook with existing uploaded note images: their
  referenced bytes are ordinary folder files usable with the notes locally,
  while existing web display and note learning identities remain intact.
- **Scope / value:** Existing notebook-owned uploaded images gain the common
  attachment behavior, benefiting current content immediately. Represent the
  transition as new accepted content without rewriting history. Remote image
  URLs and Books are not implicitly copied or converted.
- **Effort hypothesis:** L, low confidence until ownership/sharing/reference
  cases are understood; do not presume a fleet conversion fits this estimate.
- **Depends on:** Story 3 settles the reference spelling and web display that
  a converted `image:` must use; story 4 stops new uploads recreating legacy
  images. Converting before either repeats or pre-empts their work.
- **Safe stopping point:** Existing images work locally and on the web even if
  later image-authoring flows are deferred. Preserve the only accessible copy
  throughout transition; avoid a separate permanent image-file model.

#### Interim refinement (2026-09-24, not yet refined)

- **Current state:** An uploaded image is an `image` row owned by exactly one
  note, with its bytes in MySQL (`attachment_blob`), unrelated to
  `notebook_attachment`. The note refers to it only through frontmatter
  `image: /attachments/images/{id}/{name}`, optionally with `image_mask:`
  rectangles. Export writes the note unchanged and no image bytes, so every
  uploaded image is a broken server path in a local checkout. Display is only
  `NoteShow` (note page, recall, conversations); no AI feature uses images.
- **Value challenge:** Only owners who have uploaded images and work on those
  notebooks locally benefit; how many images and notebooks are affected is
  unknown. Images stay safe and visible on the web meanwhile, so deferral
  loses nothing. The benefit is a complete local notebook, not a new capability
  for AI work.
- **Ordering (owner, 2026-09-24):** Placed after stories 3 and 4 in the
  product backlog.
- **Candidate narrow scope (proposal, undecided):** Conversion is triggered
  by the owner per notebook as one accepted commit, not a fleet migration.
  It covers `image` rows of that notebook's notes that the note's `image:`
  currently references. The file is placed in the note's folder and `image:`
  is rewritten to it; `image_mask:` is unchanged. Legacy rows and the
  `/attachments/images/...` endpoint are kept.
- **Candidate exclusions:** remote URLs; one note referencing another note's
  image; notebooks the user does not own; automatic conversion of new uploads
  (story 4); local mask presentation; removing the `image` table.
- **Open decisions:**
  - Raw-representation notebooks: the North Star places story 14 (LFS
    migration, unqueued) before image conversion. Either limit this story to
    LFS notebooks, queue story 14 first, or accept image bytes entering raw
    Git history.
  - Owner-triggered per notebook, or automatic?
  - Filename clash when two notes in one folder share an image filename, or
    the folder already has that file: rename or refuse? Refusing blocks
    conversion permanently.
  - Reference spelling (note-relative or notebook-root-relative) is shared
    with story 3.
  - An owner with unpublished local work will find `pull` refusing over the
    web attachment change; acceptable, or does conversion need guidance?
- **Related finding:** the public legacy image endpoint is queued first as
  [SEED-040 story 1](SEED-040-private-note-images.md#story-1) (owner,
  2026-09-24); this story's conversion would not remove the exposure while
  legacy rows remain.

<a id="story-16"></a>

### Keep Markdown image embeds when a note is edited on the web
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"unselected"}
```

- **Identity:** SEED-035#story-16
- **Kind:** Bug, queued first priority by the owner (2026-09-24).
- **Goal:** An owner who embeds a picture in a note body locally
  (`![](force-diagram.png)`) does not lose that line because the note was later
  edited in Web Donut's rich editor and then pulled.
- **Expected:** A web edit changes only what the user changed; other authored
  body content, including image embeds, survives the save.
- **Actual (2026-09-24):** The body is shown in Quill, whose allowed formats
  (`frontend/src/components/form/QuillEditor.vue`) exclude `image`, so the
  `<img>` produced by `marked` is dropped. The first rich-mode edit rebuilds the
  whole body from Quill HTML (`RichMarkdownEditor.vue` `htmlValueUpdated`), so
  `![...](...)` is removed and published; `pull` then deletes it locally.
  Viewing alone writes nothing, and Markdown (textarea) mode keeps text verbatim.
  Applies to any body image, including remote URLs.
- **Scope:** Preserve the embed through a rich-mode edit. Showing the picture
  is not promised here (story 3 covers frontmatter `image:` only; body image
  display needs its own story), so a preserved but invisible embed is acceptable.
- **Key examples:**
  1. Body `Intro\n\n![force](force-diagram.png)\n\nMore` → the owner edits
     "More" to "More text" on the web → the saved body still contains
     `![force](force-diagram.png)` in the same place.
  2. A note without images saves exactly as before.
- **Effort hypothesis:** S–M, low confidence until the editor round trip is
  inspected; route through dough-bug-fixing.
- **Depends on:** none.
- **Safe stopping point:** Authored embeds are no longer lost; display unchanged.

<a id="story-3"></a>

### See locally added image files in Web Donut notes
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"unselected"}
```

- **Identity:** SEED-035#story-3
- **Goal:** An owner who works locally keeps a picture as a file in the
  notebook, points the note at it, and publishes; opening the note in Web Donut
  (note page, recall, conversations) shows that picture, without uploading it
  again. The owner confirmed this is a real scenario (2026-09-24): users want
  to embed an image in Markdown with the image kept in the notebook.
- **Scope (owner decision 2026-09-24: frontmatter only):**
  - The note's frontmatter `image:` accepts a path relative to the note's own
    folder, such as `force-diagram.png` or `images/force.png`. Wherever the
    note image already appears, the web shows that notebook file, with
    `image_mask:` unchanged.
  - Reading the picture needs the same notebook read authorization as the
    existing file download (Bazaar readers allowed, non-readers refused).
  - Only raster pictures are shown inline: PNG, JPEG, GIF and WebP. The file
    download itself stays a forced download.
  - Existing `/attachments/images/...` values behave as before.
- **Deferred promises (not built or verified here):**
  - Markdown body images (`![](force-diagram.png)`). They are the natural IDE
    form, but showing them is new display behavior in the editor; it needs its
    own story. The bug that deletes them on a web edit is
    [story 16](#story-16), queued first.
  - SVG inline display: an SVG opened directly can run scripts.
  - Paths starting with `/` (easily confused with legacy
    `/attachments/images/` paths), paths leaving the notebook, and remote URLs.
  - Rewriting references when a file is renamed or moved (same stance as
    story 11), and any special display for a missing file: a broken reference
    stays visibly broken, as today.
  - New web uploads (story 4), converting old images (story 5), and closing
    the public legacy image endpoint
    ([SEED-040 story 1](SEED-040-private-note-images.md#story-1)).
- **Current state (2026-09-24):** `image: force-diagram.png` is stored as-is
  and rendered as `<img src="force-diagram.png">`, which resolves against the
  web page URL and shows broken. The file download
  (`/api/notebooks/{n}/attachments/{id}/content`) is addressed by ID and forces
  a download (`attachment`, octet-stream, `nosniff`), so it cannot serve an
  `<img>` as it is.
- **Key examples:**
  1. `physics/force.md` has `image: force-diagram.png` and
     `physics/force-diagram.png` exists → after publishing, the web note page
     and that note's recall show the diagram; the note keeps its learning
     history.
  2. `image: images/force.png` → shows `physics/images/force.png`.
  3. A notebook reader (including a Bazaar reader) sees the picture; a user who
     cannot read the notebook is refused the picture.
  4. A note with `image: /attachments/images/42/x.png` looks the same as before.
  5. Editing the note's text on the web keeps its `image:` line.
- **Why now (owner, 2026-09-24):** Order kept above story 8 (skip guidance
  folders); story 4 relies on this story's display and reference spelling, and
  story 5 rewrites `image:` to this spelling.
- **Effort hypothesis:** S–M, medium confidence; reuses the existing note image
  display.
- **Depends on:** Delivered web file browsing (story 6).
- **Safe stopping point:** Locally added pictures show on the web; nothing
  about existing images, uploads, or body images changes.

<a id="story-4"></a>

### Use newly web-uploaded note images from a local checkout

- **Identity:** SEED-035#story-4
- **Goal:** Owners continue locally with newly web-authored visual material
  without a separate download-and-relink task.
- **Evaluation:** Upload a note image using an existing web authoring flow, then
  receive the accepted result locally: the note references an available image
  file and its web display still works.
- **Scope / value:** File and note reference appear in one accepted result using
  the same attachment model. Prevent new uploads from recreating the portability
  gap; generic web file upload and new authoring features are not promised.
- **Effort hypothesis:** M, low confidence pending existing upload-flow inventory.
- **Depends on:** Story 6. Earlier image stories reduce uncertainty and effort;
  neither publication direction is inherently prerequisite to the other.
- **Safe stopping point:** New images work in both contexts with common management.

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

The [product backlog](../PRODUCT-BACKLOG.md) owns global order. The size boundary
comes first because it protects accepted storage independently of the later
architecture. Story 13 follows for prevention through an automated publish/fresh-clone
loop. Candidate story 14 applies the completed workflow to existing notebooks and
should precede their image conversion when queued.
This is delivery order, not a reason to withhold browsing from already supported
legacy content. Delivered nested attachment continuity
then enables the browsing journey that requires nested files. Web browsing and
download are delivered. The remaining attachment order covers guidance-folder
assimilation, local visual authoring, new web-image portability, existing-image
conversion (after the two image flows it relies on), web cleanup,
and last the two conveniences whose absence loses nothing: dissolve/merge with
files (story 11), then the rarer cross-notebook folder move (story 10). Root
attachment continuity is also delivered.

The split is by usable placement, not backend/frontend layers or equal slice
counts. Reject a publish-now/preserve-on-web-later split: it would expose accepted
files to loss. Root continuity includes its complete web/checkout loop and
existing snapshot consumers. Nested support stays safe because operations that
would rehome files currently refuse instead of discarding contents. Both use
the same attachment concept.

Image stories retain their existing goals and can use delivered root or nested
file placement. All original promises have an owner in the slice redistribution
recorded in the delivered root-attachment plan
(`.planning/quick/002-notebook-attachment-continuity/PLAN.md` at `6b906462dd`,
section "Redistribution of the original 15 slices").

## Open Refinement Details

- Stories 12–15 carry the size, bundle, receive, and migration outcomes under the accepted
  Git LFS storage contract. Client/protocol and history policy are decided;
  detailed sizing and examples remain story refinement work.
- Story 11 needs story refinement and plan realignment before slice
  refinement/execution.
- Story 8 owns common guidance folder identification and assimilation/ignore
  behavior; the owner deferred those decisions until its refinement.
- Image stories must establish existing ownership, sharing, reference spelling,
  and current presentation contexts before promising a conversion scope.
- File deletion needs an observable outcome for remaining references; warning,
  refusal, dangling-reference presentation, and Trash are different promises.

## When to Surface

Now, under the revised near-future direction. No implementation is authorized.
Ready-made AI skills, arbitrary document previews, Books, file editors, and new
Git integration need their own selected outcomes.

## Breadcrumbs

- Owner direction, 2026-09-20: AI IDEs alongside Web Donut; portable attachments
  and image files; web browsing, download, and deletion.
- Owner clarification: all Markdown retains the existing cohesive behavior,
  with no guidance exception or refinement changes. Remove the format-guarantee
  story; place skipping common guidance folders during assimilation second and
  defer folder/ignore policy to its refinement.
- Owner direction, 2026-09-23: cap attachments at approximately 10 MB, keep
  attachment payloads out of MySQL and ordinary Git history, reduce bundle size,
  and store payloads in bucket-backed object storage. Git commits use a
  Git-LFS-style content-addressed pointer, never a raw GCS URL. The owner accepted
  standard Git LFS and the preserved-history transition, directing concise rules
  into existing ADRs and the Git LFS details into a regular document. Rollout
  belongs in the North Star and stories; implementation remains separate work.
- [Near-future direction](../PRODUCT-BACKLOG.md#near-future-direction).
- [SEED-009](SEED-009-git-backed-local-notebook-workflow.md): prior local/web note
  workflow. This seed owns non-Markdown attachment continuity and guidance-folder
  assimilation selection.
