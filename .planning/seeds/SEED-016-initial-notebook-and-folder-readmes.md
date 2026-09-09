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

**Status:** delivered. Recover quick/079 from `034be9f99d`.

- **Goal:** A notebook owner can publish one useful initial container tree from
  a local checkout: the notebook Readme and one README-only root Folder, kept
  together as the authored commit.
- **Scope:** The notebook is empty of folders and the accepted Portable tree is
  empty (empty folders alone do not qualify). Exactly one direct-child commit
  adds regular-file `README.md` and `New Folder/README.md`, both valid,
  nonblank `type: Readme` Markdown. Publication creates one fresh root Folder,
  stores both authored Readmes, and accepts the exact commit atomically.
  Excluded: ordinary notes, additional or nested folders, more changed paths,
  an already non-empty notebook (including existing empty folders), README
  edits to existing containers, multiple unpublished commits, stale/divergent
  history, attachments, and bulk import.
  Empty-notebook eligibility correction: quick/080.

## Ordering and Scope Reduction

This was the smallest verified follow-up to the local README-only folder
workflow. Stop after this exact two-path initial tree; bulk initial import
remains a separate problem.

## Open Decisions

None.

## When to Surface

Story 1 is delivered. Further SEED-016 work is unselected.

## Breadcrumbs

- [SEED-009 Story 19](SEED-009-git-backed-local-notebook-workflow.md#story-19)
  delivers the sole-changed-path folder case.
- Accepted ADR 0004 defines root and folder README Portable paths and their
  `type: Readme` contract.
