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

<a id="story-1"></a>

### Browse and download notebook files in Web Donut
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../quick/022-browse-download-notebook-files/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"38a4b4da14718395f3cf2bb428758b906b03bf4b0c0ba0713af4d7a0227a7e3f","plan":"1abf048548894e2aafd1d606bad45e2f15593d4eaedd6bd794ef7990e5532599"}}
```

- **Identity:** SEED-035#story-1
- **Slice plan:** [Browse and download notebook files](../quick/022-browse-download-notebook-files/PLAN.md).
- **Goal:** Anyone reading a notebook in Web Donut sees the non-Markdown files
  it holds, where they are, and can download any of them. Today these files
  are accepted, stored and carried through web folder operations, yet the web
  shows none of them: a folder holding only a PDF looks empty, and a permanent
  folder deletion removes files the owner cannot see. Visibility is the primary
  value; downloading without a checkout (another device, no CLI) is the second.
- **Scope:**
  - Files appear in the notebook sidebar tree at their folder location or the
    notebook root, alongside notes and subfolders.
  - Selecting a file opens its attachment page showing its filename and size,
    with a download of the exact accepted bytes under the exact filename,
    whichever way the notebook stores them (raw Git content or Git LFS).
  - Access follows the notebook's existing read authorization: whoever can read
    the notebook can browse and download its files. No owner-only rule.
  - Every non-Markdown attachment is an ordinary file of unknown format here,
    with no special case: a root `.keep` (usually empty) and any other dot-file
    are listed and downloadable like the rest.
  - Files in a trashed folder behave the same as files anywhere else.
  - Downloads are always served as a download, never rendered inline in Donut's
    origin, so an uploaded SVG or HTML file cannot run script in the page.
  - If the bytes behind an accepted LFS pointer are unavailable, the download
    fails loudly with a clear error ([ADR 0006](../../docs/adrs/0006-failure-handling-accepted.md));
    the pointer text is never served as if it were the file.
- **Deferred promises:** image, PDF or text preview; a file editor; upload,
  rename, move and delete (story 2 owns delete); earlier file versions; folder
  ZIP download (notebook export already exists); file search; showing files in
  places other than the sidebar and attachment page.
- **Key examples:**
  - `physics/force.png` and `physics/data/run.json` are accepted, and `physics`
    has one note. The sidebar under `physics` shows the note, `force.png` and a
    `data` folder; expanding `data` shows `run.json`. Selecting `run.json` opens
    its page; downloading gives the accepted bytes named `run.json`.
  - `refs/` holds only `paper.pdf`. The sidebar shows `refs` with `paper.pdf`
    inside, not an empty folder.
  - A new LFS notebook has `diagram.png` at its root. The download returns the
    image bytes (matching the pointer's SHA-256), not the pointer text.
  - A subscriber to a Bazaar notebook sees and downloads its files just as they
    read its notes; a user without read access to the notebook gets neither.
  - `drawing.svg` is downloaded as a file; opening its download link does not
    render it inside Donut.
  - A root `.keep` of zero bytes appears and downloads like any other file.
- **Git metadata stays hidden (owner decision 2026-09-24):** `.gitattributes`
  is reserved Git metadata kept outside the attachment rows
  ([North Star](../NORTH-STAR.md#use-standard-git-lfs-end-to-end)), and nested
  `.keep` files are generated empty-folder markers. Like `.git`, neither is
  listed on the web; they stay visible in local checkouts. This keeps one
  row-backed file model instead of a second, read-only source from the Git
  tree. Web deletion (story 2) therefore cannot reach `.gitattributes`.
- **Effort hypothesis:** M, medium confidence; the sidebar listing cache and
  the two byte representations are the main unknowns.
- **Depends on:** Delivered root and nested attachment continuity and the
  delivered LFS content store.
- **Safe stopping point:** Files are visible and retrievable on the web while
  file management stays local.

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
- **Related finding, owner decision pending:** `GET /attachments/images/{id}/{fileName}`
  has no authorization check (the security configuration permits all but
  `/login/continue`) and ignores the filename, and image IDs are sequential,
  so images from private notebooks can be enumerated. Whether to queue a
  separate bug item is undecided; this story's conversion would not remove
  the exposure while legacy rows remain.

<a id="story-3"></a>

### See locally added image files in Web Donut notes

- **Identity:** SEED-035#story-3
- **Goal:** Learners use locally collected diagrams when returning to a note in
  Web Donut without uploading them again.
- **Evaluation:** Publish an image and a note reference authored locally; open
  the note on the web and see that image, retaining the note's learning history.
- **Scope / value:** Notebook-local image references in note content complete the
  local visual-authoring handoff. Preserve existing image presentation; no new
  recall modes, image prompts, or question-generation behavior is promised.
- **Effort hypothesis:** M, low confidence until reference spelling and current
  presentation contexts are established.
- **Depends on:** Story 6. Story 5 depends on this story's reference spelling
  and web display, not the reverse.
- **Safe stopping point:** Local diagrams work in notes without new web-upload
  portability. Downloadable bytes do not imply every image format renders.

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
then enables the browsing journey that requires nested files. The remaining
attachment order covers browser retrieval, guidance-folder assimilation, local
visual authoring, new web-image portability, existing-image conversion (after
the two image flows it relies on), web cleanup,
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
