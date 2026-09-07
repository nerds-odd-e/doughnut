/**
 * CLI notebook clone scenario: keep steps as one-line glue to
 * `e2e_test/start/pageObjects/cli`. Behavior and assertions belong in page objects, not here.
 */
import {
  Given,
  Then,
  When,
  type DataTable,
} from '@badeball/cypress-cucumber-preprocessor'
import { cli } from '../start/pageObjects/cli'

Given(
  'the installed CLI uses the access token of {string}',
  (userIdentifier: string) =>
    cli.notebookClone().useAccessTokenOf(userIdentifier)
)

Given(
  "the notebook {string}'s Git binding reflects its current content",
  (notebookName: string) =>
    cli.notebookClone().resnapshotGitBinding(notebookName)
)

When(
  'I clone the notebook {string} into a temporary destination using the installed CLI',
  (notebookName: string) => cli.notebookClone().cloneNotebookInto(notebookName)
)

When(
  'I clone the notebook {string} into a second temporary destination using the installed CLI',
  (notebookName: string) =>
    cli.notebookClone().cloneNotebookIntoReceiver(notebookName)
)

When(
  'I clone the notebook {string} expecting rejection from the installed CLI',
  (notebookName: string) =>
    cli.notebookClone().cloneNotebookExpectingRejection(notebookName)
)

Then('the clone destination does not exist', () =>
  cli.notebookClone().expectDestinationAbsent()
)

When(
  'I commit the following edit to {string} in the cloned checkout:',
  (relativePath: string, content: string) =>
    cli.notebookCloneCheckout().commitEdit(relativePath, content)
)

When(
  'I add and commit the following note at {string} in the cloned checkout:',
  (relativePath: string, content: string) =>
    cli.notebookCloneCheckout().commitAddition(relativePath, content)
)

When('I publish the cloned checkout using the installed CLI', () =>
  cli.notebookCloneCheckout().publish()
)

When(
  'I commit the following related additions and edit together in the cloned checkout:',
  (data: DataTable) =>
    cli
      .notebookCloneCheckout()
      .commitRelatedNoteChanges(
        data
          .hashes()
          .map(({ path, content }) => ({ relativePath: path, content }))
      )
)

When(
  'I commit a removal of {string} in the cloned checkout',
  (relativePath: string) =>
    cli.notebookCloneCheckout().commitRemoval(relativePath)
)

When(
  'I commit a rename of {string} to {string} in the cloned checkout',
  (fromRelativePath: string, toRelativePath: string) =>
    cli.notebookCloneCheckout().commitRename(fromRelativePath, toRelativePath)
)

When(
  'I publish the cloned checkout expecting rejection from the installed CLI',
  () => cli.notebookCloneCheckout().publishExpectingRejection()
)

Then('the cloned checkout retains the original committed proposal', () =>
  cli.notebookCloneCheckout().expectProposalRetained()
)

Then(
  'the installed CLI reports the committed change as the accepted head',
  () => cli.notebookCloneCheckout().expectCommittedHeadAccepted()
)

Then(
  'the cloned checkout is a clean single-commit checkout on branch {string}',
  (branch: string) =>
    cli.notebookCloneCheckout().expectCleanSingleCommitCheckoutOnBranch(branch)
)

Then('the cloned checkout contains exactly:', (data: DataTable) =>
  cli
    .notebookCloneCheckout()
    .expectCanonicalTreeFor(data.raw().map((row) => row[0] as string))
)

Then('the second cloned checkout contains exactly:', (data: DataTable) =>
  cli
    .notebookCloneCheckout()
    .expectReceiverCanonicalTreeFor(data.raw().map((row) => row[0] as string))
)

When('I pull the cloned checkout using the installed CLI', () =>
  cli.notebookCloneCheckout().pull()
)

When('I pull the second cloned checkout using the installed CLI', () =>
  cli.notebookCloneCheckout().pullReceiver()
)

Then(
  'the second cloned checkout retains its original head as an ancestor',
  () => cli.notebookCloneCheckout().expectReceiverOriginalHeadIsAncestor()
)

When(
  'I pull the cloned checkout expecting rejection from the installed CLI',
  () => cli.notebookCloneCheckout().pullExpectingRejection()
)

Then(
  'the cloned checkout has a paused rebase conflict for {string}',
  (relativePath: string) =>
    cli.notebookCloneCheckout().expectPausedRebaseConflictFor(relativePath)
)

When(
  'I write, stage, and continue the cloned checkout rebase with the following edit to {string}:',
  (relativePath: string, content: string) =>
    cli.notebookCloneCheckout().continueWithChosenEdit(relativePath, content)
)

Then('the cloned checkout is a clean rebased child of the accepted head', () =>
  cli.notebookCloneCheckout().expectCleanRebasedChildOfAcceptedHead()
)

Then(
  'the cloned checkout retains the original local commit for {string}',
  (relativePath: string) =>
    cli
      .notebookCloneCheckout()
      .expectOriginalLocalCommitRetainedFor(relativePath)
)

Then(
  '{string} in the cloned checkout matches the accepted parent',
  (relativePath: string) =>
    cli
      .notebookCloneCheckout()
      .expectCheckoutFileMatchesAcceptedParent(relativePath)
)

Then(
  'the cloned checkout file {string} is:',
  (relativePath: string, content: string) =>
    cli.notebookCloneCheckout().expectCheckoutFile(relativePath, content)
)

Then(
  'the installed CLI reports the rebased local head as the accepted head',
  () => cli.notebookCloneCheckout().expectRebasedHeadAccepted()
)

Then('the cloned checkout is a clean resolved child of the accepted head', () =>
  cli.notebookCloneCheckout().expectCleanResolvedChildOfAcceptedHead()
)

Then(
  'the cloned checkout retains the original local commit author and message for {string}',
  (relativePath: string) =>
    cli.notebookCloneCheckout().expectResolvedLocalCommitFor(relativePath)
)

Then(
  'the installed CLI reports the resolved local head as the accepted head',
  () => cli.notebookCloneCheckout().expectResolvedHeadAccepted()
)
