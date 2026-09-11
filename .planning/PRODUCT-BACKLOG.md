# Product backlog

## Near-future direction

Developers and AI tasks can verify changes concurrently in local worktrees
without interfering with each other's data or running services. That remains
the leading product direction. Backend-test isolation (1a–1c), concurrent
browser workflows (2, 2a), owned SUT descendant shutdown (2b), independent
OpenAI browser mocks (3), reclaiming disposable worktree databases (6), and
the retirement eligibility correction (6a) are delivered. CLI and MCP isolation
are also delivered. Persistent Development for manual feedback (7) is delivered.
Cloud VM isolation remains deferred.

Notebook owners can refine notes in Obsidian or an AI IDE and Donut without
discarding either side's work or losing learning history for the selected
SEED-009 v1 stories through 18a, including web autosave batching (10) and its
durable fractional-second clock correction (10a). That advances
[Proposed ADR 0002](../docs/adrs/0002-git-native-portable-notebook-synchronization.md)
without claiming its complete synchronization contract. Further SEED-009
candidates stay unselected until after the worktree queue unless priority
changes.

## Taken

- [Declare isolated test capabilities in one place](seeds/SEED-017-cohesive-design-corrections.md#story-3) — SEED-017
- [Publish several note changes in one commit, including new folders](seeds/SEED-009-git-backed-local-notebook-workflow.md#story-11) — SEED-009

## Backlog

- [Release backend worktree test ownership after execution](quick/102-release-backend-worktree-test-ownership/PLAN.md)
- [Keep one owned-process termination mechanism](seeds/SEED-017-cohesive-design-corrections.md#story-4) — SEED-017
