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

## Backlog

- [Publish unambiguous note moves alongside compatible note changes](seeds/SEED-017-cohesive-design-corrections.md#story-2b) — SEED-017
- [Keep one owned-process termination mechanism](seeds/SEED-017-cohesive-design-corrections.md#story-4) — SEED-017

## Recently done

- [Use a persistent Development environment for manual feedback](seeds/SEED-015-concurrent-worktree-environments.md#story-7) — SEED-015
- [Publish two notes and their relationship together](seeds/SEED-016-initial-notebook-and-folder-readmes.md#story-7) — SEED-016
- [Publish three small initial layouts exposed by the jap1 failure](seeds/SEED-016-initial-notebook-and-folder-readmes.md#story-6) — SEED-016
- [Run MCP E2E workflows against the owning worktree's environment](seeds/SEED-015-concurrent-worktree-environments.md#story-5) — SEED-015
- [Run one non-interactive CLI E2E workflow against the owning worktree's environment](seeds/SEED-015-concurrent-worktree-environments.md#story-4) — SEED-015
- [Publish the next small initial Readme-and-Note trees](seeds/SEED-016-initial-notebook-and-folder-readmes.md#story-5) — SEED-016
- [Publish a minimal initial container with one note](seeds/SEED-016-initial-notebook-and-folder-readmes.md#story-4) — SEED-016
- [Publish the initial notebook README by itself](seeds/SEED-016-initial-notebook-and-folder-readmes.md#story-3) — SEED-016
- [Publish one initial note inside the new README-backed folder](seeds/SEED-016-initial-notebook-and-folder-readmes.md#story-2) — SEED-016
- [Publish the initial notebook README with one README-only folder](seeds/SEED-016-initial-notebook-and-folder-readmes.md#story-1) — SEED-016
