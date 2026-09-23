/**
 * CLI notebook clone scenario: keep steps as one-line glue to
 * `e2e_test/start/pageObjects/cli`. Behavior and assertions belong in page objects, not here.
 * Checkout commit steps live in `cli_notebook_clone_commits.ts`.
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
  'I clone the notebook {string} into a fresh temporary destination using the installed CLI',
  (notebookName: string) =>
    cli.notebookClone().cloneNotebookIntoFresh(notebookName)
)

When(
  'I clone the notebook {string} expecting rejection from the installed CLI',
  (notebookName: string) =>
    cli.notebookClone().cloneNotebookExpectingRejection(notebookName)
)

Then('the clone destination does not exist', () =>
  cli.notebookClone().expectDestinationAbsent()
)

When('I publish the cloned checkout using the installed CLI', () =>
  cli.notebookCloneCheckout().publish()
)

When('I publish the second cloned checkout using the installed CLI', () =>
  cli.notebookCloneCheckout().publishReceiver()
)

When(
  'I publish the cloned checkout expecting rejection from the installed CLI',
  () => cli.notebookCloneCheckout().publishExpectingRejection()
)

Then('the cloned checkout retains the original committed proposal', () =>
  cli.notebookCloneCheckout().expectProposalRetained()
)

Then(
  'the notebook {string} accepted head remains the parent of the cloned checkout',
  (notebookName: string) =>
    cli
      .notebookCloneCheckout()
      .expectAcceptedHeadUnchangedFromCheckoutParent(notebookName)
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

Then(
  'the published checkout has exactly one commit after the second clone original head',
  () =>
    cli
      .notebookCloneCheckout()
      .expectPublishedHeadIsSingleCommitAfterReceiverOriginal()
)

Then(
  'the second cloned checkout is a clean checkout of the accepted head',
  () => cli.notebookCloneCheckout().expectReceiverAtAcceptedHead()
)

Then(
  "the second cloned checkout preserves the publisher's A to C history",
  () => cli.notebookCloneCheckout().expectReceiverPreservesPublisherHistory()
)

Then(
  'the second cloned checkout file {string} is:',
  (relativePath: string, content: string) =>
    cli
      .notebookCloneCheckout()
      .expectReceiverCheckoutFile(relativePath, content)
)

Then(
  'the second cloned checkout is a clean checkout of the notebook {string} accepted head',
  (notebookName: string) =>
    cli
      .notebookCloneCheckout()
      .expectReceiverAtNotebookAcceptedHead(notebookName)
)

Then(
  'the second cloned checkout file {string} holds exactly:',
  (relativePath: string, text: string) =>
    cli
      .notebookCloneCheckout()
      .expectReceiverCheckoutFileExactText(relativePath, text)
)

Then(
  'the second cloned checkout file {string} holds the bytes {string}',
  (relativePath: string, bytes: string) =>
    cli
      .notebookCloneCheckout()
      .expectReceiverCheckoutFileBytes(relativePath, bytes)
)

Then('the fresh clone contains exactly:', (data: DataTable) =>
  cli
    .notebookCloneCheckout()
    .expectFreshCloneCanonicalTreeFor(data.raw().map((row) => row[0] as string))
)

Then('the fresh clone is a clean checkout of the accepted head', () =>
  cli.notebookCloneCheckout().expectFreshCloneAtAcceptedHead()
)

Then(
  'the fresh clone file {string} holds exactly:',
  (relativePath: string, text: string) =>
    cli
      .notebookCloneCheckout()
      .expectFreshCloneFileExactText(relativePath, text)
)

Then(
  'the fresh clone file {string} holds the bytes {string}',
  (relativePath: string, bytes: string) =>
    cli.notebookCloneCheckout().expectFreshCloneFileBytes(relativePath, bytes)
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
