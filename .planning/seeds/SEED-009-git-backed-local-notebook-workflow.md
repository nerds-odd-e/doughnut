---
id: SEED-009
status: active
planted: 2026-09-04
planted_during: ADR 0002 v1 discussion
trigger_when: when selecting the next Git-backed notebook workflow story from the product backlog
scope: large
---

# SEED-009: Refine a Donut notebook locally with Obsidian and AI-enabled IDEs

## Why This Matters

For a notebook owner using Obsidian or an AI IDE alongside Donut, supported
local refinement and web editing should form a continuous workflow without
manual copying, lost work, or learning history assigned to the wrong note.

Clone, content publish/pull, local additions/deletion/reorganization, bounded
content rebase, ordinary web note creation, receiving one accepted addition
beside one unpublished local content edit, keeping that edit across
creation-then-save, publishing related edits to several existing notes, and
keeping a related two-note local revision across one disjoint web save of a
third note are delivered. The remaining problem is unsupported everyday changes
interrupting that loop; a cleaner Git log alone does not close it. The delivered
scope below is planning evidence, not a fresh implementation audit or evidence
of real-user frequency.

The product constraints established in the discussion are:

- The working tree follows Accepted ADR 0004 and contains no Donut IDs,
  manifests, sidecar files, or local database.
- V1 uses one dedicated repository per notebook, rooted at `/`, with one
  linear `main`. Published history is never rewritten; unpublished local work
  is rebased.
- Direct use of standard `git clone`, `git fetch`, and `git push` against Donut
  is a later capability. V1 may require the Donut CLI to obtain and synchronize
  the repository, but Git commits, trees, refs, and rebase remain the
  synchronization model. Minimal binding/authentication data may live in
  ordinary Git configuration or the normal credential store, never in the
  Portable tree.
- Every existing notebook is automatically Git-backed during one fleet
  migration; there is no owner opt-in or clone-triggered persistence mode.
  Each existing notebook receives one root commit containing its canonical
  tree at cutover. Earlier history is not fabricated. New notebooks are
  Git-backed from creation.
- The accepted Git `main` is authoritative for Portable content after cutover.
  MySQL remains the current application projection and the authority for
  Donut-only identity-bound data.
- Every accepted Donut web change is represented by a commit. V1 has no Donut
  historical-checkout or revert UI.
- Unsafe identity conclusions and unresolved content conflicts stop before
  advancing remote `main`.
- A later notebook-to-subdirectory project-repository binding must remain
  architecturally possible, but it is outside this v1 decomposition.

## Alternatives and Decision

1. **Defer broader reconciliation:** keep broader batch rebase unqueued.
   On 2026-09-08 the owner selected folder-move divergence and web autosave
   batching for the backlog; Story 15 is now delivered.
2. **Smaller behavior change:** keep a two-note local revision across one web
   save of a third existing note (Story 18) — now delivered. Creation followed
   by one save is already supported for one local edit; extending batches across
   structural history is not required for that outcome.
3. **Edits-only multi-note publish:** delivered as Story 14, independent of
   receiving additions. Story 11 already publishes mixed additions and edits.
4. **Complete synchronization:** defer the broad contract. The owner also
   values readable history, so Story 10 remains queued after delivered Story 15.
   Batching already-accepted saves risks conflicting with immutable history;
   its publication boundary must be clarified before planning it.

**Priority:** Stories 13, 16, 14, 17, 18, 18a, and 15 are delivered. Web autosave
batching (10) remains queued after concurrent worktree isolation as the global
backlog lead. This is not a claim that usage data proves these are the most
frequent failures.

The estimates are comparative story hypotheses without implementation inspection:
S = 30–60 minutes, M = 1–2 hours, L = 2–4 hours. They are not commitments.

## Story Decomposition

<a id="story-1"></a>

### 1. Open an existing Donut notebook in Obsidian and an AI IDE

**Status:** delivered.

- **Goal:** Open an existing notebook as an ordinary local Git repository in
  Obsidian or an AI IDE.
- **Scope:** CLI-assisted acquisition of its canonical Portable tree and
  accepted history; automatic Git backing, no invented pre-cutover history or
  metadata in the tree.

<a id="story-2"></a>

### 2. Publish a local content edit to the same Donut note

**Status:** delivered.

- **Goal:** Publish a local content edit onto the same Donut note and keep its
  learning data.
- **Scope:** One direct-child commit editing one existing ordinary Markdown note
  at an unchanged path; atomic accepted history/projection update. Structural
  and stale proposals remain outside this content operation.

<a id="story-3"></a>

### 3. Receive a Donut web edit in a clean local repository

**Status:** delivered.

- **Goal:** Receive accepted web content edits without copying files or cloning
  again.
- **Scope:** Changed durable saves of existing ordinary-note body/frontmatter at
  unchanged paths create immutable commits when the current Portable projection
  matches accepted main. Clean bound main checkouts can fast-forward accepted
  history, including accepted structural changes. Ordinary web note creation is
  delivered in Story 13. Web deletion/rename/move and notebook/folder README
  edits are not synchronized; existing projection drift is neither absorbed nor
  repaired.

<a id="story-4"></a>

### 4. Create a new note locally

**Status:** delivered.

- **Goal:** Create a new Donut note from a locally authored Markdown file.
- **Scope:** One addition at the root or an existing represented folder, with
  fresh identity and valid authored Portable content. No implicit parent
  creation, README authoring, attachments, or identity copied from matching
  text. Story 11 extends additions to batches.

<a id="story-5"></a>

### 5. Delete a note locally without transferring its private data

**Status:** delivered.

- **Goal:** Remove a note locally without transferring its private data.
- **Scope:** One isolated ordinary-note deletion soft-deletes the same identity
  and deactivates its trackers; private associations and earlier Git history
  remain. Referring links remain authored. Empty Donut containers survive. A
  later addition at another available path gets fresh identity; deleted-path
  reuse and restore remain excluded.

<a id="story-6"></a>

### 6. Rename a note without losing its learning history

**Status:** delivered.

- **Goal:** Rename a note in place while retaining its identity and learning
  history.
- **Scope:** Exactly one removed/added ordinary Markdown pair with identical
  bytes in the same parent, and no other changes. Validate the final
  destination, including deleted-name reservations. Preserve authored links;
  old-path references may stop resolving. Accept the rename before a separate
  content edit. No similarity-based identity inference or rename-with-edit.

<a id="story-7"></a>

### 7. Move a folder while preserving descendant identities

**Status:** delivered.

- **Goal:** Move an existing folder within its notebook while retaining folder
  and descendant identities and learning history.
- **Scope:** One same-name parent relocation of a complete
  byte/mode/relative-path-preserving subtree with its own accepted README; every
  active descendant folder must be represented. Destination is the root or an
  existing represented parent. No new parents, merging, cycles, unrepresented
  empty descendants, source without README, folder rename, or accompanying
  edits. Links remain authored. A one-file move only establishes a note move,
  not folder intent. Accept the move before a separate edit; divergent pull
  across the move remains unsupported.

<a id="story-8"></a>

### 8. Keep non-overlapping accumulated local and web changes

**Status:** delivered.

- **Goal:** Keep one local content edit when accepted web edits change other
  notes.
- **Scope:** Pull rebases exactly one unpublished single-parent commit editing
  one existing ordinary note over linear existing-note content-only history;
  every accepted edge must qualify. Pull never publishes. The rebased edit is
  explicitly published onto the same identity. No multiple local commits/notes,
  structural edges, automatic stash, forced update, or drift repair. Story 9
  extends this to same-note overlap. A later accepted addition of a different
  note is a structural edge; receiving one such commit while unpublished local
  work exists is delivered in Story 16. Creation followed by another web save
  remains a multi-commit interval (Story 17), not this content-only rebase.

<a id="story-9"></a>

### 9. Resolve an overlapping edit with ordinary Git

**Status:** delivered.

- **Goal:** Reconcile same-note local and accepted web edits with ordinary Git
  while keeping the learned note.
- **Scope:** The one-local-commit/one-note content-only boundary of Story 8 also
  permits same-path edits. Let Git merge; pause on actual conflicts with native
  continue/abort and recoverable local work. Pull/publish refuse unfinished Git
  operations; later publication validates the resolved content normally. An
  absorbed patch may leave no unpublished work. No divergent structural history,
  new conflict UI, identity inference, or drift repair.

<a id="story-10"></a>

### 10. See one stable commit for one continuous web edit

**Status:** queued; refinement deferred until selection.

- **For / why:** An owner reading Git history wants a meaningful editing unit
  rather than one commit per durable autosave.
- **Evaluation:** Continuous same-note saves before publication form one
  stable commit; another accepted commit ends the batch. Once visible to any
  client, a commit ID never changes. Structural changes also end the batch.
- **Value / learning:** Tests whether more readable history materially helps
  owners and AI tools. One-note content pull handles several accepted saves;
  two-note batch pull accepts exactly one disjoint save. Story 18a corrects
  refusal guidance without broadening that limit. Evaluate readability
  separately from receiving multiple accepted saves beside a local batch;
  do not use batching to hide the limit or rewrite already accepted commits.
- **Effort hypothesis:** M — low confidence; assumes an explicit end-of-edit
  and publication boundary can preserve durable saves and timely synchronization.
- **Depends on:** delivered Story 3.
- **Safe stopping point:** All synchronization remains useful without this;
  keep extra immutable commits if batching would rewrite accepted history.
- **Boundary:** History granularity only; no structural sync or broader rebase.
  Define when a batch ends and becomes available before planning this story.
  Story 13 now appends one accepted commit at ordinary web creation and another
  for each later durable save, so sequential-loop history is noisier; that is
  not by itself a reason to select this story.

<a id="story-11"></a>

### 11. Publish several note changes in one commit

**Status:** delivered.

- **Goal:** Publish related note additions and accompanying edits as one
  authored commit.
- **Scope:** One direct-child commit containing several additions, optionally
  with existing-note edits at unchanged paths, at root or represented folders.
  Accept all or none; additions get fresh identities and edits retain private
  data. Edits-only batches are delivered separately in Story 14. Delete/move
  mixtures, new folders, README changes, multiple unpublished commits, and
  divergence remain excluded.

<a id="story-12"></a>

### 12. Move a note between existing folders without losing its learning history

**Status:** delivered.

- **Goal:** Move a learned note to another existing folder without restarting
  its learning history.
- **Scope:** Exactly one byte-identical removed/added pair changes parent,
  optionally filename, within the notebook; no accompanying changes. Destination
  is root or an existing represented folder. Validate the final location/name,
  preserve source/destination containers and authored links, and accept before a
  separate content edit. No new/unrepresented parents, overwrite, deleted-path
  reuse, folder inference, or divergent relocation.

<a id="story-13"></a>

### 13. Create a note on the web and continue refining it locally

**Status:** delivered. Recover quick/062 from `4976553c20`.

- **Goal:** Capture a new ordinary note through Donut's web creation flow, then
  refine that same identity locally and publish it without copying or blocking
  synchronization.
- **Scope:** Matching accepted Portable tree; one ordinary note at the root or
  an existing represented folder (nested/README-only allowed). Title-only or
  valid initial Markdown; creation and Git acceptance are atomic. Later ordinary
  web saves, clean-main pull, one local content commit, and explicit publish
  retain identity and private learning data. Unrepresented destinations,
  Wikidata/relationship/assisted creation, earlier drift, and divergent receipt
  across a creation followed by another web save stay unsynchronized. One
  accepted addition beside one unpublished local content edit is Story 16.
  No new UI or API shape.

<a id="story-14"></a>

### 14. Publish a related batch of edits to existing notes

**Status:** delivered.

- **Goal:** A notebook owner refining related notes in Obsidian or an AI IDE
  can publish the revision together, without creating a dummy note or splitting
  the edits into separate publications, while keeping each note's learning
  history.
- **Scope:** Use the existing CLI publication flow for exactly one unpublished
  single-parent commit directly on accepted `main`, editing two or more existing
  ordinary Markdown notes in one notebook. Paths stay unchanged, at the root or
  in existing represented folders; edits use the already-supported valid body
  and frontmatter. Clone success next-steps describe that one such commit may
  edit one or more existing ordinary notes at unchanged paths, with the current
  structural and pull limits. The starting Donut projection must match accepted
  history, and remote `main` must not have advanced. Accept the authored commit
  and all note edits together, preserving each identity and private learning
  data. Another clean checkout can receive the accepted batch through ordinary
  pull. Invalid input or a stale head refuses the whole publication, preserving
  local work and leaving remote history and notes unchanged by the attempt.
  Excludes additions, deletions, renames, moves, folder/README changes,
  multiple unpublished commits, divergent batch rebase, or drift repair. No new
  UI, commands, or identity policy. Delivered additions-and-edits publication
  remains Story 11; receiving web additions beside unpublished work remains
  Stories 16–17. A previously web-created note is simply an existing note here.

<a id="story-15"></a>

### 15. Keep a local note edit across an accepted folder move

**Status:** delivered.

- **Goal:** A notebook owner keeps one committed local note refinement when
  another checkout publishes a move of its containing folder, then publishes
  the refinement onto the same learned note at its new path.
- **Scope:** Clean bound `main`; exactly one unpublished single-parent commit
  changing the valid body/frontmatter of one existing ordinary note. Accepted
  history since that commit's parent is exactly one supported, same-name,
  README-backed folder relocation. Its complete subtree retains every relative
  path, byte, and file mode; nothing else changes. The edited note is inside
  that subtree, including a represented nested folder. Destination is the root
  or an existing represented parent, as in Story 7. Pull retains the edit at
  the uniquely mapped new path as one unpublished child of accepted history.
  Explicit publication uses the existing content-publication contract and keeps
  the same note identity and private learning data. Accepted commits remain
  immutable; authored links remain unchanged. No new command, UI, API, stored
  identity metadata, or general structural merge policy. Refuse unsupported or
  ambiguous correspondence before changing the checkout; work is preserved and
  must be reconciled or recreated as one supported commit on accepted history
  before publication. Excludes multiple local commits/notes or accepted
  commits/moves, accompanying remote content changes, edits outside the moved
  subtree, folder rename, note-only relocation, README edits, new parents,
  unrepresented descendants, collisions, deletion, dirty trees, drift repair,
  link rewriting, automatic publication, and autosave batching (Story 10).

<a id="story-16"></a>

### 16. Keep a local note edit when accepted history adds a different note

**Status:** delivered.

- **Goal:** A notebook owner can receive a new web-created note while keeping one
  committed, unpublished local refinement, then publish that refinement without
  losing either note's identity or learning history.
- **Scope:** Bound clean `main` with exactly one unpublished single-parent
  content edit of one existing ordinary note; accepted history since the shared
  base is exactly one single-parent commit adding one different ordinary note at
  root or an already represented folder. Pull receives the addition and retains
  the unpublished edit; explicit publish updates the original edited note and
  preserves both identities and private learning data. Excludes dirty trees,
  multiple local/remote commits or additions, accompanying remote edits,
  collisions, moves, renames, deletes, README/new-folder changes, drift repair,
  and creation followed by another web save.

<a id="story-17"></a>

### 17. Keep a local note edit when a web-created note is then saved

**Status:** delivered. Recover quick/065 from `1675d58cb9`.

- **Goal:** An owner with one committed local content refinement wants to
  receive a note they captured on the web and immediately continued writing
  before pulling, then publish the local refinement onto the same learned note.
- **Scope:** One unpublished local existing-note content commit. Accepted
  interval is exactly one ordinary-note addition of a different note (root or
  already represented folder) plus one content save of that same new note at
  its unchanged path. Pull retains the local edit and receives the saved note;
  explicit publish updates the original edited note and preserves both
  identities and private learning data. Excludes additional remote commits,
  accompanying edits to other notes, two independent additions, new folders,
  collisions, moves, deletes, README changes, dirty trees, multiple local
  commits, and drift repair.

<a id="story-18"></a>

### 18. Keep a related local edit batch when the web changes a different note

**Status:** delivered. Recover quick/066 from `fc9bc7a477`.

- **Goal:** An owner can retain one related local revision when a web edit to
  a different note arrives, then publish the revision without splitting it.
- **Scope:** One unpublished single-parent commit edits two existing ordinary
  notes at unchanged root/nested paths; accepted history advances by exactly
  one content-only save of a third existing note. Pull retains both local edits
  over the accepted commit; explicit publish keeps all three note identities
  and learning data. No overlap, additions, moves, README changes, multiple
  local/remote commits, dirty trees, or drift repair. Already-based batch pull,
  including repeating pull after a successful rebase, remains excluded;
  direct-child batch publication is supported independently by Story 14.

<a id="story-18a"></a>

### 18a. Understand how to proceed when batch pull refuses divergent history

**Status:** delivered. Recover quick/068 from `1945a62286`.

- **Goal:** A notebook owner whose two-note batch cannot receive accepted history
  gets accurate next-step guidance rather than a suggestion to publish a stale
  commit that publication will reject.
- **Scope:** Correct the batch-specific refusal guidance. Explain that the local
  work is preserved and must be reconciled or recreated as one supported commit
  directly on accepted history before publication. No automatic recovery, broader
  pull eligibility, new commands, or change to publication validation.

## Ordering and Scope Reduction

The [product backlog](../PRODUCT-BACKLOG.md) owns global priority. Keep the
existing SEED-015 browser-workflow stories ahead of this seed; this reassessment
selects notebook-workflow priorities and does not displace that direction.

Within this seed, **13**, **16**, **14**, **17**, **18**, **18a**, and **15**
are delivered. Queue **10** after the existing worktree priorities. Story 10
remains separately valuable to the owner for history readability, with its
publication boundary still to refine.

First-to-drop order among remaining items: **10**. Delivered batch
publication and batch reconciliation across one disjoint web save stay useful
even if broader reconciliation is deferred.

Preserve these boundaries in future refinement:

- Accepted history is immutable; pull receives accepted history, never repairs
  unsynchronized web state or publishes automatically.
- Commit boundaries distinguish deletion/new identity from same-identity moves.
  Missing files do not establish missing Donut containers or identity intent.
- Authored links remain unchanged during supported structural publication.
- Ordinary Git handles content conflicts; ambiguous identity must still stop.
- Web deletion/rename/move, earlier projection-drift recovery, folders without
  README, new parents, deleted-path reuse, and multiple unpublished commits
  remain outside this selection. Completing the queue is not full ADR 0002.

## Open Decisions

- **Priority assumption open to revision:** no real-user frequency evidence
  ranks folder divergence or history readability. Do not displace the
  developer's concurrent-worktree priority.
- **Story 10, before planning:** define the end-of-edit/publication boundary
  and acceptable delay without losing durable saves or rewriting visible history.
- **Batch follow-ons:** Repeated pull, three-or-more-note rebase, and multiple
  accepted saves are distinct excluded outcomes, not delivered by Story 18.
  Capture the blocking owner journey before selecting a broader batch story;
  passing batch publication alone does not prove divergent pull support.
- **Earlier drift:** Story 13 prevents a new source of drift only from matching
  state. Recovery for notebooks already out of sync remains an unselected need.

## When to Surface

Story 15 is delivered. Refine Story 10 when it reaches selection for planning.
This seed is non-executable; queue selection does not authorize implementation.

## Breadcrumbs

- Proposed
  [ADR 0002 — Git-native Portable notebook tree synchronization](../../docs/adrs/0002-git-native-portable-notebook-synchronization.md)
  supplies the Git authority, linear `main`, identity, conflict, web-commit,
  history-retention, and future subtree-binding direction. It remains Proposed
  and is not binding.
- Accepted
  [ADR 0004 — OKF-compatible notebook Markdown profile](../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
  defines the Portable notebook tree that every root and later commit contains.
- Accepted
  [ADR 0001 — Ubiquitous language](../../docs/adrs/0001-ubiquitous-language.md)
  defines **Notebook**, **Note**, **Folder**, **Portable notebook tree**, and
  **Portable path**.
