# Product backlog

## Near-future direction

Let notebook owners refine notes in Obsidian or an AI IDE and Donut without
discarding either side's work or losing learning history. Other-note and
same-note concurrent content edits can already be pulled and then published,
with ordinary Git pausing on real conflicts. Represented folder relocation
that preserves descendant identities is delivered. Remaining notebook-sync
gaps — unpublished content over an accepted folder move, folders without
their own README, and web structural sync — are not yet stories and are not
in this queue. The next queued notebook-sync story is seeing one stable
commit for one continuous web edit; that is history granularity, not a
synchronization unlock. Pull already rebases over several accepted content
commits. This advances the direction in
[Proposed ADR 0002](../docs/adrs/0002-git-native-portable-notebook-synchronization.md);
the selected queue does not cover its complete synchronization contract.
Enable developers and AI tasks to verify changes concurrently in local
worktrees without interfering with each other's data or running services.
This development priority still leads the queue. 1a–1c delivered opt-in and
ordinary backend-test isolation on a persistent worktree identity; that does
not isolate `pnpm sut`, Cypress, E2E data, or ports. Next is the smallest
usable concurrent browser workflow without external-service mocks, then
independent mocks. Reuse the 1c identity; prove ordinary SUT/Cypress commands
in linked worktrees before broadening to CLI or MCP. Do not insert cleanup,
Cloud VM, or development-profile isolation until a selected workflow needs
them.

1. [Run browser E2E scenarios concurrently without external-service mocks](seeds/SEED-015-concurrent-worktree-environments.md#story-2) — SEED-015
2. [Run browser E2E scenarios with independent external-service mocks](seeds/SEED-015-concurrent-worktree-environments.md#story-3) — SEED-015
3. [See one stable commit for one continuous web edit](seeds/SEED-009-git-backed-local-notebook-workflow.md#story-10) — SEED-009

## Recently done

1. [Move a folder while preserving descendant identities](seeds/SEED-009-git-backed-local-notebook-workflow.md#story-7) — SEED-009
2. [Use ordinary backend test and migration commands in isolated worktrees](seeds/SEED-015-concurrent-worktree-environments.md#story-1c) — SEED-015
3. [Resolve an overlapping edit with ordinary Git](seeds/SEED-009-git-backed-local-notebook-workflow.md#story-9) — SEED-009
4. [Run the first backend tests in a fresh worktree without manual setup](seeds/SEED-015-concurrent-worktree-environments.md#story-1b) — SEED-015
5. [Move a note between existing folders without losing its learning history](seeds/SEED-009-git-backed-local-notebook-workflow.md#story-12) — SEED-009
6. [Run concurrent backend tests using explicitly configured databases](seeds/SEED-015-concurrent-worktree-environments.md#story-1a) — SEED-015
7. [Keep non-overlapping accumulated local and web changes](seeds/SEED-009-git-backed-local-notebook-workflow.md#story-8) — SEED-009
8. [Rename a note without losing its learning history](seeds/SEED-009-git-backed-local-notebook-workflow.md#story-6) — SEED-009
9. [Delete a note locally without transferring its private data](seeds/SEED-009-git-backed-local-notebook-workflow.md#story-5) — SEED-009
10. [Return to Donut without browser history blocking login or an unexplained error](seeds/SEED-014-reliable-login-with-browser-history.md#story-1) — SEED-014
