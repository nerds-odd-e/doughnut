# Keep a local edit across web creation and one save

Source: [SEED-009 Story 17](../../seeds/SEED-009-git-backed-local-notebook-workflow.md#story-17).
Status: complete.

## Goal and scope

An owner receives web-created B's first saved content while retaining one
committed local edit to existing A, then explicitly publishes A onto its same
learned identity. Clean bound `main`; exactly one unpublished single-parent,
single-note content commit. Remote interval is exactly two linear commits:
one ordinary-note addition directly on the shared base, then one content-only
save of that same path. B is at root or in a folder represented at the base.

Preserve accepted commit IDs, both note identities and learning data, and
recoverable local work. Pull does not publish. Refuse larger or differently
shaped intervals without changing local HEAD/files or accepted history.
Keep delivered content-only and single-addition receipt behavior.

Exclude local batches, multiple local commits, additional remote saves or
additions, other-note remote edits in this interval, moves, renames, deletions,
README/new-folder changes, dirty checkouts, drift repair, new UI/commands,
autosave batching, and general structural reconciliation. A no-op save with
no accepted commit still uses existing single-addition support.

## Ordered slices

### 1. Receive the saved new note while keeping the local edit

Type: Behavior
Status: done
Behavior: Local A is committed but unpublished; accepted history creates B and
saves B once → `donut notebook pull` → clean local main contains saved B and
local A as one unpublished child of the unchanged accepted head.

Proof: CLI success (root + represented folder) and refusal variants;
browser receipt ends after pull with server A unchanged.
`pnpm -C cli exec vitest run tests/notebookPull.test.ts`;
`pnpm cypress run --spec e2e_test/features/cli/cli_notebook_web_created_note.feature`.

### 2. Publish the retained edit onto the same learned note

Type: Behavior
Status: done
Behavior: Slice 1 has retained A over accepted creation/save of B → explicit
`donut notebook publish` → Donut accepts A's edit while B's saved content,
both identities, and both notes' learning data remain intact.

Proof: Controller creation-then-save publication retains note/tracker IDs,
recall/scheduling, both contents, and creation→save history;
browser publish shows both final contents.
`pnpm backend:test_only`; same focused Cypress feature.

## Promise ownership

| Promise | Leaf and observation |
| --- | --- |
| Exact creation/save interval, root and represented folder | 1: CLI success fixtures and browser receipt |
| Retained unpublished edit, immutable accepted chain, no pull publication | 1: Git ancestry/content and HTTP-call assertions; server A unchanged |
| Refused excluded shapes preserve local work and remote state | 1: unchanged-checkout assertions and no publication call |
| Explicit publication preserves both contents, identities, learning data | 2: controller tracker/history assertions and browser publication |
