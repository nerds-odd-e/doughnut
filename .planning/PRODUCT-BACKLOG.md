# Product backlog

## Near-future direction

Let notebook owners refine notes in Obsidian or an AI IDE and Donut without
discarding either side's work or losing learning history. Web note creation,
keeping one unpublished local content edit across one accepted addition or
across creation-then-save of a different note, publishing related edits to
several existing notes in one local commit, keeping a related local edit batch
across one disjoint web save of a different note, and keeping a local note edit
across one accepted folder move are delivered. Content rebase and bounded local
note/folder reorganization are delivered. Web autosave batching (Story 10) is
delivered. This advances
[Proposed ADR 0002](../docs/adrs/0002-git-native-portable-notebook-synchronization.md)
without claiming its complete synchronization contract.
Enable developers and AI tasks to verify changes concurrently in local
worktrees without interfering with each other's data or running services.
This development priority still leads the queue. 1a–1c, stories 2, 2a, and 3
delivered ordinary backend-test isolation, concurrent browser workflows, and
independent OpenAI browser mocks. Next is reclaiming retired databases to limit
accumulation, followed by CLI and MCP isolation. Cloud VM and development-profile
isolation remain deferred.

1. [Stop an isolated SUT without leaving its forked backend running](seeds/SEED-015-concurrent-worktree-environments.md#story-2b) — SEED-015
2. [Apply the web edit batching interval accurately across durable saves](seeds/SEED-009-git-backed-local-notebook-workflow.md#story-10a) — SEED-009
3. [Reclaim databases from retired worktrees](seeds/SEED-015-concurrent-worktree-environments.md#story-6) — SEED-015
4. [Run CLI E2E workflows against the owning worktree's environment](seeds/SEED-015-concurrent-worktree-environments.md#story-4) — SEED-015
5. [Run MCP E2E workflows against the owning worktree's environment](seeds/SEED-015-concurrent-worktree-environments.md#story-5) — SEED-015

## Recently done

1. [See one stable commit for one continuous web edit](seeds/SEED-009-git-backed-local-notebook-workflow.md#story-10) — SEED-009
2. [Run browser E2E scenarios with independent external-service mocks](seeds/SEED-015-concurrent-worktree-environments.md#story-3) — SEED-015
3. [Keep a local note edit across an accepted folder move](seeds/SEED-009-git-backed-local-notebook-workflow.md#story-15) — SEED-009
4. [Refuse isolated browser verification against an unverified allocation](seeds/SEED-015-concurrent-worktree-environments.md#story-2a) — SEED-015
5. [Understand how to proceed when batch pull refuses divergent history](seeds/SEED-009-git-backed-local-notebook-workflow.md#story-18a) — SEED-009
6. [Keep a related local edit batch when the web changes a different note](seeds/SEED-009-git-backed-local-notebook-workflow.md#story-18) — SEED-009
7. [Run browser E2E scenarios concurrently without external-service mocks](seeds/SEED-015-concurrent-worktree-environments.md#story-2) — SEED-015
8. [Keep a local note edit when a web-created note is then saved](seeds/SEED-009-git-backed-local-notebook-workflow.md#story-17) — SEED-009
9. [Publish a related batch of edits to existing notes](seeds/SEED-009-git-backed-local-notebook-workflow.md#story-14) — SEED-009
10. [Keep a local note edit when accepted history adds a different note](seeds/SEED-009-git-backed-local-notebook-workflow.md#story-16) — SEED-009
