# Publish concepts into an implied Folder

Source: [SEED-009 Story 11](../../seeds/SEED-009-git-backed-local-notebook-workflow.md#story-11),
refined from the rejected `例文/111/` publication observed on 2026-09-11.

Status: done. Executed 2026-09-11 on `quick/103-publish-concepts-into-implied-folder`.

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
| Concepts under `例文/111/` publish without a Folder Readme | `NotebookGitProposalFolderCreationControllerTest.publishesConceptsBeneathAnImpliedFolderWithoutAFolderReadme` observes the created `111` Folder, persisted ordinary and Relationship notes, and accepted proposed head/tree |
| Folder inference follows one existing model | Existing initial mixed-tree controller examples remain green, including implied nested ancestry and mixed concept types |
| Publication remains atomic on a real identity collision | `NotebookGitFolderNotePublicationControllerTest.refusesToAdoptAnUnrepresentedLiveFolderWhenAddingANote` and the existing README-backed live-Folder proof observe `FOLDER_NAME_CONFLICT` with no partial publication |
| README presence is not an admission gate | Publisher pre-admission rule and missing-parent rejection tests removed; source review finds no replacement shape/count/type guard |

## Current design evidence

`NotebookGitProposalDocumentApplication` sends all added concept paths to
`NotebookGitProposalFolderMaterialization`. That materializer starts with
accepted represented Folders and creates missing path segments from the proposed
documents. Initial publication and this existing-notebook case now share that
rule. Rename/move placement continues through its existing represented-destination
path. A live-but-unaccepted same-path Folder remains excluded from the
materializer's accepted Folder index, so Folder construction reaches the
existing sibling identity conflict instead of adopting it.

## Ordered slices

### 1. Publish added concepts through the shared Folder materializer
Type: Behavior
Status: done
Sizing: about 8 minutes active work excluding backend-suite wait.

Behavior: given a non-empty accepted notebook with `例文/` represented and no
accepted or live `例文/111` Folder → one direct-child commit adds valid ordinary
and Relationship concepts beneath `例文/111/` without a Folder README → publish
creates the `111` Folder, persists every added concept in it, and accepts the
exact commit atomically.

Proof:

```bash
unset SPRING_DATASOURCE_URL DB_URL SPRING_FLYWAY_URL
CURSOR_DEV=true nix develop -c pnpm backend:test_only
```

First-red: `publishesConceptsBeneathAnImpliedFolderWithoutAFolderReadme` failed
with 400 "Parent folder for path \"例文/111/A-related-to-B.md\" is not
represented in accepted Portable content" from the removed pre-admission rule.
After removing `requireRepresentedDestinationsUnlessParentFolderIsAdded` and
both call sites, the complete backend suite passed.

Safe stop: concept additions in the motivating existing-notebook case use the
same materialization rule as initial publication; no README-, count-, type-,
or layout-specific admission policy remains in front of that rule.

## Sizing and cumulative-design assessment

One Behavior slice owns one user-visible publication outcome and one
controller/full-backend proof loop. The cumulative design is simpler: accepted
represented Folders seed one materialization map, and all added concept paths
extend it. The only deliberate exception is a live Folder identity that
accepted Git does not represent.

## Delivery

Executed in worktree `/Users/terryyin/git/doughnut-103-implied-folder`.
Story moved from Backlog to Taken. Slice delivered test-first with Jidoka.
Post-change refactor: none — already clean. Coordinator ran
`./scripts/run.sh pnpm format:changed` once. No generated API or schema change.

Keep this plan and its story through execution retrospective and story wrap-up.

## Learnings

The README/represented-parent restriction was only the publisher pre-admission
guard plus its handled rejection tests. Removing that guard was sufficient;
the shared Folder materializer already created `例文/111` from concept paths.
The live-unaccepted Folder case changed from missing-parent wording to
`FOLDER_NAME_CONFLICT` as predicted. Refactor found no remaining duplication of
the admission rule. Feature-branch push has no GitHub Actions coverage (`ci.yml`
is push-triggered on `main` only).
