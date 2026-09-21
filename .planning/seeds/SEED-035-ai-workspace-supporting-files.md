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

The [North Star](../NORTH-STAR.md) owns the concise direction and the
[synchronization contract](../../docs/notebook-git-synchronization.md#attachments-in-the-portable-tree)
owns details. Ordinary local rebase reconciles unpublished work against the
latest accepted history; the remote never merges or rebases. These stories
introduce no new conflict-recovery or transport policy. Story 8 owns the selected
folder-based assimilation outcome; its detailed behavior is not decided here.

## Story Decomposition

S = 30–60 minutes, M = 1–2 hours, L = 2–4 hours, including delivery. Estimates
are hypotheses; refine/split work that exceeds L before execution planning.
Root-file and nested-file continuity are delivered. Stories 11 and 10 own the
dissolve/merge and cross-notebook operations that the delivered behavior
refuses for now. The [product backlog](../PRODUCT-BACKLOG.md) owns global order.
No executable plan or implementation is authorized by this seed.

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

The [product backlog](../PRODUCT-BACKLOG.md) owns global order. Delivered nested
attachment continuity enables the browsing journey that requires nested files.
The remaining attachment order then covers browser retrieval, existing images,
guidance-folder assimilation, local visual authoring, new web-image portability,
web cleanup, and last the two conveniences whose absence loses nothing:
dissolve/merge with files (story 11), then the rarer cross-notebook folder move
(story 10). Root attachment continuity is also delivered.

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
- [Near-future direction](../PRODUCT-BACKLOG.md#near-future-direction).
- [SEED-009](SEED-009-git-backed-local-notebook-workflow.md): prior local/web note
  workflow. This seed owns non-Markdown attachment continuity and guidance-folder
  assimilation selection.
