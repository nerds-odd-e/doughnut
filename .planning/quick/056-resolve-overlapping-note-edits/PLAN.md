# Resolve overlapping note edits with ordinary Git

Status: complete (2026-09-07).
Source: [SEED-009 Story 9](../../seeds/SEED-009-git-backed-local-notebook-workflow.md#story-9).

## Goal and scope

Let the notebook owner reconcile one unpublished existing-note content commit
with accepted content edits to the same note. Ordinary Git auto-merges when it
can and pauses a normal rebase on actual conflicts. The owner continues or
aborts with native Git; only a later explicit publish can change Donut.
Accepted history and note identities remain intact.

Write A for L's sole parent, L for the original unpublished local commit, B
for downloaded accepted main, and L′ for a nonempty resolved child of B.
Accepted A..B contains only ordinary-note content changes at unchanged paths;
the local change touches one ordinary note. Include body/frontmatter, root and
nested paths, multiple accepted commits, disjoint edits, edit-then-restore,
and the already-present/empty-result boundary. Keep structural checks on every
accepted edge, not just endpoint trees.

Exclude divergent structural changes and their reversals, multiple unpublished
commits/local edited notes, identity inference, drift repair, automatic
publish/retry/stash/reset, new sync or continue/abort commands, transport,
web merge UI, batching, or Portable metadata.

## Delivered outcomes

- Pull and publish refuse an unfinished Git operation before download or
  submission and leave the operation and files unchanged.
- Eligible same-note content overlap rebases with ordinary Git; disjoint and
  edit-then-restore cases merge; every-edge structural exclusions remain.
- When Git absorbs the local patch, HEAD stays at B and the CLI reports that
  no unpublished change remains.
- A real text conflict pauses with named-path native continue/abort guidance.
- Native `git rebase --continue` / `--skip` / `--abort` work after CLI return
  and download cleanup. Publication of a valid L′ updates the same learned
  note; rejection retains the resolved checkout.
- The installed clone/pull/conflict/continue/publish journey changes Donut
  only at explicit publish.
- Clone success and pull usage share next-steps copy: content-only eligibility,
  auto-merge or native conflict recovery, publish only if unpublished work
  remains, and the accepted-history warning. The rename sequence is unchanged.

## Proof

- `CURSOR_DEV=true nix develop -c pnpm -C cli exec vitest run tests/notebookPull.test.ts tests/notebookPublish.test.ts tests/notebookClone.test.ts`
- `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/cli/cli_notebook_clone.feature`
- `CURSOR_DEV=true nix develop -c pnpm backend:test_only`

Accepted ADR 0004 governs authored Portable content. Accepted ADR 0006 permits
actionable business errors. Enduring behavior lives in those tests and CLI
copy; the home story keeps goal and scope.
