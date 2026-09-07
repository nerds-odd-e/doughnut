# Exclusive worktree checkout lock after first-use

Status: complete.
Source: [SEED-015 story 1b](../../seeds/SEED-015-concurrent-worktree-environments.md#story-1b).
Reviewed first-use commits: `162feeacca`..`987d0fbc52` (recover the completed
quick/057 PLAN from `fce6bd68f4`).

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

- Reclaim uses exclusive `mkdir` of `reclaimed.<pid>`, the same exclusive-creation
  rule as first acquire. Live and malformed records remain immediate refusals.
  Follow [ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md).
- Ignore `/.worktree.local.lock` at the repo root next to `/.worktree.local.json`.
  The lock directory stays after `exec`; reclaim is the reuse path.
- Fixtures split along stand-ins / launchers / lock helpers. Allocation helpers
  stay unexported.

## Ordered slices

### 1. Split the oversized worktree launcher fixture
Type: Structure
Status: done
Proof: stand-in 153 / launcher 101 / lock 24 lines.
`CURSOR_DEV=true nix develop -c pnpm test:backend-test-worktree`.

### 2. Refuse a second reclaimer of a stale checkout lock
Type: Behavior
Status: done
Proof: two reclaimers against one stale lock; one Gradle owner, one
in-checkout refusal before Gradle.
`CURSOR_DEV=true nix develop -c pnpm test:backend-test-worktree`.

### 3. Ignore the leftover checkout lock in Git
Type: Behavior
Status: done
Proof: `git check-ignore -v .worktree.local.lock` →
`.gitignore:159:/.worktree.local.lock`. Guide names that entry beside
`.worktree.local.json`.
`CURSOR_DEV=true nix develop -c pnpm test:backend-test-worktree`.

## Considered but excluded

- `flock`, a supervisor, or a wait queue.
- Cleaning the spent quick/057 PLAN diary, or rewriting SEED-015 story 1b
  (uncommitted seed edits are preserved; parent status already records 1b
  delivered).
- An explicit “reused environment” print on later invocations.
- Ordinary `backend:test` / `backend:test_only` integration (story 1c).
