---
id: SEED-009
status: dormant
planted: 2026-09-04
planted_during: ADR 0002 v1 discussion
trigger_when: when selecting the first implementation story for Git-backed Portable notebook synchronization
scope: large
---

# SEED-009: Refine a Donut notebook locally with Obsidian and AI-enabled IDEs

## Why This Matters

For an Obsidian user who also uses an AI-enabled IDE such as Codex, Cursor, or
Claude Code, a Donut notebook that currently lives only as remote Donut state
should become an ordinary local Git repository that the user can open, refine,
and synchronize with the accepted remote copy, within the constraints below.

The current catalog ZIP export is decoration, not a synchronization workflow.
Initializing a personal Git repository from a ZIP gives the user neither a
shared remote history nor a safe way to return changes to the same Donut
entities. Re-exporting and copying files cannot reliably detect divergence,
merge accumulated changes, or preserve the private identity and learning data
of a renamed note.

The desired effect is observable without a Donut-specific editor: the user
works on the Portable notebook tree with Obsidian or an AI IDE, commits with
Git, and synchronizes in either direction without silent overwrite or transfer
of learning history to the wrong note.

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

The highest-learning first increment is whether an existing remote notebook can
cross the automatic Git cutover and arrive as an ordinary local repository
that the owner can open with their chosen tools. This tests the baseline,
portability, and user entry workflow before accepting local mutations.

## Alternatives and Decision

1. **Do nothing or defer:** users remain limited to the Donut web application
   and one-way export. Rejected because it does not enable Obsidian or
   AI-assisted local refinement.
2. **Offer a smaller snapshot/download behavior:** a ZIP or read-only folder
   gives a local copy but no common history or return path. The user explicitly
   rejected ZIP as merely decorative.
3. **Use a manual existing-tool workflow:** export, run `git init`, edit, and
   manually copy files back. Rejected because it cannot establish the accepted
   remote base, safely detect concurrent changes, or preserve Donut identity
   through structural changes.
4. **Pursue Git-backed synchronization:** automatically bootstrap every
   notebook with one root commit, let V1 use a CLI-assisted Git workflow, and
   add user-visible capabilities in the order below. Recommended because it
   provides immediate local-tool value while keeping one revision and conflict
   model that can later support a standard Git remote.

The estimates below are story hypotheses made without implementation
inspection. Any discovery that changes a story's outcome, boundary, or order
returns to this seed; implementation complexity within one selected story is
handled by `slice-planning` and, when necessary, `slice-plan-refinement`.

## Story Decomposition

### 1. Open an existing Donut notebook in Obsidian and an AI IDE

- **For / why:** An existing notebook owner wants a normal local directory and
  Git history that their preferred tools can open without first activating or
  converting that notebook.
- **Evaluation:** After the v1 cutover, the owner uses the supported
  CLI-assisted flow for any compatible existing notebook, receives its
  canonical Portable tree in an ordinary Git repository with one root commit
  for the pre-cutover state, and opens the same files in Obsidian and an AI
  IDE. No fabricated earlier commits or Donut metadata appear in the tree.
- **Value / learning:** Delivers useful local ownership and AI/Obsidian access
  even if publishing is cancelled. Tests the most consequential first
  assumption: automatic bootstrap plus local delivery can preserve the
  notebook exactly.
- **Effort hypothesis:** L — low confidence; assumes the accepted ADR-0004
  representation and CLI authentication can be reused, but no implementation
  inspection has tested that assumption.
- **Depends on:** none.
- **Safe stopping point:** The local copy is useful as a snapshot, but the
  product must clearly say that local publishing is unavailable until Story 2;
  creating the copy must not mutate remote content.

### 2. Publish a local content edit to the same Donut note

- **For / why:** The owner wants an Obsidian- or AI-assisted refinement to
  become the remote truth without copy-paste.
- **Evaluation:** Starting from current `main`, the owner edits one existing
  Markdown note locally, commits it with Git, synchronizes through the
  supported v1 flow, and sees the same Donut note updated while its existing
  identity-bound learning data remains attached.
- **Value / learning:** Completes the primary local-refinement payoff for the
  simplest non-divergent case and tests whether one accepted Git commit can
  update Donut without creating a second authority.
- **Effort hypothesis:** L — low confidence; assumes Story 1 establishes an
  authenticated accepted head and that one-note content replacement is a
  representative first projection.
- **Depends on:** Story 1.
- **Safe stopping point:** A stale or invalid proposal is rejected without
  changing remote `main` or the Donut projection.

### 3. Receive a Donut web edit in a clean local repository

- **For / why:** The owner also edits remotely and needs their Obsidian vault
  and AI IDE to see the accepted web change.
- **Evaluation:** With a clean local repository at current `main`, the owner
  edits an existing note in Donut, synchronizes locally, and observes the exact
  accepted Portable content as the next Git commit and working-tree change.
- **Value / learning:** Establishes sequential two-way synchronization for
  existing-note content. If later stories are cancelled, users can still move
  cleanly between local and web editing as long as they synchronize between
  changes.
- **Effort hypothesis:** L — low confidence; assumes an individual durable web
  save may initially produce its own commit before later batching refinement.
- **Depends on:** Story 1.
- **Safe stopping point:** Uncommitted or unpublished local work is never
  overwritten; this story may stop and instruct the user to resolve their local
  state rather than attempting divergence.

### 4. Create a new note locally

- **For / why:** Local refinement often discovers a new concept that should
  become a real Donut note rather than remain a local-only file.
- **Evaluation:** The owner adds one valid ADR-0004 Markdown note, commits and
  synchronizes it, and sees one new note at the matching Portable path in
  Donut.
- **Value / learning:** Makes local tools useful for growing, not merely editing,
  a notebook. The capability remains useful even if deletion, moves, and
  divergence are deferred.
- **Effort hypothesis:** M — low confidence; assumes the basic publish boundary
  from Story 2 can distinguish a valid addition from a content update.
- **Depends on:** Story 2.
- **Safe stopping point:** Invalid files and destination collisions reject the
  commit without partially creating remote entities.

### 5. Delete a note locally without transferring its private data

- **For / why:** The owner wants an intentional Git deletion to remove the
  corresponding Donut note while keeping its learning history from being
  attached to some other note.
- **Evaluation:** The owner deletes one existing note in its own commit and
  synchronizes it; the note is no longer active in Donut, and its
  identity-bound data remains with the deleted entity rather than moving to a
  later addition.
- **Value / learning:** Completes the basic file lifecycle while proving that
  absence in the Portable tree does not cause identity corruption.
- **Effort hypothesis:** M — low confidence; assumes Donut already has a
  product-visible deletion outcome that can be invoked safely from the accepted
  commit.
- **Depends on:** Story 2.
- **Safe stopping point:** A delete/add combination whose identity meaning is
  unsafe is rejected rather than guessed.

### 6. Rename or move a note without losing its learning history

- **For / why:** Obsidian and AI IDE users routinely reorganize files and expect
  a moved note to remain the same learned concept in Donut.
- **Evaluation:** The owner commits an unambiguous move-only rename or move and
  synchronizes it; Donut shows the new Portable path on the same note entity,
  with its memory trackers and other private data still attached. A combined
  move-and-rewrite whose identity is ambiguous is rejected with enough guidance
  to split it into a move commit followed by an edit.
- **Value / learning:** Tests the defining Donut-specific risk that Git itself
  does not solve: conservative projection from path changes to stable private
  identity.
- **Effort hypothesis:** L — low confidence; assumes exact one-to-one
  correspondence is a sufficiently narrow first identity case.
- **Depends on:** Story 2.
- **Safe stopping point:** Similarity alone never transfers private data; the
  remote remains unchanged when correspondence is ambiguous.

### 7. Move a folder while preserving descendant identities

- **For / why:** The owner wants to reorganize a group of notes without losing
  the individual Donut identities and learning data under that folder.
- **Evaluation:** The owner commits one unambiguous folder relocation and
  synchronizes it; Donut shows the folder and descendants at their new paths,
  and each corresponding Note or Folder entity retains its private data.
- **Value / learning:** Extends safe reorganization from one file to the
  common notebook-level operation. Note-level moves remain valuable if this
  story is cancelled.
- **Effort hypothesis:** L — low confidence; assumes exact descendant
  correspondence can define a bounded folder-move case and excludes ambiguous
  partial rewrites.
- **Depends on:** Story 6.
- **Safe stopping point:** Incomplete or multiply plausible descendant
  correspondence rejects the commit atomically.

### 8. Keep non-overlapping accumulated local and web changes

- **For / why:** The owner may refine locally while another accepted web edit
  reaches remote `main`; changes to different notes should not force either
  side to be discarded.
- **Evaluation:** A local unpublished commit and a newer remote commit change
  different notes. Synchronization rebases the local work onto remote `main`,
  publishes a linear result, and leaves both changes visible locally and in
  Donut.
- **Value / learning:** Delivers the first genuinely divergent workflow and
  tests the chosen rebase-only policy with accumulated changes.
- **Effort hypothesis:** M — low confidence; assumes Stories 2 and 3 make the
  two directions independently reliable and limits this story to
  non-overlapping note changes.
- **Depends on:** Stories 2 and 3.
- **Safe stopping point:** If rebase cannot complete cleanly, synchronization
  stops without advancing remote `main`.

### 9. Resolve an overlapping edit with ordinary Git

- **For / why:** The owner needs conflicting local and web refinements to be
  detected and resolved without a Donut-specific merge format.
- **Evaluation:** Local and remote commits overlap in one Markdown file.
  Synchronization presents an ordinary Git rebase conflict, leaves the accepted
  remote unchanged, and, after the owner resolves and continues the rebase,
  accepts the resulting linear history and displays the resolution in Donut.
- **Value / learning:** Completes the safety promise for overlapping
  accumulated changes while validating that standard Git conflict handling is
  understandable in the CLI-assisted v1 workflow.
- **Effort hypothesis:** M — low confidence; assumes standard text conflict
  behavior is adequate for the first overlapping Markdown case.
- **Depends on:** Story 8.
- **Safe stopping point:** Cancelling or abandoning conflict resolution loses
  neither the accepted remote commit nor the user's original local commit.

### 10. See one stable commit for one continuous web edit

- **For / why:** An owner reading Git history should see a meaningful editing
  unit rather than one commit per autosave or rewritten published commits.
- **Evaluation:** Several continuous saves to the same note, with no intervening
  accepted commit, appear as one stable commit. A different accepted commit
  cuts the batch. Once advertised, the earlier commit ID never changes.
- **Value / learning:** Improves the history users and AI tools inspect without
  changing the synchronization model. All earlier synchronization value
  remains if this refinement is cancelled.
- **Effort hypothesis:** M — low confidence; assumes Story 3 may safely start
  with one commit per durable save and that batching can be added as an
  observable policy refinement.
- **Depends on:** Story 3.
- **Safe stopping point:** Prefer extra immutable commits over amending any
  history already visible to a client.

## Ordering and Scope Reduction

Story 1 comes first because the user named initial local acquisition as the
first difficulty and because it proves the automatic baseline before Donut
accepts local mutations. Story 2 follows the user's highest-value workflow:
refine locally, then publish. Story 3 closes the clean sequential loop in the
opposite direction. Stories 4 and 5 complete the basic file lifecycle. Stories
6 and 7 address the identity-preservation risk unique to Donut. Stories 8 and 9
then add accumulated divergence and conflict handling on top of two already
working directions. Story 10 is last because commit batching improves history
quality but is not required for safe synchronization.

Safe stopping points:

- After Story 1: useful local Git snapshot for Obsidian and AI tools, explicitly
  read-only with respect to Donut.
- After Story 3: basic sequential two-way editing of existing note content;
  creation, deletion, moves, and divergence remain rejected or unsupported.
- After Story 5: basic note content lifecycle without structural identity
  inference.
- After Story 7: ordinary linear note and folder reorganization preserves
  private identity.
- After Story 9: the six synchronization needs are covered for the supported
  dedicated-repository v1.
- After Story 10: web-authored history also has the desired editing granularity.

First-to-drop order when reducing an initial release is:

1. Story 10, accepting extra immutable web commits.
2. Story 7, rejecting folder moves while retaining note moves.
3. Story 5, rejecting local deletions while retaining edits and additions.
4. Story 4, limiting local publication to edits of existing notes.
5. Stories 8 and 9 together, limiting the product to an explicitly sequential
   synchronize-before-edit workflow.
6. Story 6, rejecting all local rename/move operations.

Dropping any of Stories 4–9 creates an honest interim capability, not complete
ADR-0002 v1 synchronization. Unsupported operations must fail clearly rather
than be approximated.

## Open Decisions

No open product decision changes the candidate-story order.

Before planning Story 1, reconcile Proposed ADR 0002 with the later human
discussion recorded here. The current draft says Donut exposes a standard Git
remote in v1 and that a CLI is never required; the later v1 boundary defers
direct standard-Git access and permits a required CLI-assisted acquisition and
synchronization workflow. This is an ADR wording/alignment task owned by the
human advice process, not permission to treat the Proposed ADR as Accepted.

The exact CLI verbs, authentication presentation, and whether its Git transport
appears as a dedicated sync command or a Git remote helper do not change these
story outcomes. Slice planning may decide them only if the result keeps Git as
the revision/merge model and adds no Donut metadata to the Portable tree.

## When to Surface

Surface this seed when the human is ready to select the first ADR-0002 v1
implementation story. Reconcile the Proposed ADR's v1 CLI boundary, select
exactly one story—Story 1 is recommended—and invoke `slice-planning`. Do not
turn the whole seed into one executable plan.

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
- Human decisions from the 2026-09-04 discussion:
  - The primary beneficiary is an Obsidian user who also wants Codex, Cursor,
    or Claude Code to refine their local notes.
  - ZIP export is decoration and is not a useful synchronization alternative.
  - V1 is a dedicated repository rooted at `/`, linear `main`, and rebase-only
    integration. Direct standard-Git clone/fetch/push is later; V1 may use the
    Donut CLI.
  - No Donut identity or synchronization metadata belongs in the Portable
    tree. Minimal repository binding/authentication may use ordinary Git
    configuration and credential storage.
  - Every existing notebook is automatically bootstrapped during one fleet
    migration. Each gets one root commit from the current canonical MySQL
    projection; no earlier Git history is invented and there is no per-notebook
    activation mode. New notebooks begin Git-backed.
  - After cutover, accepted Git `main` is the Portable-content authority and
    MySQL is its current projection plus the authority for Donut-only data.
  - Donut web edits create commits. Continuous same-note edits may be
    coalesced only before publication and only when no other commit cuts the
    sequence.
  - V1 has no Donut historical checkout/revert UI. Reachable Git history is
    nevertheless retained.
- Representative example: edit an existing note in an AI IDE, commit it,
  synchronize, and see the same Donut note updated with its memory trackers
  intact.
- Counterexample: exporting a ZIP, initializing an unrelated local repository,
  and copying files back does not establish common ancestry or identity.
- Boundary: a clean sequential content edit is earlier than divergence;
  ambiguous identity and unresolved conflicts never mutate remote `main`.
- Fresh-agent handoff: this seed contains the product context and story
  decisions, not implementation design. A new agent should read this seed and
  the current ADRs, select one story, then use `slice-planning`, which may
  inspect the codebase and create the executable PLAN.
