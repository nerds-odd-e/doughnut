---
id: SEED-016
status: active
planted: 2026-09-09
planted_during: README-only folder publication verification
trigger_when: selected now as the top product-backlog story
scope: small
---

# SEED-016: Publish the initial notebook and folder Readmes together

## Why This Matters

A notebook owner starting from an empty accepted notebook can publish one
README-only folder only when it is the sole changed path. If the same initial
commit also authors the notebook's root README, publication refuses the folder
README. The owner should be able to publish that smallest useful initial
container tree without splitting the commit or adding an ordinary note.

## Alternatives and Decision

Keep the existing one-folder-only path and add only the verified adjacent case.
Requiring separate commits would interrupt the owner's initial authored unit;
supporting arbitrary initial trees or the observed 10,406-file commit would
expand this into bulk import and is explicitly deferred.

## Story Decomposition

<a id="story-1"></a>

### 1. Publish the initial notebook README with one README-only folder

**Status:** Planned in
[quick/079](../quick/079-initial-notebook-and-folder-readmes/PLAN.md); not
executed.

- **Goal:** A notebook owner can publish one useful initial container tree from
  a local checkout: the notebook Readme and one README-only root Folder, kept
  together as the authored commit.
- **Scope:** The accepted notebook and Portable tree are empty. Exactly one
  direct-child commit adds regular-file `README.md` and
  `New Folder/README.md`, both valid, nonblank `type: Readme` Markdown.
  Publication creates one fresh root Folder, stores both authored Readmes, and
  accepts the exact commit atomically. Excluded: ordinary notes, additional or
  nested folders, more changed paths, an already non-empty accepted notebook,
  README edits to existing containers, multiple unpublished commits,
  stale/divergent history, attachments, and bulk import.
- **Key example:** Empty accepted notebook → commit the two README paths →
  Donut shows the notebook Readme and `New Folder` with its Readme, no ordinary
  Notes, and accepted/downloaded Git equals the authored commit.
- **Boundary example:** Adding any third path remains an unsupported tree shape.

## Ordering and Scope Reduction

This is the top backlog story because it is the smallest verified follow-up to
the just-delivered local README-only folder workflow. Stop after this exact
two-path initial tree; bulk initial import remains a separate problem.

## Open Decisions

None.

## When to Surface

Selected now for slice planning. Planning does not authorize execution.

## Breadcrumbs

- [SEED-009 Story 19](SEED-009-git-backed-local-notebook-workflow.md#story-19)
  delivers the sole-changed-path folder case.
- A temporary controller-boundary reproduction on 2026-09-09 added only the
  two README paths to an empty accepted tree; the backend suite ran 2,330 tests
  and failed only that example with the reserved folder-README refusal. The
  temporary test was removed after confirming the gap.
- Accepted ADR 0004 defines root and folder README Portable paths and their
  `type: Readme` contract.
