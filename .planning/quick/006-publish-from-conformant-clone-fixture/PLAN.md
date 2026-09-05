# Publish from a conformant cloned-notebook fixture

Source: manual UAT follow-up for delivered
[SEED-009 Story 2](../../seeds/SEED-009-git-backed-local-notebook-workflow.md#story-2).
Status: complete.

## Outcome and boundaries

An owner exercising the installed-CLI workflow needs the representative cloned
notebook fixture to satisfy the same typed-Markdown invariant as production, so
the acceptance path proves that one committed local edit reaches the same Donut
note instead of failing on unrelated invalid fixture content.

Representative behavior: a Git-backed notebook contains valid typed Markdown,
including an otherwise-empty root note → the owner clones it, edits and commits
one folder-contained note, and publishes through the installed CLI → Donut shows
the committed body on that same note and the CLI reports the accepted head.

The production note-save path and the earlier production type backfill already
establish typed stored Markdown. This plan does not weaken whole-tree validation,
change cutover or publication production code, add a data migration, or extend
publication beyond one direct-child commit modifying one existing note. It
follows Accepted ADR 0004's conformant Portable-tree requirement.

## Outside-in proof

Extend the existing installed-CLI notebook feature. First add the publish path
while retaining the invalid empty `Overview.md` setup and confirm it fails with
the manual-UAT frontmatter rejection. Then make that fixture use valid typed
Markdown and rerun the same feature: the installed CLI clones, ordinary Git
edits and commits the checkout, publication succeeds, and the normal web note
page displays the new content.

## Ordered slices

### 1. Publish a committed edit from the installed clone
Type: Behavior
Status: done
Proof: `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/cli/cli_notebook_clone.feature`

Behavior: a bound checkout cloned from a conformant typed notebook has one
committed edit to an existing folder-contained note → the owner runs the
installed CLI publish command → the CLI reports the accepted head and Donut
shows the new body on that same note.

Add only the checkout edit/commit and publish operations needed by this
scenario, keeping Cucumber glue thin and CLI behavior in the existing page
objects/tasks. Reuse the existing web note-content assertion. Confirm the red
failure is the unchanged empty `Overview.md`, then replace that invalid fixture
precondition with typed Markdown rather than changing production validation.
The focused Cypress runtime may exceed the normal leaf target; implementation
scope remains one proof loop.

## Current decisions

- Accepted ADR 0004 requires the complete Portable notebook tree to remain
  conformant; unchanged invalid files are not grandfathered.
- The manual finding is a test-fixture fidelity and missing installed-CLI
  acceptance-coverage problem, not evidence for another production data
  migration: the type backfill preceded Git cutover, and current note creation
  stores normalized typed Markdown.
- Existing user changes to the Story 2 seed and product backlog are outside this
  plan and must not be staged or committed here.

## Completion record

The installed-CLI feature now proves clone → ordinary Git edit and commit →
publish → same-note web content. The test first reproduced the manual-UAT
failure on the invalid empty `Overview.md`, then passed after the fixture was
made typed and conformant. Production whole-tree validation and cutover code
remain unchanged.
