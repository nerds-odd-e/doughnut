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

<a id="story-13"></a>

### 13. Publish supporting files and acquire a fresh usable checkout
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../quick/019-notebook-lfs-continuity/PLAN.md","assessment":"not-ready","reasons":["Slices 2-3 need representative standard-client and isolated GCS proof; no test target or result is established. Product scope and first plan are refined."],"basis":{"document":"a8e49cb96a19199e7ab541a279f730a36b8315af625440bc64f6cc19f89d2726","plan":"22812a1a5979ccf00c66bc13ad645a7fce9139f9a7994b14e9e3321ab7b32a2c"}}
```

- **Identity:** SEED-035#story-13
- **Slice plan:** [Publish supporting files and acquire a fresh usable checkout](../quick/019-notebook-lfs-continuity/PLAN.md).
- **Resplit trace:** Original “Acquire current supporting files without carrying
  their bytes in Git history” is partitioned between this story and story 15,
  not completed or cancelled. Retain this identity/anchor and first plan.
  Plan 019 maps every original leaf/promise; no implementation or evidence exists.
- **First delivery boundary:** Automated Donut CLI clone and publish on current
  accepted history, followed by fresh acquisition after web/remote changes.
  Story 15 owns receiving into the same checkout and supported local rebase.
  Until then, LFS pull stops before changing refs/files and advises a separate
  fresh clone, preserving the original directory and unpublished work. Legacy
  pull is unchanged. This is an explicit temporary safety boundary, removed by
  story 15; stale publication keeps today's refusal.
- **Alternatives reconsidered:** The owner said no user would use the proposed
  manual whole API/Git-LFS workflow, conditionally accepting it only if it were
  a good split. It fails independent user value here. Keep automated clone and
  publish first, rather than creating storage/API infrastructure stories.
  A fresh CLI clone is the interim receive workflow; web preservation, usable
  files and historical retention cannot be postponed.
- **Owner decisions, current refinement (2026-09-23):** Keep this first among
  untaken work to prevent further binary-history growth before attachment use
  expands. New notebooks only is a progressive delivery boundary, not an
  urgency claim or preference for new users. Ordinary acquisition hydrates only
  the current checkout's attachments; retained historical versions are fetched
  when explicitly needed. Maintain a cohesive design with the taken story 12.
- **Value now / alternatives:** Browsing/download and guidance-folder exclusion
  offer more immediate workflow benefits and do not inherently depend on LFS.
  The owner nevertheless selected prevention first: the per-file guard does not
  bound accumulated binary versions. This is an intentional investment, not a
  measured storage emergency. LFS moves retained payload storage and avoids
  binary-driven Git transfer growth; it does not eliminate storage costs or the
  transfer of current bytes. Existing notebooks benefit through later story 14,
  which remains an unqueued migration candidate, not an implied delivery here.
- **Owner-approved recovery and retention, 2026-09-23:** Every successfully
  published snapshot retains its exact attachment bytes. In a new LFS proposal,
  a new oversized payload occurring only in unpublished intermediate commits
  may be omitted when the tip is valid: a corrective follow-up commit suffices.
  Preserve intermediate commits/pointers and ordinary within-limit versions;
  report omitted content as unavailable on attempted hydration, never substitute
  current bytes. Example: unpublished 20 MiB then 3 MiB versions publish with
  only the latter payload stored; a fresh checkout of the tip succeeds, while
  the intermediate oversized version cannot be fully restored from Donut.
  Replacing/deleting any previously published file never authorizes removing
  its retained bytes. Previously accepted same-notebook payloads remain
  grandfathered. This replaces story 12's strict intermediate-blob rejection
  only when LFS is adopted, including later conversion under story 14.
- **Authorized architectural exception:** Terry explicitly accepted the above
  narrow exception to ADR 0002 and `docs/notebook-git-lfs.md`'s all-intermediate-
  objects availability rule during refinement. It permits omission only of new
  oversized intermediate LFS payloads; commit IDs and successfully published
  snapshots remain preserved. Align the durable LFS contract when implementing
  this story; do not generalize the exception to latest-snapshot-only retention.
- **Goal:** Notebook owners use Donut CLI to publish current images, PDFs, and
  other attachments in a new notebook and acquire a fresh usable checkout,
  without making MySQL storage and Git bundle transfer grow with binary history.
- **Evaluation:** In a new notebook, publish a valid attachment, change it in
  later accepted history, and acquire the notebook in a fresh checkout. The current file is
  available with its exact verified bytes, while the Git history and bundle
  carry content-addressed pointer data rather than either binary version and
  MySQL holds no attachment payload copy. Compare bundles containing several
  incompressible binary versions: they carry pointers rather than payload-sized
  growth. Measure separate LFS downloads honestly; current file bytes still move.
- **Scope / value:** Use the standard Git LFS pointer representation for all
  non-Markdown attachments and bucket-backed immutable object content. Never
  store a raw GCS URL in a commit. Preserve notebook paths, references, access
  control, atomic acceptance, and the common attachment model used by later
  browsing, image, deletion, and folder-operation stories. Local workflows
  require standard Git LFS; Donut's bundle workflow explicitly
  coordinates its object transfers. Include web preservation, metadata/empty-file
  classification, and missing-object failure in the same usable loop. Existing
  notebooks retain current behavior until story 14; no generic web-upload UI is
  added here.
- **Architecture:** Follow the accepted Git LFS storage contract and North Star.
  Client choice is decided; endpoint spelling and implementation details belong
  in slice planning.
- **Cohesion with taken story 12:** Preserve one server-owned attachment
  admission policy at the existing publication boundary, the inclusive
  10,485,760-byte limit, trusted same-notebook content-based grandfathering,
  common Portable-tree classification, and atomic accepted-state changes.
  Evolve the existing admission responsibility for LFS; do not create a second
  synchronization path, client-authoritative size policy, or persistent
  exemption registry. Raw Git measures blob bytes; LFS verifies actual payload
  size and digest, never treating pointer length or a client size claim as
  sufficient evidence. The approved intermediate-content exception changes
  availability/recovery only for LFS; legacy raw-Git admission stays strict.
  Storage identities differ between raw blobs and LFS payloads, but the domain
  rule remains previously accepted content in this notebook. Do not equate a
  Git blob ID with a payload SHA-256 or infer acceptance from mere object-store
  presence. Keep format classification, publication, and projection with their
  existing owners rather than inventing a generic storage framework in advance.
- **Taken-work inspection:** Latest read-only inspection found story 12's three
  slices marked done, published on its branch through `4e04fa3eb5` and now
  integrated with cleanup through trunk `d1dc83a698`. CI completion is not
  asserted here. Admission remains in
  `NotebookGitAttachmentSizeAdmission` and the existing publisher.
  The admission class reuses proposal ancestry, attachment classification,
  accepted-history object identities, and object lengths without loading bytes
  solely for size checks. Reinspect its delivered result before implementation;
  preserve its ownership and behavior, not a frozen class layout.
- **Key examples:**
  - Publish several distinct incompressible attachment versions in a new
    notebook. A fresh acquisition receives exact current bytes, downloads no
    obsolete payload versions by default, and carries pointers in Git history.
    Measure bundle growth separately from LFS traffic; MySQL retains metadata
    and references without attachment payload copies.
  - Explicitly request a previously published snapshot after replacement or
    deletion of its attachment: fetch that snapshot's exact retained bytes,
    never the current version as a substitute.
  - A payload of exactly 10,485,760 bytes is admissible; one additional byte at
    the proposed tip is refused without changing accepted state. Missing or
    corrupt required objects likewise prevent acceptance. Preserve the approved
    20 MiB intermediate / 3 MiB tip correction exception described above.
  - A web note edit preserves unchanged attachment pointers and bytes without
    loading their payloads or contacting GCS to rewrite them. A fresh CLI clone
    receives that accepted result. The old directory and any unpublished work
    remain intact; in-place receive/rebase belongs to story 15.
  - Until story 15, LFS pull refuses before mutation with fresh-clone guidance.
    Publishing from a checkout already based on accepted history still works;
    a stale checkout is refused without overwriting either side.
  - Acquisition configures the authenticated LFS endpoint before hydration.
    Missing Git LFS, failed authorization, or interrupted hydration reports
    incomplete acquisition with actionable recovery; retry can finish without
    losing local work. Pointer text is never reported as a successfully acquired
    attachment. Credentials stay outside authored content.
- **Deferred promises / exclusions:** Story 15 owns in-place receive/rebase.
  Existing-notebook conversion and arbitrary
  legacy binary-history migration, historical bundle shrinking, new web file
  browsing/upload/preview UI, legacy note-image conversion, Book changes,
  aggregate quotas, automatic garbage collection, direct Git hosting, automatic
  history rewriting, and latest-snapshot-only retention. Existing web behavior
  must preserve accepted content; this does not promise sibling stories' new
  presentation features. The ordinary new-notebook workflow is the initial
  example, not a new rejection rule for other naturally valid inputs.
- **Effort hypothesis:** L (2–4 hours), low-to-medium confidence after separating
  in-place receive and removing redundant schema/standalone proof work. Ten
  refined leaves; reassess actual overruns instead of inflating time exceptions.
  Necessary standard-client/GCS work stays inside the usable first outcome.
- **Depends on:** Story 12 supplies the accepted per-file boundary. Existing
  root and nested attachment continuity supplies the paths and user model this
  story changes internally.
- **Safe stopping point:** New attachments publish and fresh-clone through
  Donut CLI with exact bounded content and retained history, even if story 15
  is indefinitely deferred. Web operations preserve them and old local work
  stays intact. Later browsing uses the same attachment model.
- **Refinement outcome:** First replacement story and its ten-slice plan are
  refined. The second plan is mapped only, awaiting its own story refinement.
  Outstanding standard-client/isolated-GCS representative proof is an execution
  readiness concern, not inherited readiness or a remaining user-scope question.
  The owner subsequently authorized keeping/publishing this preparation;
  implementation remains separate.
- **Architecture simplification:** The existing attachment column can retain
  accepted Git representation (legacy raw content or LFS pointer bytes) with
  explicit accessor semantics; inspected consumers are projection/tree/path
  owners. Do not add redundant digest/size columns. Keep one explicit binding
  mode to prevent accidental conversion and preserve accepted metadata through
  both full and derived trees. No extra storage framework or migration here.

<a id="story-15"></a>

### Receive supporting files into an existing working checkout
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"planned","plan":"../quick/020-notebook-lfs-receive/PLAN.md","assessment":"not-ready","reasons":["Awaiting story refinement and mapped-plan realignment before slice-plan refinement or execution."],"basis":{"document":"a8e49cb96a19199e7ab541a279f730a36b8315af625440bc64f6cc19f89d2726","plan":"d3096f3804bb2be6dfc4fd22e12e72ac634d67bdd2ad0d40988341d30cc9a92a"}}
```

- **Identity:** SEED-035#story-15
- **Slice plan:** [Receive LFS files into an existing working checkout](../quick/020-notebook-lfs-receive/PLAN.md).
- **Resplit trace:** Receives original plan 019 leaves 11/12 and the receive
  portion of activation. Mapped plan only: awaiting story refinement — not
  ready for slice-plan refinement or execution.
- **Goal:** Owners continue in the same AI IDE checkout after web/other-checkout
  changes, preserving unpublished work without acquiring another directory.
- **Scope direction:** Reuse story 13's standard setup, content and transfers in
  existing pull/rebase owners. Remove its temporary LFS pull refusal; handle
  equal-head hydration retry and supported local reconciliation. No broader
  merge shapes, binary merge algorithm, new storage model, or migration.
- **Evaluation:** Publish from one checkout, pull into another and get exact
  current files; reconcile a supported unpublished note edit across web changes
  without losing files or work. Failed hydration followed by equal-head retry
  completes files rather than falsely reporting unchanged success.
- **Value now:** Completes frequent parallel local/web work. Fresh acquisition
  remains a usable fallback, so this follows web retrieval in the queue rather
  than automatically inheriting the old story's first place.
- **Effort hypothesis:** M (1–2 hours), low confidence pending failure/recovery
  examples. Three provisional leaves may consolidate during refinement.
- **Depends on:** Story 13. Browsing is not a technical prerequisite.
- **Safe stopping point:** Existing-checkout continuity works through the same
  content/admission model; migration and visual authoring remain separate.
- **Refinement remaining:** Post-rebase hydration failure recovery, dirty/readiness
  checks during retry, and proof for supported receive-result branches. Resume
  with story refinement then plan realignment before slice-plan refinement or
  execution. No inherited readiness.

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

### Browse and download local supporting files in Web Donut

- **Identity:** SEED-035#story-1
- **Goal:** Owners away from a checkout can find and retrieve its non-Markdown
  supporting files without acquiring the entire repository.
- **Evaluation:** Given accepted attachments, including nested JSON and image
  files, browse their folder locations and download the exact accepted bytes.
  A folder containing only attachments remains discoverable.
- **Scope / value:** One find-and-retrieve journey for attachments regardless
  of origin. Compliant Markdown guidance uses existing note/Readme presentation.
  No attachment text editor, image preview, or dedicated IDE screen is promised.
- **Effort hypothesis:** M, medium confidence assuming existing folder navigation.
- **Depends on:** Stories 6 and 9; the promised browsing includes nested files.
- **Safe stopping point:** Web retrieval works while file management stays local.

<a id="story-5"></a>

### Access existing note images as notebook folder files

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
- **Depends on:** Story 6. Stories 1, 3, and 4 are not user prerequisites.
- **Safe stopping point:** Existing images work locally and on the web even if
  later image-authoring flows are deferred. Preserve the only accessible copy
  throughout transition; avoid a separate permanent image-file model.

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
- **Depends on:** Story 6. Story 5 may supply reuse, not a user prerequisite.
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
loop. Resplit story 15 follows browsing: existing-checkout continuity strongly
supports parallel AI work, while fresh cloning is a safe interim receive path.
Candidate story 14 applies the completed workflow to existing notebooks and
should precede their image conversion when queued.
This is delivery order, not a reason to withhold browsing from already supported
legacy content. Delivered nested attachment continuity
then enables the browsing journey that requires nested files. The remaining
attachment order covers browser retrieval, existing images, guidance-folder
assimilation, local visual authoring, new web-image portability, web cleanup,
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
