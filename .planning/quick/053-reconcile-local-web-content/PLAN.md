# Keep accumulated local and web content edits

Source: [SEED-009 Story 8](../../seeds/SEED-009-git-backed-local-notebook-workflow.md#story-8).
Status: complete. All 11 slices done.

## Goal and scope

Keep one unpublished commit editing one existing note when accepted history
has advanced through content edits to other notes. `donut notebook pull
<directory>` rebases locally; explicit `donut notebook publish <directory>`
publishes. Pull produces a clean, inspectable result with both sides' content;
publish retains the notes' identities and learning data.

Exclude same-path edits, structural/README/mode changes on either divergent
side, multiple unpublished commits, conflict-resolution, drift repair, and new
transport or UI. Enduring behavior lives in CLI/E2E tests; design in pull
receive/rebase and existing publication.

## Delivered proof

- Local-shape, overlap, and structural guards: `cli/tests/notebookPull.*.suite.ts`
- Eligible rebase and already-based pull: `notebookPull.rebase.suite.ts`,
  `notebookPull.alreadyBased.suite.ts`
- Concurrent checkout protection: `notebookPull.concurrentChange.suite.ts`
- Publication identity: `NotebookGitLocalContentOverWebEditPublicationControllerTest`
- Rejected publish retains L′: `notebookPublish.rebasedRejection.suite.ts`
- Installed journey: `e2e_test/features/cli/cli_notebook_clone.feature`
- Clone/pull guidance: `cli/src/nonInteractiveCli.ts` with
  `notebookClone.test.ts` and `notebookPull.readiness.suite.ts`

Do not mark Story 9 or broader Git synchronization delivered.
