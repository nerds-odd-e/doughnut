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
  "the notebook {string}'s Git binding reflects its current content",
  (notebookName: string) =>
    cli.notebookClone().resnapshotGitBinding(notebookName)
)

When(
  'I clone the notebook {string} into a temporary destination using the installed CLI',
  (notebookName: string) => cli.notebookClone().cloneNotebookInto(notebookName)
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
