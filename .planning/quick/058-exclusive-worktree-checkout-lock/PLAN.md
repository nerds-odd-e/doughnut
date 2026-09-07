# Exclusive worktree checkout lock after first-use

Status: planned.
Source: [SEED-015 story 1b](../../seeds/SEED-015-concurrent-worktree-environments.md#story-1b)
via completed [quick/057](../057-automatic-worktree-backend-tests/PLAN.md).
Reviewed commits: `162feeacca`..`987d0fbc52` (first-parent execution set).

## Goal and scope

Keep the delivered automatic first-use workflow, and close three leftover
checkout-ownership gaps: overlapping reclaimers of a dead owner must still
leave one test process; the leftover lock must not appear as untracked Git
state; the command-boundary fixture that grew past the line limit must be
split so the overlap proof can extend it.

Do not change provisioning, identity format, database grants, ordinary
`backend:test` entry points, lock retirement/cleanup of databases, or add a
wait queue or process supervisor.

## Outside-in proof

| Promise | Owning slice | Observable signal |
|---|---:|---|
| Command-boundary fixture stays under the 250-line limit | 1 | Each split module is ≤250 lines; `pnpm test:backend-test-worktree` stays green |
| Two overlapping reclaimers of a stale lock never share Gradle | 2 | Two command-boundary processes against one stale lock and valid config: one reaches Gradle, the other refuses before Gradle |
| The checkout lock is ignored like the identity file | 3 | `git check-ignore -v .worktree.local.lock` matches a root ignore entry; the guide names that entry |

## Current decisions

- Reclaim must use the same exclusive-creation rule as first acquire. Replacing
  `owner.pid` with `mv` is not exclusive: two processes that both observe a
  dead PID can both proceed into Gradle against the same database. Do not add
  a wait queue, trap, or supervisor. Live and malformed records remain
  immediate refusals. Follow [ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md).
- Ignore `/.worktree.local.lock` at the repo root next to `/.worktree.local.json`.
  The lock directory is still left in place after `exec`; reclaim remains the
  reuse path. Ignoring it is not lock retirement.
- Split the fixture along cohesive seams (stand-ins vs launchers vs lock
  helpers). Do not export allocation helpers for direct tests.

## Execution context

- `scripts/backend-test-worktree.sh` creates `.worktree.local.lock` with
  `mkdir`, writes `owner.pid`, and on a dead numeric PID writes a sibling temp
  file then `mv`s it over `owner.pid`.
- Command-boundary tests: `scripts/backend-test-worktree-lock.test.mjs` (live
  owner, malformed record, other checkout, single stale reclaim).
  `scripts/backend-test-worktree-test-fixtures.mjs` is 277 lines.
- `.gitignore` has `/.worktree.local.json` only. Slice 12 of quick/057 needed
  `git worktree remove --force` because the leftover lock directory was
  untracked.
- Focused proof: `CURSOR_DEV=true nix develop -c pnpm test:backend-test-worktree`.

## Ordered slices

### 1. Split the oversized worktree launcher fixture
Type: Structure
Status: planned
Proof: `wc -l` on each resulting fixture module is ≤250. Historical comments
that record superseded missing-config refusal or slice numbers are gone from
the launcher test files. Existing tests remain green:
`CURSOR_DEV=true nix develop -c pnpm test:backend-test-worktree`.

Internal change: Split `scripts/backend-test-worktree-test-fixtures.mjs`
along cohesive seams so lock overlap helpers can be added without growing a
file already over the limit. Immediate next Behavior: slice 2.

Sizing: about 5 minutes, high confidence; file split and import updates only.

### 2. Refuse a second reclaimer of a stale checkout lock
Type: Behavior
Status: planned
Proof: Given a valid `.worktree.local.json` and a stale owner PID, start two
launcher processes without waiting for the first to reach Gradle. One reaches
Gradle against the configured database; the other exits nonzero with the
existing in-checkout owner message and never writes a Gradle invocation.
Single stale reclaim, live owner, and malformed record remain as they are.
`CURSOR_DEV=true nix develop -c pnpm test:backend-test-worktree`.

Behavior: A previous launcher has exited and left a dead owner record, and two
later commands start in that checkout → only one owns the run and reaches
tests; the other refuses rather than sharing the database.

Close the reclaim TOCTOU with exclusive creation, not `mv` over `owner.pid`.
Do not wait, retry past a live owner, or serialize across checkouts.

Sizing: about 5 minutes, medium confidence; one overlap proof on the existing
async launcher fixture.

### 3. Ignore the leftover checkout lock in Git
Type: Behavior
Status: planned
Proof: `git check-ignore -v .worktree.local.lock` reports the root ignore
entry. `docs/worktree-backend-tests.md` names that entry beside
`.worktree.local.json`. Identity-file ignore and lock/reclaim behavior stay
unchanged.
`CURSOR_DEV=true nix develop -c pnpm test:backend-test-worktree`.

Behavior: A checkout has run the opt-in command and still has
`.worktree.local.lock` on disk → Git treats that path as ignored, so status
and worktree removal are not dirtied by the leftover lock.

Sizing: about 5 minutes, high confidence; one ignore entry and guide sentence.

## Learnings

Execution retrospective of quick/057 (`162feeacca`–`987d0fbc52`): first-use
provisioning, reuse, live-owner refusal, and real two-worktree proof stand.
Remaining gaps are exclusive stale reclaim, gitignore for the lock directory,
and the 277-line fixture.

## Considered but excluded

- `flock`, a supervisor, or a wait queue.
- Cleaning the spent quick/057 PLAN diary, or rewriting SEED-015 story 1b
  (uncommitted seed edits are preserved; parent status already records 1b
  delivered).
- An explicit “reused environment” print on later invocations.
- Ordinary `backend:test` / `backend:test_only` integration (story 1c).
