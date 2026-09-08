# Keep a local edit batch across a disjoint web save

## Source, goal, and scope

Source: [SEED-009 story 18](../../seeds/SEED-009-git-backed-local-notebook-workflow.md#story-18).

An owner retains one related two-note local revision across one web save to a
third note, then explicitly publishes it with learning history intact.

Bound clean `main`; exactly one unpublished single-parent commit edits exactly
two existing ordinary Markdown notes A/B at unchanged root or represented nested
paths. Accepted main advances from their shared parent by exactly one
single-parent content commit editing only existing note C. Valid supported
body/frontmatter and matching server projection are preconditions.

Exclude overlap, three-or-more-note local batches, additional local/accepted
commits, additions (including creation-then-save), deletes, renames, moves,
README/folder changes, dirty trees, drift repair, new commands/UI, and conflict
policy. Already-based batch pull, including repeating pull after rebase, stays
outside this increment. Existing unchanged-base batch publication still works.

## Execution context and decisions

- Two-note/disjoint-one-save eligibility lives in `notebookLocalCandidate` with
  parent-edge checks; pull reuses the existing native rebase path.
- Do not loosen `firstStructuralPathInAcceptedInterval` for batches.
- Portable trees stay free of identity metadata (ADR 0004). Unsupported shapes
  fail deliberately (ADR 0006). ADR 0002 remains Proposed.

## Ordered slices

### 1. Receive the two-note batch across one disjoint save

Type: Behavior
Status: done
Proof: `pnpm -C cli exec vitest run tests/notebookPull.test.ts tests/notebookClone.test.ts`
(90 passed) — A/B retained as one unpublished child of accepted C; original
recoverable; no publish; unsupported shapes refuse; guidance updated.

### 2. Publish the retained revision through the existing CLI flow

Type: Behavior
Status: done
Proof: `pnpm cypress run --spec e2e_test/features/cli/cli_notebook_existing_note_edits.feature`
(2/2) — pull then publish lands A/B/C in Donut; server A/B unchanged before publish.

### 3. Keep learned notes intact when publishing onto the web save

Type: Behavior
Status: done
Proof: `pnpm backend:test_only` (~54s) —
`keepsAllThreeLearnedNotesWhenPublishingTwoNoteRevisionOntoAcceptedWebSave`
retains A/B/C IDs and learning via `assertShownContentAndRetainedLearning`.

## Promise ownership

| Promise | Owner and observable proof |
| --- | --- |
| Exact eligible shape; root/nested and authored content retained | Slice 1 CLI result and Git tree |
| One unpublished child; original commit recoverable; immutable accepted C | Slice 1 ancestry, original object/reflog, and SHA assertions |
| Pull never publishes or changes server A/B | Slice 1 no publish call; slice 2 live server content |
| Unsupported local/remote shapes preserve work; existing one-note cases survive | Slice 1 rejection variations and existing pull suites |
| Guidance accurately states this bounded case | Slice 1 usage/clone output assertions |
| Explicit publication accepts whole revision onto C | Slice 2 installed CLI and server contents |
| A/B/C identities and learning history retained | Slice 3 controller observations |

## Current status

All slices done. Story 18 delivered.
