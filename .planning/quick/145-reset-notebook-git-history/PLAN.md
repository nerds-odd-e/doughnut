# Reset a notebook's Git history

Status: in progress
Source: [SEED-030 story 2](../../seeds/SEED-030-folder-ancestry-single-representation.md#2-notebooks-that-gained-repaired-content-can-publish-again).
Authority: 2026-09-18 owner instruction to refine the story, write and refine a
slice plan, and commit it. Planning only. The owner executes from another
thread; this plan does not authorize execution by itself.

Baseline: `7cb6e4da90` on `main`.

## Execution identity

- Originating and integration checkout: `/Users/terryyin/git/doughnut`, integration branch `main`.
  Queue claim `e86f76566a` recorded there; it remains in `main`'s history.
- Execution checkout: `/Users/terryyin/git/doughnut/.worktrees/145-reset-notebook-git-history`,
  branch `145-reset-notebook-git-history`, created from `e86f76566a`.
- Mode: Story Branch. Authorized push destination: `origin` `145-reset-notebook-git-history`.
- CI observation: GitHub Actions, workflow `ci.yml`, display name `donut CI`,
  observer directory `/tmp/dough-ci-501/watch-6PqtuY`.

## Goal and scope

Someone who can edit a notebook resets its Git history from the notebook
settings. The accepted history is replaced by one initial commit of the entire
current notebook, so the notebook can be cloned and published again whatever
state its history was in. This is the recovery path for the projection drift
that `V300000333__notebook_follows_folder_containment.sql` leaves in the
notebooks that gain rows. By owner decision it does not block the release
carrying that migration; those notebooks refuse publication until they are reset.

Included: one reset operation, its endpoint, a settings button with a warning.
Reset is allowed in any state; do not special-case "only when drifted".

Excluded, by owner direction: reconciling an existing clone's unpublished
commits with the new root; refusing to clone a drifted notebook; an admin
reset of another user's notebook; any CLI change; any edit to ADR 0002,
NORTH-STAR or the backlog direction text. The owner knowingly accepts that
this rewinds accepted `main`, which the Proposed ADR 0002 says v1 rejects.
Do not add null guards or drift detection.

Assumptions:

- "Can edit the notebook" is the existing `authorizationService.assertAuthorization(notebook)`
  rule every notebook settings action uses. Any member of an owning circle
  passes it. The owner confirmed any circle member may reset.
- The reset commit is authored by the existing system author constants.

## Existing solutions and selected design

| Responsibility | Existing owner / evidence | Choice |
| --- | --- | --- |
| Replace a binding with one fresh parentless snapshot of current content | `NotebookGitCutoverService.resnapshotForTestability`, called only by `NotebookGitTestabilityController` | Promote it to the product operation under a capability name. The testability endpoint calls the same method. Do not write a second rebuild. |
| Create the first binding for a new notebook | `NotebookGitCutoverService.createBindingForNotebook` | Leave as is unless the refactor pass finds the two collapse without growth. |
| Writer lock on a binding | `NotebookGitBindingRepository.findByNotebookIdForUpdate`, used by download and publish | The product reset reads the binding through this lock inside one transaction. `resnapshotForTestability` reads it unlocked today. |
| Settings action with confirmation | "Reset index" in `NotebookSettings.vue`: `popups.confirm`, `apiCallWithLoading`, toast | Same pattern. No new component. |
| A pre-reset clone meets the new history | `LOCAL_UNRELATED` in `cli/src/commands/notebook/notebookLocalCandidate.ts`, covered by `cli/tests/notebookPull.localCandidate.suite.ts` | Reuse. No CLI change, no new test. |
| Drifted fixture at the controller boundary | `NotebookGitProjectionDriftControllerTest` and `NotebookGitBundleControllerTestBase`: rows built with `makeMe` after the binding exists are not in accepted history | Reuse the idiom. |

Expected production shape: one controller method beside `resetNotebookIndex`,
one renamed service method that now takes the lock, one button. The testability
specialization of the snapshot replacement disappears, so the service should
not grow by more than the lock.

### Accepted decisions carried

- ADR 0006: no catch around the reset. A failure rolls the transaction back and
  surfaces.
- ADR 0002 is **Proposed**, not Accepted. Its append-only mainline section
  conflicts with this story; the owner chose the exception. Surface nothing
  further and do not edit it.

## Outside-in proof

1. **Reset recovers a drifted notebook** at `NotebookController`: a git-backed
   notebook gains a note outside accepted history; publishing a plain edit
   answers 409 projection drift. After the reset, the downloaded bundle has one
   parentless commit whose tree equals the current notebook, including that
   note, and a plain edit built on the new head publishes.
2. **A user who cannot edit the notebook is refused** with
   `UnexpectedNoAccessRightException`, and the accepted head and bundle are
   unchanged. A member of the owning circle succeeds.
3. **The owner resets from notebook settings**: E2E, settings button, warning,
   confirm, then `donut notebook clone` yields the whole notebook in one commit.
   A mounted-component test covers cancelling the warning: no request is sent.

## Ordered slices

### Slice 1 — Resetting replaces accepted history with one commit of the current notebook

Type: Behavior
Status: done

Behavior: a git-backed notebook whose live content differs from its accepted
history → someone who can edit it calls the reset endpoint → the accepted
history is one parentless commit equal to the current notebook, and a plain
edit on that head publishes. Rejection constraint in the same trigger: a user
who cannot edit the notebook is refused and the binding is unchanged; a circle
member is accepted.

Add `POST /api/notebooks/{notebook}/reset-git-history` beside `reset-index`,
`assertAuthorization(notebook)` first. Rename
`resnapshotForTestability` to a capability name such as `resetHistory`, make it
transactional and read the binding with `findByNotebookIdForUpdate`. Point
`NotebookGitTestabilityController` at the renamed method; keep that testability
endpoint because E2E fixtures depend on it. Use a commit message that says the
history was reset, distinct from the cutover message.

Proof: one new controller test class extending
`NotebookGitBundleControllerTestBase`, proof examples 1 and 2.
Accepted proof, 2026-09-18: `NotebookGitHistoryResetControllerTest`.
`resetRestartsHistoryFromTheCurrentNotebookSoAPlainEditPublishesAgain` builds a
git-backed notebook, snapshots it, then adds a note after the snapshot, so a
plain edit is refused 409 with reason containing "refresh the checkout before
publishing"; after `controller.resetNotebookGitHistory(notebook)` the reloaded
binding has a new accepted id, `getParentCount() == 0`, a walked history of
exactly one commit, `readTreeEntries` equal to `note.md` plus `outside.md`, and a
plain edit on the new head publishes so the note content becomes the edited one.
`deniedResetLeavesAcceptedHistoryUnchanged` refuses a non-owner with
`UnexpectedNoAccessRightException` and leaves the accepted id and bundle bytes
equal to their pre-call values. Commands:
`unset SPRING_DATASOURCE_URL DB_URL SPRING_FLYWAY_URL && CURSOR_DEV=true nix develop -c pnpm backend:test:worktree --tests 'com.odde.donut.controllers.NotebookGit*'`
(99 classes, 320 tests, 0 failures) and the same wrapper with
`--tests '*NotebookGitCutoverServiceTest*'`.
 Assert the
commit count and parentlessness through `GitBundleTestReader`, and the tree
through `GitBundleTestReader.readTreeEntries` or the existing blob readers.
Command:
`unset SPRING_DATASOURCE_URL DB_URL SPRING_FLYWAY_URL && CURSOR_DEV=true nix develop -c pnpm backend:test:worktree --tests '*NotebookGitHistoryReset*'`
then the feature suite, which must stay green because the testability fixture
path changed:
`unset SPRING_DATASOURCE_URL DB_URL SPRING_FLYWAY_URL && CURSOR_DEV=true nix develop -c pnpm backend:test:worktree --tests 'com.odde.donut.controllers.NotebookGit*'`

Wrap-up for this slice regenerates the TypeScript API client, because a
controller signature was added (`generate-api-client` skill).

Safe stopping point: yes. The endpoint works without the button.

### Slice 2 — The owner resets Git history from notebook settings behind a warning

Type: Behavior
Status: planned

Behavior: the owner opens notebook settings → presses "Reset Git history" and
confirms a warning that the history is permanently discarded and existing
clones must be cloned again → a fresh `donut notebook clone` contains the whole
notebook in one commit. Cancelling sends nothing.

Add the button and handler to `NotebookSettings.vue` following "reset index",
with a success toast.

Proof, E2E first and kept explicitly unfinished until green: one scenario in
`e2e_test/features/cli/`, next to `cli_notebook_clone.feature`, whose notebook
holds injected notes that accepted history lacks. Reuse the existing clone
steps and assertions; add one settings step. Do **not** use the step "the
notebook ...'s Git binding reflects its current content" in this scenario,
since the reset is what makes the notes clonable. Then one mounted-component test in
`frontend/tests/components/notebook/` for the cancel path. Follow the
`e2e-authoring`, `frontend` and `unit-testing` skills for commands and tags.

Safe stopping point: yes. Story complete.

## Refinement assessment

Two slices, each one postcondition and one proof loop. Authorization stays in
slice 1 as the rejection constraint of the same trigger, because splitting it
out would leave a tests-only slice. Slice 2 has two beats, E2E and component
test, around one button; if the E2E setup overruns, stop and refine rather than
dropping the scenario.

Remaining concerns:

- Slice 1: the writer lock has no dedicated concurrency proof. Download and
  publish have none at this boundary either beyond
  `NotebookGitConcurrent*ControllerTest`. Consequence: the lock is design by
  reuse, not proven behavior.

Resolved during refinement: notes injected by E2E fixtures never reach accepted
history. `InjectNotesWorker` has no Git handling, and existing scenarios sync
with the step "the notebook ...'s Git binding reflects its current content".
Omitting that step gives slice 2 a genuinely drifted notebook. The assertions
"the cloned checkout is a clean single-commit checkout on branch "main"" and
"the cloned checkout contains exactly:" already exist as steps.

## Current decisions

- One reset operation serves the product endpoint and the testability fixture.
- Circle-member acceptance is proved by the existing
  `AuthorizationServiceTest.noteBelongsToACircle.memberCanAccess`, which covers the
  same `assertAuthorization` rule the endpoint calls. The new controller test
  therefore proves drift recovery and the refusal of a user who cannot edit,
  without a bespoke circle fixture. A circle-owned notebook at this controller
  boundary would need manual `circle_user` and ownership teardown, because
  `CommittedUserCleanup` only reaches notebooks through `ownership → user`; that
  fixture growth would prove an already-covered rule.
- Reset never inspects whether the notebook is drifted.
- Once the button is released, the owner resets notebooks 4, 26 and 191 by hand. Notebook
  309 is its own owner's to reset. Nothing in this plan touches production data.

## Learnings

- 2026-09-18: the execution worktree and branch disappeared while the first slice
  was being implemented, alongside a concurrent session executing plan 146 in
  `.worktrees/146-permanently-delete-trashed-content`. No implementation work
  existed yet and the claim commit, the **Taken** entry and this plan were all
  intact at `main`, so recovery was to recreate the branch and worktree from
  `e86f76566a`. The detached CI observer survived and was reused rather than
  relaunched.
- Slice 1 delivered the shape the plan predicted: no new service and no new
  class. The service grew by the writer lock and `@Transactional` plus a
  `RESET_COMMIT_MESSAGE` and a `message` parameter on `buildBundle` — three lines
  beyond "the lock" — which is the price of keeping the reset and cutover commit
  messages distinct.
- The refactor pass collapsed three character-identical private
  `reloadCommittedBinding` helpers (one newly introduced by this slice) into one
  on `NotebookGitBundleControllerTestBase`, a net reduction across the change.
- Declined during refactoring, with reasons: `resetHistory` and
  `createBindingForNotebook` do not collapse without growth, since a shared
  writer would leave two wrappers plus a helper and would push the pessimistic
  lock onto the notebook-creation path where a binding provably cannot exist yet;
  the plan's stated default therefore stands. Renaming `NotebookGitCutoverService`
  would touch ten files mechanically without reducing complexity.
- Naming debt found, not this story's to fix: `CUTOVER_COMMIT_MESSAGE`
  ("Cutover: snapshot existing notebook content into Git") is now inaccurate at
  its only remaining call site, notebook creation, where there is no existing
  content. No test asserts either commit message. "Cutover" in the class name is
  likewise vestigial.
