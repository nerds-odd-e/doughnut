# Product backlog

## Near-future direction

Let notebook owners refine notes in Obsidian or an AI IDE and Donut without
discarding either side's work or losing learning history. Next, keep web note
creation inside the sequential synchronization loop, then support related edits
to several existing notes in one local commit. Content rebase and bounded local
note/folder reorganization are delivered. Reconciliation across folder moves and
web autosave batching remain unqueued candidates in SEED-009. This advances
[Proposed ADR 0002](../docs/adrs/0002-git-native-portable-notebook-synchronization.md)
without claiming its complete synchronization contract.
Enable developers and AI tasks to verify changes concurrently in local
worktrees without interfering with each other's data or running services.
This development priority still leads the queue. 1a–1c and story 2 delivered
ordinary backend-test isolation and the no-mock concurrent browser workflow
on a persistent worktree identity. Next is independent external-service mocks.
Do not insert cleanup, Cloud VM, or development-profile isolation until a
selected workflow needs them.

1. [Run browser E2E scenarios with independent external-service mocks](seeds/SEED-015-concurrent-worktree-environments.md#story-3) — SEED-015
2. [Create a note on the web and continue refining it locally](seeds/SEED-009-git-backed-local-notebook-workflow.md#story-13) — SEED-009
3. [Publish a related batch of edits to existing notes](seeds/SEED-009-git-backed-local-notebook-workflow.md#story-14) — SEED-009

## Recently done

1. [Run browser E2E scenarios concurrently without external-service mocks](seeds/SEED-015-concurrent-worktree-environments.md#story-2) — SEED-015
2. [Move a folder while preserving descendant identities](seeds/SEED-009-git-backed-local-notebook-workflow.md#story-7) — SEED-009
3. [Use ordinary backend test and migration commands in isolated worktrees](seeds/SEED-015-concurrent-worktree-environments.md#story-1c) — SEED-015
4. [Resolve an overlapping edit with ordinary Git](seeds/SEED-009-git-backed-local-notebook-workflow.md#story-9) — SEED-009
5. [Run the first backend tests in a fresh worktree without manual setup](seeds/SEED-015-concurrent-worktree-environments.md#story-1b) — SEED-015
6. [Move a note between existing folders without losing its learning history](seeds/SEED-009-git-backed-local-notebook-workflow.md#story-12) — SEED-009
7. [Run concurrent backend tests using explicitly configured databases](seeds/SEED-015-concurrent-worktree-environments.md#story-1a) — SEED-015
8. [Keep non-overlapping accumulated local and web changes](seeds/SEED-009-git-backed-local-notebook-workflow.md#story-8) — SEED-009
9. [Rename a note without losing its learning history](seeds/SEED-009-git-backed-local-notebook-workflow.md#story-6) — SEED-009
10. [Delete a note locally without transferring its private data](seeds/SEED-009-git-backed-local-notebook-workflow.md#story-5) — SEED-009
11. [Return to Donut without browser history blocking login or an unexplained error](seeds/SEED-014-reliable-login-with-browser-history.md#story-1) — SEED-014
