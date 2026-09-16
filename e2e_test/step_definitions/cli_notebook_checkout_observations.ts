/**
 * Observable Git checkout outcomes shared by clone, pull, publish, and rebase
 * journeys. Step definitions remain one-line glue to the CLI page objects.
 */
import { Then, When } from '@badeball/cypress-cucumber-preprocessor'
import { cli } from '../start/pageObjects/cli'

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
  'the cloned checkout file {string} is unchanged from its parent',
  (relativePath: string) =>
    cli
      .notebookCloneCheckout()
      .expectCheckoutFileUnchangedFromParent(relativePath)
)

Then(
  'the cloned checkout file {string} is:',
  (relativePath: string, content: string) =>
    cli.notebookCloneCheckout().expectCheckoutFile(relativePath, content)
)

Then(
  'the cloned checkout is a clean append-only chain from its original head',
  () => cli.notebookCloneCheckout().expectCleanAppendOnlyChainFromOriginalHead()
)

Then(
  'the cloned checkout retains its original head as an ancestor and is clean at the accepted head',
  () =>
    cli
      .notebookCloneCheckout()
      .expectOriginalHeadIsAncestorAndCleanAcceptedHead()
)

When(
  'I record the accepted head of notebook {string}',
  (notebookName: string) =>
    cli.notebookCloneCheckout().recordAcceptedHead(notebookName)
)

Then(
  'the cloned checkout retains the recorded accepted heads as ancestors and is clean at the accepted head',
  () =>
    cli
      .notebookCloneCheckout()
      .expectRecordedAcceptedHeadsAreAncestorsAndCleanAcceptedHead()
)

Then(
  'the cloned checkout file {string} at the accepted parent is:',
  (relativePath: string, content: string) =>
    cli.notebookCloneCheckout().expectCheckoutParentFile(relativePath, content)
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
