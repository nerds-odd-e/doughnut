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
content rebase, and ordinary web note creation into the sequential loop are
delivered. The remaining problem is unsupported everyday changes interrupting
that loop; a cleaner Git log alone does not close it. Story 13 made one
interruption live: pull still refuses a plain accepted addition when unpublished
local work exists.
The delivered scope below is planning evidence, not a fresh implementation audit
or evidence of real-user frequency.

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

1. **Defer:** keep the delivered sequential loop and require a clean checkout
   before pulling a web-created note. Usable, but it forces the owner to publish
   or stash local work before receiving a capture made in Donut.
2. **Smaller behavior change:** keep one unpublished local content edit when
   accepted history adds a different ordinary note. Recommended next: it removes
   the interruption Story 13 just enabled, without opening folder-move
   divergence or commit batching.
3. **Edits-only multi-note publish:** still valuable for related local
   revisions, and independent of receiving additions. Story 11 already publishes
   mixed additions and edits; this is convenience, not the newly live block.
4. **Complete synchronization or web-commit batching:** defer the broad contract
   and readability polish. Neither is the smallest way to remove the next
   concrete interruption; batching already-accepted saves risks conflicting
   with immutable history unless its publication boundary is clarified.

**Priority hypothesis:** Story 13 (web capture in the sequential loop) is
delivered. Next selected remaining story is keeping a local content edit across
one accepted addition (Story 16), then a related local edits-only batch
(Story 14). This follows the established beneficiary and two-way workflow goal.
It is not a claim that usage data proves these are the most frequent failures.

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
  note is a structural edge; receiving it while unpublished local work exists
  remains Story 16, not this delivered content-only rebase.

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

**Status:** deferred; not queued.

- **For / why:** An owner reading Git history wants a meaningful editing unit
  rather than one commit per durable autosave.
- **Evaluation:** Continuous same-note saves before publication form one
  stable commit; another accepted commit ends the batch. Once visible to any
  client, a commit ID never changes. Structural changes also end the batch.
- **Value / learning:** Tests whether more readable history materially helps
  owners and AI tools. Existing pull already handles several accepted saves.
- **Effort hypothesis:** M — low confidence; assumes an explicit end-of-edit
  and publication boundary can preserve durable saves and timely synchronization.
- **Depends on:** delivered Story 3.
- **Safe stopping point:** All synchronization remains useful without this;
  keep extra immutable commits if batching would rewrite accepted history.
- **Boundary:** History granularity only; no structural sync or broader rebase.
  Define when a batch ends and becomes available before selecting this story.
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
  data. Edits-only batches, delete/move mixtures, new folders, README changes,
  multiple unpublished commits, and divergence remain excluded.

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

**Status:** delivered.

- **Goal:** Capture a new ordinary note through Donut's web creation flow, then
  refine that same identity locally and publish it without copying or blocking
  synchronization.
- **Scope:** Matching accepted Portable tree; one ordinary note at the root or
  an existing represented folder (nested/README-only allowed). Title-only or
  valid initial Markdown; creation and Git acceptance are atomic. Later ordinary
  web saves, clean-main pull, one local content commit, and explicit publish
  retain identity and private learning data. Unrepresented destinations,
  Wikidata/relationship/assisted creation, earlier drift, and divergent receipt
  across additions stay unsynchronized. No new UI or API shape.

<a id="story-14"></a>

### 14. Publish a related batch of edits to existing notes

**Status:** queued.

- **For / why:** An owner using an AI IDE revises several related notes and
  wants to publish the coherent revision without manufacturing a new note or
  splitting the work into separate publications.
- **Evaluation:** With no remote advance, commit content edits to two or more
  existing ordinary notes at unchanged paths in one local commit. Publish and
  see the complete revision in Donut on the original identities; another clean
  checkout receives it through pull.
- **Value / learning:** Makes existing-note refinement usable as one related
  change. Tests the value of multi-note authoring before multiple-commit history
  or concurrent multi-note conflict policies.
- **Effort hypothesis:** M — low confidence; assumes the delivered mixed
  additions/edits behavior can extend to edits-only batches without new identity
  policy. No code audit was used to estimate this.
- **Depends on:** delivered Stories 2 and 11; independent of Story 13.
- **Safe stopping point:** A sequential multi-note revision is useful on its
  own. Accept all edits or none; invalid input or a stale head retains local
  work, accepted history, and all remote note identities/private data.
- **Boundary:** One direct-child commit, matching starting projection, valid
  body/frontmatter edits at existing root or nested note paths. No structural
  edits, README changes, multiple unpublished commits, or divergent batch rebase.
  Existing additions-and-edits behavior remains separate delivered scope.
  Reminder from Story 13: publishing a locally refined web-created note uses the
  ordinary publication path; do not add a special identity route. Receiving an
  accepted addition while unpublished local work exists is Story 16, not this
  edits-only batch.

<a id="story-15"></a>

### 15. Keep a local note edit across an accepted folder move

**Status:** candidate; not queued.

- **For / why:** An owner with one committed local content refinement wants to
  retain it when accepted history relocates the folder containing that note.
- **Evaluation:** Accepted history contains one supported exact folder move;
  pull reconciles one unpublished content edit to a descendant at its new path,
  then explicit publication updates the same learned note. An ambiguous
  correspondence stops with recoverable local work.
- **Value / learning:** Tests the first interaction between delivered folder
  reorganization and local content divergence. A clean fast-forward already
  works; this story is specifically for work committed before the move.
- **Effort hypothesis:** L — low confidence; assumes a single exact accepted
  relocation supplies sufficient correspondence. Revisit size and selection if
  preserving identity needs broader structural conflict policy.
- **Depends on:** delivered Stories 7–9; no dependency on Stories 13–14.
- **Safe stopping point:** This narrow reconciliation remains useful without
  arbitrary structural merges. Preserve accepted commit IDs and private identity;
  never discard local work or infer identity from content similarity alone.
- **Boundary:** One local note/content commit and one accepted README-backed
  folder relocation with unchanged bytes. No concurrent content change on the
  remote side, rename-with-edit, deletion, multiple moves, README authoring,
  unrepresented descendants, or general structural conflict resolution.
  A plain accepted addition is a different structural edge (Story 16); do not
  fold that receipt into this folder-move story.

<a id="story-16"></a>

### 16. Keep a local note edit when accepted history adds a different note

**Status:** queued; refined.

**Goal:** A notebook owner can receive a new web-created note while keeping one
committed, unpublished local refinement, then publish that refinement without
losing either note's identity or learning history.

**Scope:**

- Start from a bound `main` checkout with a clean working tree and exactly one
  unpublished single-parent commit editing the body/frontmatter of one existing
  ordinary note at its unchanged path. The remote projection matches accepted
  history.
- Since the shared base, accepted history contains exactly one new commit,
  adding exactly one different ordinary note at the root or an existing
  represented folder, with no accompanying edits.
- Pull receives the added note and rebases the local edit, keeping it
  unpublished. Accepted commit IDs remain unchanged. A later explicit publish
  updates the original edited note; both notes retain their own identities and
  private learning data.
- Exclude uncommitted edits/automatic stash, multiple local commits or edited
  notes, multiple remote commits or additions, accompanying remote content
  edits, same-path collisions, moves, deletes, renames, README changes, new
  folders, and projection-drift repair. Existing supported content-only rebase
  remains as delivered; this adds no general structural reconciliation.

**Key examples:**

1. From a shared base containing `Alpha.md`, commit a local body edit to Alpha;
   create `Beta.md` on the web in one accepted commit → pull → Beta appears
   locally and Alpha retains its local edit in an unpublished commit. Donut's
   Alpha is still unchanged. The same receipt works when Beta is created in an
   existing represented folder.
2. After that pull → explicitly publish → Donut's original Alpha contains the
   refinement and keeps its learning history; Beta's content, identity, and
   private data are unchanged.
3. With the same local edit, accepted history adds Beta and also renames another
   note → pull → refuse this unsupported divergence, retaining local work and
   leaving accepted history unchanged. Likewise, uncommitted local edits still
   require the owner to clean the working tree; pull does not stash them.

- **Effort hypothesis:** M — low confidence; assumes unique path correspondence
  is enough to rebase one content edit over one addition. Revisit size if the
  new note's identity policy needs more than Stories 8 and 13 already prove.
- **Depends on:** delivered Stories 8 and 13; independent of Story 14.

No unresolved decisions within this narrow scope. Broader receipt across a
creation followed by web edits is excluded from this story.

## Ordering and Scope Reduction

The [product backlog](../PRODUCT-BACKLOG.md) owns global priority. Keep the
existing SEED-015 browser-workflow stories ahead of this seed; this reassessment
selects notebook-workflow priorities and does not displace that direction.

Within this seed, **13** is delivered. Remaining selected stories are **16**
(keep a local edit across one accepted addition) then **14** (related local
revisions). These are independent vertical outcomes, not technical
prerequisites for each other. Keep **15 → 10** as unqueued candidates:
folder-move divergence is consequential but narrower and has unresolved
identity-policy risk; batching changes readability without unlocking
synchronization.

First-to-drop order among remaining items: **10, 15, 14**. Stopping after
Story 16 still leaves a useful supported workflow.

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
  ranks receiving an addition beside local work, batch refinement, folder
  divergence, or history readability. Story 13 execution made the addition
  refusal observable in the sequential loop; that is why 16 precedes 14. Revise
  if the owner's current workflow makes a different interruption dominant.
- **Story 15, before selection:** confirm same-identity correspondence and the
  refusal boundary for ambiguity; do not assume ordinary Git rename detection
  alone establishes Donut identity.
- **Story 10, before selection:** define the end-of-edit/publication boundary
  and acceptable delay without losing durable saves or rewriting visible history.
- **Earlier drift:** Story 13 prevents a new source of drift only from matching
  state. Recovery for notebooks already out of sync remains an unselected need.

## When to Surface

Refine queued Stories 16 and 14 in this seed before slice planning. Reconsider
15 when folder relocation blocks unpublished work, and 10 when history
readability is an observed problem. This seed is non-executable; queue selection
does not authorize implementation.

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
