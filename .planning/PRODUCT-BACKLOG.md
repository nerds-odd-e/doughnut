# Product backlog

## Near-future direction

Let notebook owners refine notes in Obsidian or an AI IDE and Donut without
discarding either side's work or losing learning history. Other-note and
same-note concurrent content edits can already be pulled and then published,
with ordinary Git pausing on real conflicts. Next among notebook-sync stories
is moving a represented folder without losing descendant identities. Web-edit
commit batching stays later: pull already rebases over several accepted
content commits. This advances the direction in
[Proposed ADR 0002](../docs/adrs/0002-git-native-portable-notebook-synchronization.md);
the selected queue does not cover its complete synchronization contract.
Worktree backend-test isolation still leads: 1a–1b delivered an automatic
opt-in workflow, so the remaining gap is ordinary test commands sharing
`doughnut_test`. Leftover checkout-lock exclusivity from 1b is
[quick/058](quick/058-exclusive-worktree-checkout-lock/PLAN.md), not a new
story.

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
