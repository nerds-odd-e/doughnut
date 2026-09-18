---
id: SEED-035
status: dormant
planted: 2026-09-18
planted_during: execution retrospective of permanent deletion of trashed content, owner judged the gap a problem
predecessor: SEED-034 story 1 and plan 146, recoverable at commit 8b8b83e962
trigger_when: now, at the head of the product backlog
scope: medium
---

# SEED-035: Web folder rename and dissolve keep the accepted Git tree in step

## Why This Matters

A notebook owner who edits a Git-backed notebook in Obsidian or an AI IDE relies
on Donut's accepted Portable tree describing the same notebook the database
describes. Folder move, trash and permanent deletion all lock the notebook, apply
the whole domain operation and append one accepted commit. Folder **rename** and
**dissolve** change the same Portable paths but never reach that owner, so after
either action the accepted tree can describe a notebook that no longer exists.

## Alternatives and Decision

The operations already have a consistency owner and a shared seam; the question
is whether rename and dissolve join it. Giving either its own snapshot path
would contradict the single-consistency-owner direction. Leaving the gap in place
is the alternative the owner rejected when this was raised.

## Evidence

Gathered while reviewing plan 146; all of it predates that plan.

- In `backend/src/main/java/com/odde/donut/services/FolderRelocationService.java`,
  `acceptedWebChangeService.apply` is reached from exactly one place: the private
  `applyLiveFolderChange` helper. Its three callers are
  `moveFolderWithinNotebook`, `trashFolderWithinNotebook` and
  `permanentlyDeleteFolderWithinNotebook`.
- `renameFolder` and `dissolveFolder` return without ever reaching
  `acceptedWebChangeService`.
- Both change Portable paths. Rename changes a folder's own path and therefore
  the path of everything beneath it. Dissolve reparents the folder's subfolders
  and notes to its parent and removes the folder.
- There is no `NotebookGitWeb*` accepted-commit test class for web folder rename
  or dissolve. The `NotebookGit*Rename*` classes cover proposal and publication
  renames, which are a different path.

## Refinement direction, 2026-09-18

The owner requested scope defined around high cohesion and direct mapping to
domain concepts: one representation of a responsibility throughout the system,
so later requirements can change that responsibility in one place. This is
authority to refine the story, not to implement it.

The recommendation is to complete the existing accepted-web-change concept.
The omission of rename and dissolve from NORTH-STAR's examples is not a domain
reason for giving them separate consistency rules. Pre-existing drift is a
boundary condition to preserve, not the intended result of these operations.

Further inspection found that `WebFolderCreationService` and the supported
ordinary-note path in `WebNoteCreationService` also implement accepted-web-change
coordination: lock/load, validate the accepted head, check the initial projection,
mutate, snapshot, and persist. Merely connecting rename and dissolve would leave
this responsibility represented three times. The recommended structural scope
therefore includes consolidating these existing creation paths with the shared
owner, preserving their current admission behavior. The owner approved this
scope on 2026-09-18 and requested slice planning with refinement if needed.
Both constraints are mandatory: remove competing representations of the same
domain responsibility, and do not generalize distinct responsibilities together.
Behavior-preserving removal of a redundant condition requires evidence of
equivalence; preserving behavior does not require preserving duplicate code.

## Story Decomposition

<a id="story-1"></a>

### 1. Web folder rename and dissolve keep the accepted Git tree in step

- **Goal / beneficiary:** A notebook owner whose database projection matches its
  accepted Git tree can rename or dissolve a folder on the web and retain that
  agreement. Maintainers can change accepted-web-change coordination through one
  domain owner shared by the existing participating operations.
- **Value:** Obsidian and AI-IDE round-tripping depends on the accepted tree
  being trustworthy after every web action. An owner cannot tell from the UI that
  two of the five folder operations leave it behind.
- **Scope:**
  - Web folder rename and dissolve apply their complete domain operation through
    the existing accepted-web-change owner. For an initially synchronized
    notebook, append one commit if its Portable tree changes, in the same
    transaction as the database mutation. A no-op appends no commit.
  - Resolve current folder state and validate authorization and notebook
    membership within the existing locked operation. Reference capture precedes
    mutation; existing reference rewriting, collision handling and requested
    merging finish before the final snapshot.
  - The snapshot includes the complete result in that notebook: descendant
    paths, rewritten authored content, folder READMEs and empty-folder markers.
    Preserve surviving note identities and learning history.
  - Consolidate the duplicated accepted-web-change coordination in web folder
    creation and currently supported ordinary-note creation. Preserve their
    operation-specific eligibility, domain behavior and caller contracts; do not
    expand which creation variants participate in Git history as a side effect.
  - Preserve existing move, trash, permanent deletion, note editing and
    relationship reduction callers. Structural changes needed for this coherent
    ownership belong to the story even outside the rename/dissolve files.
  - Preserve pre-existing drift policy: the operation may succeed in the database
    without appending an accepted commit. Do not absorb earlier unsynchronized
    work. A notebook without a binding still performs the operation without a
    commit. Rejections and transaction failures leave no partial database/Git
    result; propagate unexpected failures through existing handling.
- **Boundary assumptions:** This is about the web actions only. Proposal and
  publication renames already have their own covered path and are out of scope.
  Preserve current reference rewriting outside the notebook, but do not promise
  accepted Git commits in those other notebooks: NORTH-STAR explicitly defers
  cross-notebook referrer coordination. This limitation remains visible rather
  than being hidden behind an all-notebooks consistency claim.
- **Key examples:** Unless stated otherwise, the notebook has a Git binding and
  its database projection matches the accepted tree before the action.
  - A Git-backed notebook has `Biology/Cells.md`. Renaming `Biology` to `Life`
    appends exactly one accepted commit whose tree contains `Life/Cells.md` and
    no `Biology/` path.
  - A Git-backed notebook has `Biology/Nested/Cells.md` and `Biology/Atoms.md`.
    Dissolving `Biology` into the notebook root appends exactly one accepted
    commit whose tree contains `Nested/Cells.md` and `Atoms.md`, with no
    `Biology/` path and no commit per moved note.
  - Dissolving a folder whose child name already exists at the destination, with
    merge requested, still appends exactly one accepted commit describing the
    merged result.
  - A same-notebook note links to a descendant of the renamed/dissolved folder:
    the accepted result includes the existing link rewrite, not an intermediate
    snapshot taken before rewriting. The descendant retains its note ID and
    learning history.
  - Renaming to the same effective name leaves the accepted head unchanged.
    A conflicting rename, or a dissolve with a conflicting child and no merge
    requested, preserves both the original database state and accepted head.
  - An empty folder is renamed or dissolved: its `.keep` representation follows
    the final folder structure. Renaming carries its README; dissolving removes
    the dissolved folder's own README under existing behavior. This story adds
    no README transfer/merge policy.
  - The database already differs from the accepted tree: rename/dissolve still
    follows existing database behavior and does not append a commit. The same
    operation without a Git binding succeeds without creating a binding.
  - After a synchronized rename, a later ordinary web content edit still appends
    its accepted commit: the rename has not stranded subsequent work in drift.

### Domain ownership and structural completion

| Domain responsibility | Existing owner and intended result |
| --- | --- |
| One complete accepted web change | `AcceptedWebChangeService`: lock/load, initial drift decision, complete mutation, final projection and conditional commit in one transaction. Existing participating callers use this one policy. |
| Folder rename, relocation, dissolve and merge | Existing folder services and `FolderSubtree`: retain their distinct domain recipes; coordinate each complete user operation once. |
| Construction and creation eligibility | Existing construction/web creation owners: preserve ordinary-note, Wikidata, concept-type and represented-folder rules while delegating shared coordination. |
| Reference meaning and rewriting | Existing reference owners: capture resolution before paths change and apply their current rewrite rules. |
| Portable representation and accepted snapshot persistence | Existing snapshot/codec and persistence owners: reuse them; no caller-specific tree builder. |

One representative means one authoritative rule, not one class containing every
operation. Delegating adapters can remain when they have a distinct purpose.
Do not introduce an operation registry, generic mutation framework, per-action
dispatch flags, new identity model, or heuristic rename detection for known web
identities. Git publication and export have different purposes; sharing Portable
encoding does not make them accepted web changes.

Structural completion requires checking all existing representations of this
coordination, removing the duplicated policy from the creation paths, and
reviewing affected callers for preserved semantics. Merely adding two calls or
passing the rename/dissolve tests does not establish that completion. The common
owner must not learn the names of its individual operations to serve them.

Relevant constraints: [NORTH-STAR](../NORTH-STAR.md#one-complete-accepted-web-change)
for complete-operation ownership and deferred cross-notebook coordination;
[Accepted ADR 0004](../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
for Portable paths, READMEs and `.keep`;
[Accepted ADR 0005](../../docs/adrs/0005-web-routes-accepted.md) for stable note-ID
web destinations; [Accepted ADR 0006](../../docs/adrs/0006-failure-handling-accepted.md)
for failure propagation. Proposed ADR 0002 remains proposed.

### Evaluation and deferred promises

Evaluate behavior through real controller operations and inspection of persisted
database state and accepted Git trees/heads. Reuse existing proof for creation
eligibility and established callers where its setup and observations establish
the preserved promises. Add missing rename/dissolve observations at that same
boundary. Assess ownership by reading the resulting responsibilities and call
paths, not tests tied to private helper structure.

Deferred: repairing already-divergent notebooks, new synchronization coverage
for creation variants, cross-notebook history coordination, changes to dissolve
or merge policy (including trash behavior), Git publication/inference changes,
new UI, and unrelated folder-service cleanup. These are delivery limits, not
reasons to add production rejection gates or duplicate shared behavior.

The main sizing uncertainty is preserving creation eligibility while removing
its duplicate coordination. Planning should establish those existing contracts
before sizing slices. If a meaningful policy conflict is discovered, surface it
rather than silently broadening behavior or retaining a second consistency owner.
Executable plan: [148 — Cohesive accepted web folder changes](../quick/148-cohesive-accepted-web-folder-changes/PLAN.md).
Planning is authorized; implementation is not yet authorized.
