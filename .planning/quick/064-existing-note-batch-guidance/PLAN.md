# Describe existing-note batch publication in clone guidance

Source: [SEED-009 Story 14](../../seeds/SEED-009-git-backed-local-notebook-workflow.md#story-14),
execution retrospective of completed quick/063.
Status: complete.

## Goal and scope

After cloning, a notebook owner learns that one direct-child commit can publish
edits to several existing ordinary notes. Correct the obsolete single-edited-note
limit without changing publication, pull, identity, or structural policies.

## Ordered slices

### 1. Explain the supported existing-note batch after cloning

Type: Behavior
Status: done
Proof: `CURSOR_DEV=true nix develop -c pnpm -C cli exec vitest run tests/notebookClone.test.ts`

Behavior: An owner clones successfully → reads next-step output → sees that
one commit directly on accepted main may edit one or more existing ordinary
Markdown notes at unchanged paths, with the current structural and pull limits.
