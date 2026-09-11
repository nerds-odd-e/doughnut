# Publish concepts into an implied Folder

Source: [SEED-009 Story 11](../../seeds/SEED-009-git-backed-local-notebook-workflow.md#story-11),
refined from the rejected `例文/111/` publication observed on 2026-09-11.

Status: planned. Story refinement and slice planning requested 2026-09-11;
implementation not requested.

## Goal and scope

A notebook owner can publish one direct-child commit against a matching,
non-empty accepted notebook when added concepts imply one new Folder beneath an
accepted represented Folder. Donut materializes the Folder from the concept
paths, persists the concepts, and accepts the authored commit without requiring
a Folder `README.md`.

The delivery proof uses the observed shape: accepted content represents
`例文/`; the proposed commit adds ordinary Notes and Relationship notes beneath
`例文/111/`; `例文/111/README.md` is absent. Counts and document roles in that
example are data, not production admission rules.

Preserve atomic publication, exact accepted-tree correspondence, fresh
identities for additions, identity/private data for accompanying unchanged-path
edits, and refusal to adopt a same-path live Folder that is absent from accepted
Portable content. This plan makes no new promise about pull/rebase, multiple
unpublished commits, moves or deletions, or editing container Readmes; do not add
new rejection logic for those variations.

Accepted [ADR 0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
requires tracked content for a Folder to exist in the Portable tree and says a
Readme is typical for an empty Folder; it does not require a Readme for a Folder
that contains concepts. No ADR conflict or new architectural decision remains.

## Key examples and proof ownership

| Promise | Observable proof |
| --- | --- |
| Concepts under `例文/111/` publish without a Folder Readme | Controller-boundary publication test observes the created `111` Folder, persisted ordinary and Relationship notes, and accepted proposed head/tree |
| Folder inference follows one existing model | Existing initial mixed-tree controller examples remain green, including implied nested ancestry and mixed concept types |
| Publication remains atomic on a real identity collision | Existing controller proof with a same-path live-but-unaccepted Folder observes the established sibling-name conflict and no partial publication |
| README presence is not an admission gate | Remove the handled missing-parent rejection expectation and the publisher pre-admission rule that produces it; source review finds no replacement shape/count/type guard |

## Current design evidence

`NotebookGitProposalDocumentApplication` already sends all added concept paths
to `NotebookGitProposalFolderMaterialization`. That materializer starts with
accepted represented Folders and creates missing path segments from the proposed
documents; initial-publication tests already prove the general mechanism for
Notes, Relationships, custom types, siblings, and deeper ancestry.

`NotebookGitProposalPublisher.requireRepresentedDestinationsUnlessParentFolderIsAdded`
prevents that common mechanism from running for a non-empty accepted notebook
unless an exact Folder README is also added. Its addition-only and
addition-with-edits call sites are the same accidental policy. Removing that
pre-admission policy lets both existing publication paths use the already shared
materialization behavior. Do not replace it with a recognizer for one Folder,
six files, specific concept types, or the `例文/111/` spelling.

Rename/move placement continues through its existing represented-destination
path and is not part of this addition behavior. A live-but-unaccepted same-path
Folder remains excluded from the materializer's accepted Folder index, so normal
Folder construction reaches the existing sibling identity conflict instead of
adopting it.

## Ordered slices

### 1. Publish added concepts through the shared Folder materializer
Type: Behavior
Status: planned
Sizing: about 5 minutes for test-first implementation, the full backend suite,
and slice-local cleanup; medium-high confidence. Backend-suite runtime is an
external-wait exception. Stop and refine before 10 minutes of active work if a
new product or identity decision appears.

Behavior: given a non-empty accepted notebook with `例文/` represented and no
accepted or live `例文/111` Folder → one direct-child commit adds valid ordinary
and Relationship concepts beneath `例文/111/` without a Folder README → publish
creates the `111` Folder, persists every added concept in it, and accepts the
exact commit atomically.

At the controller boundary, replace the obsolete test that expects a missing
parent rejection with a positive scenario based on the motivating shape. Assert
the created Folder ancestry, representative ordinary/Relationship authored
content, and the accepted proposed head/tree. First run the backend suite and
confirm this changed proof fails because the current pre-admission rule rejects
the Folder.

Remove the example-shaped destination pre-admission rule and both of its
publication call sites so added documents reach the existing shared Folder
materializer. Remove imports/helpers left dead by that change. Do not add a
replacement eligibility matrix. Align the independent live-but-unaccepted
Folder test with the genuine sibling identity-conflict outcome if its old
missing-parent wording changes.

Proof:

```bash
CURSOR_DEV=true nix develop -c pnpm backend:test_only
```

The first run after changing the outside-in expectation must fail for the
current README/represented-parent restriction. After the smallest production
change, the complete backend suite must pass, including initial mixed-tree,
existing represented Folder, README-backed Folder, exact-tree, rollback, and
live Folder collision coverage.

Safe stop: concept additions in the motivating existing-notebook case use the
same materialization rule as initial publication; no README-, count-, type-, or
layout-specific admission policy remains in front of that rule.

## Sizing and cumulative-design assessment

One Behavior slice owns one user-visible publication outcome and one
controller/full-backend proof loop. Separating test removal, guard removal, or
cleanup would split one red-to-green behavior and create unsafe stopping points.
No preparatory Structure slice is justified because the shared Folder
materializer already exists.

The cumulative design becomes simpler: accepted represented Folders seed one
materialization map, and all added concept paths extend it. Existing specific
initial-tree examples support that rule; they do not justify separate production
branches. The only deliberate exception in this area is a live Folder identity
that accepted Git does not represent, which existing Folder construction
detects as a collision rather than another tree-shape policy.

No slice-specific concern remains after inspection. The plan intentionally
commits to one motivating existing-notebook example while allowing the shared
algorithm's natural general behavior; it adds no exhaustive verification promise
for every depth, sibling count, or document mixture.

## Delivery

Planning only; execution is not authorized by this request. When authorized,
use `dough-execute-plan`: move the story from Backlog to Taken at execution
start, deliver the slice test-first with Jidoka, run a fresh
`dough-post-change-refactor` pass, run coordinator-owned
`./scripts/run.sh pnpm format:changed` once, update this plan, commit, push, and
handle CI asynchronously. No generated API or schema change is expected.

Keep this plan and its story through execution retrospective and story wrap-up.

## Learnings

Planning inspection found that the general Folder materializer already supports
the desired behavior. The remaining restriction is an earlier publisher guard
and its handled rejection test, both shaped around README-backed incremental
examples. No tests were run and no product behavior was changed during planning.
