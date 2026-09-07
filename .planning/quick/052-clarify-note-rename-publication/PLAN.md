# Clarify identity-preserving note rename publication

Status: complete (2026-09-07).
Source: [SEED-009 Story 6](../../seeds/SEED-009-git-backed-local-notebook-workflow.md#story-6).

## Goal and scope

An owner following the existing Git publication workflow can understand how
to publish a same-folder rename and a later edit, what happens to authored
links, and which destination is blocked when a deleted note reserves a title.

The delivered corrections preserve the existing unchanged-content,
same-parent rename boundary, note identity, authored bytes, transaction,
learning state, and local-work protection. They add no commands, link rewrites,
aliases, restore support, cross-folder movement, API schema, or DDL changes.

## Delivered outcomes

- CLI clone guidance now says to commit and publish the unchanged same-folder
  rename, wait for acceptance, then edit and separately commit and publish the
  content change. It explains that authored referring links are not rewritten
  and old-path links may no longer resolve (`cc7b053424`).
- A rename rejected because an accepted deletion reserves its destination now
  names the requested Portable path while preserving the original conflict
  type, fields, cause, and rollback behavior (`6c94b5f802`).

## Proof

- `CURSOR_DEV=true nix develop -c pnpm -C cli exec vitest run tests/notebookClone.test.ts`
- `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/cli/cli_notebook_clone.feature`
- `CURSOR_DEV=true nix develop -c pnpm backend:test_only`

Accepted ADR 0004 governs authored filenames, content, and references. Accepted
ADR 0006 permits contextual business errors while retaining the original cause.
