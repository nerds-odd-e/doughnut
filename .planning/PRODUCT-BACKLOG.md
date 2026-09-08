# Product backlog

## Near-future direction

Let notebook owners refine notes in Obsidian or an AI IDE and Donut without
discarding either side's work or losing learning history. Web note creation and
keeping one unpublished local content edit across one accepted addition are
delivered. Next selected SEED-009 story is related edits to several existing
notes in one local commit. Content rebase and bounded local note/folder
reorganization are delivered. Reconciliation across folder moves and web
autosave batching remain unqueued candidates in SEED-009. This advances
[Proposed ADR 0002](../docs/adrs/0002-git-native-portable-notebook-synchronization.md)
without claiming its complete synchronization contract.
Enable developers and AI tasks to verify changes concurrently in local
worktrees without interfering with each other's data or running services.
This development priority still leads the queue. 1a–1c delivered opt-in and
ordinary backend-test isolation on a persistent worktree identity; that does
not isolate `pnpm sut`, Cypress, E2E data, or ports. Next is the smallest
usable concurrent browser workflow without external-service mocks, then
independent mocks. Reuse the 1c identity; prove ordinary SUT/Cypress commands
in linked worktrees, then reclaim retired databases to limit accumulation,
followed by CLI and MCP isolation. Cloud VM and development-profile isolation
remain deferred.

1. [Run browser E2E scenarios concurrently without external-service mocks](seeds/SEED-015-concurrent-worktree-environments.md#story-2) — SEED-015
2. [Run browser E2E scenarios with independent external-service mocks](seeds/SEED-015-concurrent-worktree-environments.md#story-3) — SEED-015
3. [Reclaim databases from retired worktrees](seeds/SEED-015-concurrent-worktree-environments.md#story-6) — SEED-015
4. [Run CLI E2E workflows against the owning worktree's environment](seeds/SEED-015-concurrent-worktree-environments.md#story-4) — SEED-015
5. [Run MCP E2E workflows against the owning worktree's environment](seeds/SEED-015-concurrent-worktree-environments.md#story-5) — SEED-015
6. [Publish a related batch of edits to existing notes](seeds/SEED-009-git-backed-local-notebook-workflow.md#story-14) — SEED-009
7. [Keep a local note edit when a web-created note is then saved](seeds/SEED-009-git-backed-local-notebook-workflow.md#story-17) — SEED-009

## Recently done

1. [Keep a local note edit when accepted history adds a different note](seeds/SEED-009-git-backed-local-notebook-workflow.md#story-16) — SEED-009
2. [Create a note on the web and continue refining it locally](seeds/SEED-009-git-backed-local-notebook-workflow.md#story-13) — SEED-009
3. [Move a folder while preserving descendant identities](seeds/SEED-009-git-backed-local-notebook-workflow.md#story-7) — SEED-009
4. [Use ordinary backend test and migration commands in isolated worktrees](seeds/SEED-015-concurrent-worktree-environments.md#story-1c) — SEED-015
5. [Resolve an overlapping edit with ordinary Git](seeds/SEED-009-git-backed-local-notebook-workflow.md#story-9) — SEED-009
6. [Run the first backend tests in a fresh worktree without manual setup](seeds/SEED-015-concurrent-worktree-environments.md#story-1b) — SEED-015
7. [Move a note between existing folders without losing its learning history](seeds/SEED-009-git-backed-local-notebook-workflow.md#story-12) — SEED-009
8. [Run concurrent backend tests using explicitly configured databases](seeds/SEED-015-concurrent-worktree-environments.md#story-1a) — SEED-015
9. [Keep non-overlapping accumulated local and web changes](seeds/SEED-009-git-backed-local-notebook-workflow.md#story-8) — SEED-009
10. [Rename a note without losing its learning history](seeds/SEED-009-git-backed-local-notebook-workflow.md#story-6) — SEED-009
