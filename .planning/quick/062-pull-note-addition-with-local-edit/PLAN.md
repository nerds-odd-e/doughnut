# Receive one added note beside a local edit

## Source

[SEED-009 story 16](../../seeds/SEED-009-git-backed-local-notebook-workflow.md#story-16).
Status: delivered.

## Goal and scope

Receive one web-created ordinary note while retaining one committed local
refinement, then explicitly publish the refinement on its original identity.

- Bound, clean `main`; exactly one unpublished single-parent commit changing
  one existing ordinary note's body/frontmatter at its unchanged path.
- The accepted interval from that commit's parent is exactly one single-parent
  commit adding one different ordinary note, with no other tree changes.
  Addition destination is root or an already represented folder; the remote
  projection matches accepted history.
- Pull keeps the added note and local edit, leaves the edit unpublished, and
  preserves accepted commit IDs. Explicit publish retains both note identities
  and their private learning data.
- Exclude dirty worktrees/automatic stash, multiple local commits or edited
  notes, multiple remote commits/additions, addition plus content edits,
  collisions, moves, renames, deletes, README changes, new folders, and drift
  repair. Creation followed by another web save is deliberately excluded.
  Existing content-only rebase and clean fast-forward behavior remain supported.

## Ordered slices

### 1. Pull one addition while retaining one unpublished edit

Type: Behavior
Status: done
Proof: `CURSOR_DEV=true nix develop -c pnpm -C cli exec vitest run tests/notebookPull.test.ts tests/notebookClone.test.ts` (74 tests)

Behavior: One clean bound checkout has one existing-note content commit and
accepted main has one different-note addition → run notebook pull → receive the
addition beneath the retained unpublished edit, with all other structural
intervals still refused.

### 2. Publish the retained edit without changing either learned identity

Type: Behavior
Status: done
Proof: `CURSOR_DEV=true nix develop -c pnpm backend:test_only`

Behavior: Accepted creation of Beta follows the shared base; both Alpha and
Beta have private learning state; a direct-child proposal changes only Alpha →
publish through the controller → original Alpha receives the edit and both
notes retain their identities and learning state.

### 3. Complete the web-capture and local-refinement journey

Type: Behavior
Status: done
Proof: `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/cli/cli_notebook_web_created_note.feature` (3 scenarios)

Behavior: Clone and commit an edit to Recipes/Pasta, then create a title-only
Shopping list on the web → installed CLI pull followed by explicit publish →
the web capture is retained and Donut's original Pasta receives the local text.

## Delivery

All slices delivered. Enduring behavior lives in CLI/backend/E2E tests and the
story Goal/Scope in SEED-009.
