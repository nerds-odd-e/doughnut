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

Clone, content publish/pull, local additions/deletion/reorganization, and bounded
content rebase are delivered. The remaining problem is unsupported everyday
changes interrupting that loop; a cleaner Git log alone does not close it.
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

1. **Defer:** keep the delivered bounded workflow and tolerate extra immutable
   autosave commits. Reasonable for history readability; it leaves common
   notebook changes outside synchronization.
2. **Smaller behavior change:** synchronize one ordinary web-created note,
   starting from matching accepted state. Recommended first: it tests whether
   adding knowledge in Donut can preserve the established local refinement loop
   without taking on all structural operations or historical drift recovery.
3. **Manual/existing-tool workflow:** create all new notes locally, synchronize
   before editing, and publish existing-note edits separately. This is today's
   strongest smaller alternative. It remains usable, but restricts where the
   owner captures ideas and fragments a related AI-assisted revision. It does
   not meet the established goal of working in both Donut and local tools.
4. **Complete synchronization or web-commit batching:** defer the broad contract
   and readability polish. Neither is the smallest way to remove the next
   concrete interruption; batching already-accepted saves risks conflicting
   with immutable history unless its publication boundary is clarified.

**Priority hypothesis:** first keep web note creation inside the synchronization
loop, then allow one related local edits-only batch. This follows the established
beneficiary and two-way workflow goal plus the owner's request to select new
priorities. It is not a claim that usage data proves these are the most frequent
failures. The first story tests whether owners can capture a new idea on the
web and continue refining it locally without copying or blocking publication.

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
  history, including accepted structural changes. Web
  creation/deletion/rename/move and notebook/folder README edits are not
  synchronized; existing projection drift is neither absorbed nor repaired.

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
  extends this to same-note overlap.

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

**Status:** queued.

- **For / why:** A notebook owner captures an idea in Donut, then wants to refine
  it in Obsidian or an AI IDE without web creation blocking later publication.
- **Evaluation:** From a notebook whose Portable content matches accepted main,
  create one valid ordinary note on the web at the root or in an existing
  represented folder. Pull into a clean up-to-date checkout, edit that new note,
  commit and publish; Donut shows the refinement on that same note identity.
- **Value / learning:** Removes a break in the sequential two-way workflow.
  Tests web capture followed by local refinement before broad structural sync.
- **Effort hypothesis:** L — low confidence; assumes existing web creation and
  accepted-content synchronization can support one ordinary-note addition
  without changing the Portable profile or solving earlier drift.
- **Depends on:** delivered acquisition, content publish/pull, and local creation
  capabilities (Stories 1–4); no unfinished product prerequisite.
- **Safe stopping point:** Web-created notes can participate in the existing
  loop even if every other new story is cancelled. Creation and accepted history
  must agree; failure must not leave a new unsynchronized note. Existing notes'
  private data stays untouched.
- **Boundary:** One note creation from matching state, including an empty
  notebook with matching accepted tree. No new folder/README, attachment, web
  delete/rename/move, pre-existing drift repair, or unpublished local work during
  receipt. Those are separate outcomes, not hidden acceptance obligations.

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

## Ordering and Scope Reduction

The [product backlog](../PRODUCT-BACKLOG.md) owns global priority. Keep the
existing SEED-015 browser-workflow stories ahead of this seed; this reassessment
selects notebook-workflow priorities and does not displace that direction.

Within this seed, select **13 → 14**: prevent web capture from interrupting the
round trip, then support related local revisions. These are independent vertical
outcomes, not technical prerequisites for each other. Keep **15 → 10** as
unqueued candidates: folder-move divergence is consequential but narrower and
has unresolved identity-policy risk; batching changes readability without
unlocking synchronization. All four have a named owner and a result observable
through ordinary tools and Donut, and include their complete user journey.

First-to-drop order: **10, 15, 14, 13**. Stopping after either selected story
leaves a useful supported workflow, with no preparation for another story.

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
  ranks web capture, batch refinement, folder divergence, or history readability.
  The recommended queue follows the established product goal; revise if the
  owner's current workflow makes a different interruption dominant.
- **Story 15, before selection:** confirm same-identity correspondence and the
  refusal boundary for ambiguity; do not assume ordinary Git rename detection
  alone establishes Donut identity.
- **Story 10, before selection:** define the end-of-edit/publication boundary
  and acceptable delay without losing durable saves or rewriting visible history.
- **Earlier drift:** Story 13 prevents a new source of drift only from matching
  state. Recovery for notebooks already out of sync remains an unselected need.

## When to Surface

Refine queued Stories 13 and 14 in this seed before slice planning. Reconsider
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
