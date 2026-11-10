import { Given, When, Then } from '@badeball/cypress-cucumber-preprocessor'
import { cli } from '../start/pageObjects/cli'

Given('notebook {string} has a large accepted Markdown tree', (name: string) =>
  cli.notebookLargePublication.seed(name)
)
Then(
  'notebook {string} is healthy with an accepted listing above one MiB',
  (name: string) =>
    cli.notebookLargePublication.expectHealthyLargeAcceptedTree(name)
)
When('I commit three small edits in the large notebook checkout', () =>
  cli.notebookLargePublication.commitThreeEdits()
)
Then(
  'the fresh clone retains the large notebook {string} history and three edits',
  (name: string) => cli.notebookLargePublication.expectFreshReceiver(name)
)
