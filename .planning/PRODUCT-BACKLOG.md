# Product backlog

## Near-future direction

Let notebook owners refine notes in Obsidian or an AI IDE and Donut without
discarding either side's work or losing learning history. Other-note and
same-note concurrent content edits can already be pulled and then published,
with ordinary Git pausing on real conflicts. Next among notebook-sync stories
is broader reorganization. This advances the direction in
[Proposed ADR 0002](../docs/adrs/0002-git-native-portable-notebook-synchronization.md);
the selected queue does not cover its complete synchronization contract.
Worktree test isolation takes immediate priority so concurrent development tasks
can verify changes independently.

1. [Use ordinary backend test and migration commands in isolated worktrees](seeds/SEED-015-concurrent-worktree-environments.md#story-1c) — SEED-015
2. [Move a folder while preserving descendant identities](seeds/SEED-009-git-backed-local-notebook-workflow.md#story-7) — SEED-009
3. [See one stable commit for one continuous web edit](seeds/SEED-009-git-backed-local-notebook-workflow.md#story-10) — SEED-009

## Recently done

1. [Resolve an overlapping edit with ordinary Git](seeds/SEED-009-git-backed-local-notebook-workflow.md#story-9) — SEED-009
2. [Run the first backend tests in a fresh worktree without manual setup](seeds/SEED-015-concurrent-worktree-environments.md#story-1b) — SEED-015
3. [Move a note between existing folders without losing its learning history](seeds/SEED-009-git-backed-local-notebook-workflow.md#story-12) — SEED-009
4. [Run concurrent backend tests using explicitly configured databases](seeds/SEED-015-concurrent-worktree-environments.md#story-1a) — SEED-015
5. [Keep non-overlapping accumulated local and web changes](seeds/SEED-009-git-backed-local-notebook-workflow.md#story-8) — SEED-009
6. [Rename a note without losing its learning history](seeds/SEED-009-git-backed-local-notebook-workflow.md#story-6) — SEED-009
7. [Delete a note locally without transferring its private data](seeds/SEED-009-git-backed-local-notebook-workflow.md#story-5) — SEED-009
8. [Return to Donut without browser history blocking login or an unexplained error](seeds/SEED-014-reliable-login-with-browser-history.md#story-1) — SEED-014
9. [Publish several note changes in one commit](seeds/SEED-009-git-backed-local-notebook-workflow.md#story-11) — SEED-009
10. [Create a new note locally](seeds/SEED-009-git-backed-local-notebook-workflow.md#story-4) — SEED-009
11. [Receive a Donut web edit in a clean local repository](seeds/SEED-009-git-backed-local-notebook-workflow.md#story-3) — SEED-009
12. [Publish a local content edit to the same Donut note](seeds/SEED-009-git-backed-local-notebook-workflow.md#story-2) — SEED-009
13. [Open an existing Donut notebook in Obsidian and an AI IDE](seeds/SEED-009-git-backed-local-notebook-workflow.md#story-1) — SEED-009
