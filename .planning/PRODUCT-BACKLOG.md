# Product backlog

## Near-future direction

Developers and AI tasks can verify changes concurrently in local worktrees
without interfering with each other's data or running services. That remains
the leading product direction. Backend-test isolation (1a–1c), concurrent
browser workflows (2, 2a), owned SUT descendant shutdown (2b), independent
OpenAI browser mocks (3), and reclaiming disposable worktree databases (6)
are delivered. Next is CLI and MCP isolation. Cloud VM and development-profile
isolation remain deferred. Quick/073 is an outstanding proof correction for
delivered story 3, not a separate queue story.

Notebook owners can refine notes in Obsidian or an AI IDE and Donut without
discarding either side's work or losing learning history for the selected
SEED-009 v1 stories through 18a, including web autosave batching (10) and its
durable fractional-second clock correction (10a). That advances
[Proposed ADR 0002](../docs/adrs/0002-git-native-portable-notebook-synchronization.md)
without claiming its complete synchronization contract. Further SEED-009
candidates stay unselected until after the worktree queue unless priority
changes.

## Backlog

- [Run CLI E2E workflows against the owning worktree's environment](seeds/SEED-015-concurrent-worktree-environments.md#story-4) — SEED-015
- [Run MCP E2E workflows against the owning worktree's environment](seeds/SEED-015-concurrent-worktree-environments.md#story-5) — SEED-015

## Recently done

- [Reclaim databases from retired worktrees](seeds/SEED-015-concurrent-worktree-environments.md#story-6) — SEED-015
- [Apply the web edit batching interval accurately across durable saves](seeds/SEED-009-git-backed-local-notebook-workflow.md#story-10a) — SEED-009
- [Stop an isolated SUT without leaving its forked backend running](seeds/SEED-015-concurrent-worktree-environments.md#story-2b) — SEED-015
- [See one stable commit for one continuous web edit](seeds/SEED-009-git-backed-local-notebook-workflow.md#story-10) — SEED-009
- [Run browser E2E scenarios with independent external-service mocks](seeds/SEED-015-concurrent-worktree-environments.md#story-3) — SEED-015
- [Keep a local note edit across an accepted folder move](seeds/SEED-009-git-backed-local-notebook-workflow.md#story-15) — SEED-009
- [Refuse isolated browser verification against an unverified allocation](seeds/SEED-015-concurrent-worktree-environments.md#story-2a) — SEED-015
- [Understand how to proceed when batch pull refuses divergent history](seeds/SEED-009-git-backed-local-notebook-workflow.md#story-18a) — SEED-009
- [Keep a related local edit batch when the web changes a different note](seeds/SEED-009-git-backed-local-notebook-workflow.md#story-18) — SEED-009
- [Run browser E2E scenarios concurrently without external-service mocks](seeds/SEED-015-concurrent-worktree-environments.md#story-2) — SEED-015
- [Keep a local note edit when a web-created note is then saved](seeds/SEED-009-git-backed-local-notebook-workflow.md#story-17) — SEED-009
