# Product backlog

## Near-future direction

Let notebook owners refine notes in Obsidian or an AI IDE and Donut without
discarding either side's work or losing learning history. Web note creation,
keeping one unpublished local content edit across one accepted addition or
across creation-then-save of a different note, publishing related edits to
several existing notes in one local commit, and keeping a related local edit
batch across one disjoint web save of a different note are delivered. Content
rebase and bounded local note/folder reorganization are delivered.
Reconciliation across folder moves and web autosave batching remain unqueued
candidates in SEED-009. This advances
[Proposed ADR 0002](../docs/adrs/0002-git-native-portable-notebook-synchronization.md)
without claiming its complete synchronization contract.
Enable developers and AI tasks to verify changes concurrently in local
worktrees without interfering with each other's data or running services.
This development priority still leads the queue. 1a–1c, story 2, and story 2a
delivered ordinary backend-test isolation, the no-mock concurrent browser
workflow, and refusal of unverified browser allocations on a persistent
worktree identity. Next is independent external-service mocks,
then reclaim retired databases to limit accumulation, followed by CLI and
MCP isolation. Cloud VM and development-profile isolation remain deferred.

1. [Run browser E2E scenarios with independent external-service mocks](seeds/SEED-015-concurrent-worktree-environments.md#story-3) — SEED-015
2. [Reclaim databases from retired worktrees](seeds/SEED-015-concurrent-worktree-environments.md#story-6) — SEED-015
3. [Run CLI E2E workflows against the owning worktree's environment](seeds/SEED-015-concurrent-worktree-environments.md#story-4) — SEED-015
4. [Run MCP E2E workflows against the owning worktree's environment](seeds/SEED-015-concurrent-worktree-environments.md#story-5) — SEED-015

## Recently done

1. [Refuse isolated browser verification against an unverified allocation](seeds/SEED-015-concurrent-worktree-environments.md#story-2a) — SEED-015
2. [Understand how to proceed when batch pull refuses divergent history](seeds/SEED-009-git-backed-local-notebook-workflow.md#story-18a) — SEED-009
3. [Keep a related local edit batch when the web changes a different note](seeds/SEED-009-git-backed-local-notebook-workflow.md#story-18) — SEED-009
4. [Run browser E2E scenarios concurrently without external-service mocks](seeds/SEED-015-concurrent-worktree-environments.md#story-2) — SEED-015
5. [Keep a local note edit when a web-created note is then saved](seeds/SEED-009-git-backed-local-notebook-workflow.md#story-17) — SEED-009
6. [Publish a related batch of edits to existing notes](seeds/SEED-009-git-backed-local-notebook-workflow.md#story-14) — SEED-009
7. [Keep a local note edit when accepted history adds a different note](seeds/SEED-009-git-backed-local-notebook-workflow.md#story-16) — SEED-009
8. [Create a note on the web and continue refining it locally](seeds/SEED-009-git-backed-local-notebook-workflow.md#story-13) — SEED-009
9. [Move a folder while preserving descendant identities](seeds/SEED-009-git-backed-local-notebook-workflow.md#story-7) — SEED-009
10. [Use ordinary backend test and migration commands in isolated worktrees](seeds/SEED-015-concurrent-worktree-environments.md#story-1c) — SEED-015
11. [Resolve an overlapping edit with ordinary Git](seeds/SEED-009-git-backed-local-notebook-workflow.md#story-9) — SEED-009
