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

- [Identify large-publication bottlenecks with a reproducible baseline](seeds/SEED-018-publish-large-authored-notebooks.md#story-2) — SEED-018
- [Preserve pipe wiki links through rich editing](quick/107-preserve-pipe-wiki-links-through-rich-editing/PLAN.md)

## Backlog list

- [Publish large notebook commits within a practical measured time](seeds/SEED-018-publish-large-authored-notebooks.md#story-3) — SEED-018
- [Restart primary SUT only with proven process ownership](quick/104-refuse-unowned-primary-sut-restart/PLAN.md)
