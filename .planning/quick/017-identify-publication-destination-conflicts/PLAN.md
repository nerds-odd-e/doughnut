# Identify the conflicting note in a multi-note publication

## Source and status

- Source: [SEED-009 Story 11](../../seeds/SEED-009-git-backed-local-notebook-workflow.md#story-11).
- Follow-up to completed `016-publish-several-note-changes/PLAN.md`, recovered
  from Git: earliest tracked plan at `3ea066f12d`; all 13 leaves done at
  `4e9fb6809d`; cleanup at `6b6bf5ddd8`.
- Status: slice 1 complete; commit/push and completion cleanup next.
- CI observer: coordinator `/root`, checkout `/Users/terryyin/git/doughnut`,
  repository `nerds-odd-e/doughnut`, branch `main`; cell `169`, PTY `66832`,
  directory `/tmp/donut-ci-501/watch-sd5l2O`, PID `5672`.
- Startup CI event `34029294495` / attempt 1 / job `101475765216` is the
  identical previously triaged lifecycle-fixture failure at `ae291e3c8a`:
  readiness preceded its SIGTERM handler. Ancestor repair `af65fb4d5f` installs
  the handler first. The prior execution verified the repaired lifecycle file
  with its exact Node test command (3 passed); that file is unchanged since
  that proof. Reused disposition; no new repair or repeated test needed.

## Goal and scope

A notebook owner publishing several changes can identify the exact added
Portable path that conflicts with a soft-deleted note and correct the proposal.
Preserve the existing rejection policy, error type and fields, original cause,
and all-or-nothing publication. This completes the original story's promise
that a rejected member identifies its path and reason.

Confine the change to publication's error context and its controller proof.
Do not alter shared web create/restore behavior, restore deleted notes, accept
new commit shapes, change transactions, or redesign error transport. Existing
CLI submission already displays `ApiError.message` without modifying local
files or refs; it needs no production change.

## Review evidence and learning

**P2 — deleted-destination rejection omits the offending path.**
`NotebookGitProposalPublisher.applyAddition` calls `NoteFactory.create` at
line 154 outside the authored-property catch at lines 163–176. Creation calls
`NoteTitlePlacementRules.requireNoSoftDeletedTitleAt`, whose message says only
"A note with this title already exists here but was deleted..." and whose
additional field is `deletedNoteId`. Neither identifies the authored path.
`NotebookGitDeletedDestinationControllerTest` currently asserts that exact
generic message. CLI submission renders only `ApiError.message`.

With a valid `Added.md` and conflicting `Physics/Reserved destination.md` in
one commit, the entire proposal is rejected but the owner cannot tell which
file needs correction. The generic error predates this execution; accepting
several additions exposes the ambiguity. The reviewed boundary and current
HEAD are both `6b6bf5ddd8`, with no later fix or pre-existing local edits.

Other aggregate checks found no actionable defect, scope drift, superseded
interim branch, redundant example, or consequential refactoring issue. Review
was static; prior execution test results were inspected, not rerun. Process
retrospective was excluded by the developer.

## Ordered slices

### 1. Identify a deleted destination when rejecting a multi-note commit

Type: Behavior
Status: done
Evidence: Regression reproduced the missing path with one failure among 2,210
backend tests. First fix verification exposed a fixture-only timestamp mismatch:
an in-memory millisecond timestamp was compared with MySQL's whole-second
persisted value. Reading the committed pre-publication baseline fixes that
comparison. Final `CURSOR_DEV=true nix develop -c pnpm backend:test_only` passed
in 47 seconds. Coordinator inspected full-path/reason, fields/cause and committed
rollback assertions. Independent refactor found no edits; required formatting
passed. About 3 minutes active work,
144 seconds across required suite runs, plus environment startup.
Proof: Through `NotebookController.publishNotebookGitProposal`, publish an
eligible mixed proposal with a valid earlier addition/edit and a later addition
at a deleted note's path in a represented folder. Assert the response message
contains that full path and the existing restore-or-retitle reason, retaining
`SOFT_DELETED_TITLE_CONFLICT`, `deletedNoteId`, `_originalMessage`, and the
original cause. Committed-state reads show unchanged original/deleted notes
and accepted binding, with no surviving attempted addition.

Behavior: A clean bound checkout proposes a direct-child commit containing an
addition at a soft-deleted destination → publication rejects everything and
names the conflicting Portable path so the owner can correct that file.

- Extend the existing deleted-destination controller example instead of adding
  another same-boundary fixture framework. Represent the folder with an
  unchanged live note or README in the accepted snapshot; keep every unchanged
  file in the proposed tree. Order a valid change before the conflicting path
  to exercise rollback after mutation.
- Add publication-local path context around the relevant creation error.
  Reuse a cohesive exception-enrichment operation with the existing property
  diagnostic if needed; do not duplicate the field/cause copying or label a
  destination conflict as an invalid authored property. Keep property errors'
  existing semantics and path context.
- Retain the existing publisher transaction and final binding write. Reuse
  committed fixture/rollback helpers and existing CLI rejection propagation
  proof. Do not introduce compensating writes or a new API shape.
- Validation on later execution:
  `CURSOR_DEV=true nix develop -c pnpm backend:test_only` per backend rules.
  Existing property, atomicity, single-note, and placement tests must stay green.
  No broad E2E, manual testing, or mutation testing is needed for this message
  correction.

Sizing: approximately five minutes active implementation and fixture work,
medium confidence; the required backend suite may exceed that by itself.
One rejection outcome and one controller proof loop; no preceding Structure
leaf or further refinement is indicated. Apply the normal execution overrun
gates if this estimate proves wrong.

## Constraints and proof ownership

| Promise | Owning proof |
| --- | --- |
| Exact conflicting path and useful reason | Slice 1 controller response |
| Preserve error fields and cause | Slice 1 response/cause assertions |
| Reject all changes without restoring the deleted note | Slice 1 committed-state observations and existing atomicity tests |
| Owner sees the server diagnostic; local proposal remains | Existing `notebookPublish.submission.suite.ts` transport/state proof |
| Shared create behavior and supported shapes stay unchanged | Existing backend placement and publication regressions |

Accepted ADR 0004's Portable path and format rules remain unchanged. Accepted
ADR 0006 permits context enrichment for a clearer message while preserving the
cause. No storage assumption or transaction ownership changes require a new
experiment. Do not execute this plan as part of the retrospective.

## Reviewed commit manifest

The commits are contiguous and all related. Aggregate boundary:
`74a384f7f4..6b6bf5ddd8`. Planning changes are provenance only.

| Commit | Inclusion reason |
| --- | --- |
| `3ea066f12d` | Per-path rejection and first tracked execution plan |
| `81081ebae2` | Extract note-addition application |
| `117c33e095` | Accept/prove atomic several-addition publication |
| `3dd2f0dada` | Separate accepted-base validation from edit lookup |
| `9eacb24a1d` | Accept/prove mixed additions and identity-preserving edits |
| `e2b85f0330` | Whole mixed-proposal missing-type rejection proof |
| `96eede10d4` | Path-aware authored-property error and proof |
| `5e1534c093` | Actionable unrepresented-parent rejection and proof |
| `6278b53f2b` | Unchanged-state accepted mixed retry proof |
| `72831a75ba` | CLI supported-shape guidance |
| `bf2387a5a0` | Explicit multi-file installed-checkout fixture |
| `b3b462b65b` | Installed CLI mixed publication and note-view proof |
| `4e9fb6809d` | Real-Git multi-note pull proof; all leaves completed |
| `6b6bf5ddd8` | Completed story/backlog update and spent-plan deletion |
