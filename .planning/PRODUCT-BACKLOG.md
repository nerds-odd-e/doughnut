# Product backlog

## Near-future direction

Let notebook owners refine notes in Obsidian or an AI IDE and Donut without
discarding either side's work or losing learning history. Non-overlapping
other-note concurrent edits can already be pulled and then published. Next
among notebook-sync stories is overlapping same-note handling, then broader
reorganization. This advances the direction in
[Proposed ADR 0002](../docs/adrs/0002-git-native-portable-notebook-synchronization.md);
the selected queue does not cover its complete synchronization contract.
Worktree test isolation takes immediate priority so concurrent development tasks
can verify changes independently.

1. [Run the first backend tests in a fresh worktree without manual setup](seeds/SEED-015-concurrent-worktree-environments.md#story-1b) — SEED-015
2. [Use ordinary backend test and migration commands in isolated worktrees](seeds/SEED-015-concurrent-worktree-environments.md#story-1c) — SEED-015
3. [Resolve an overlapping edit with ordinary Git](seeds/SEED-009-git-backed-local-notebook-workflow.md#story-9) — SEED-009
4. [Move a folder while preserving descendant identities](seeds/SEED-009-git-backed-local-notebook-workflow.md#story-7) — SEED-009
5. [See one stable commit for one continuous web edit](seeds/SEED-009-git-backed-local-notebook-workflow.md#story-10) — SEED-009

## Recently done

1. [Move a note between existing folders without losing its learning history](seeds/SEED-009-git-backed-local-notebook-workflow.md#story-12) — SEED-009
2. [Run concurrent backend tests using explicitly configured databases](seeds/SEED-015-concurrent-worktree-environments.md#story-1a) — SEED-015
3. [Keep non-overlapping accumulated local and web changes](seeds/SEED-009-git-backed-local-notebook-workflow.md#story-8) — SEED-009
4. [Rename a note without losing its learning history](seeds/SEED-009-git-backed-local-notebook-workflow.md#story-6) — SEED-009
5. [Delete a note locally without transferring its private data](seeds/SEED-009-git-backed-local-notebook-workflow.md#story-5) — SEED-009
6. [Return to Donut without browser history blocking login or an unexplained error](seeds/SEED-014-reliable-login-with-browser-history.md#story-1) — SEED-014
7. [Publish several note changes in one commit](seeds/SEED-009-git-backed-local-notebook-workflow.md#story-11) — SEED-009
8. [Create a new note locally](seeds/SEED-009-git-backed-local-notebook-workflow.md#story-4) — SEED-009
9. [Receive a Donut web edit in a clean local repository](seeds/SEED-009-git-backed-local-notebook-workflow.md#story-3) — SEED-009
10. [Publish a local content edit to the same Donut note](seeds/SEED-009-git-backed-local-notebook-workflow.md#story-2) — SEED-009
11. [Open an existing Donut notebook in Obsidian and an AI IDE](seeds/SEED-009-git-backed-local-notebook-workflow.md#story-1) — SEED-009
