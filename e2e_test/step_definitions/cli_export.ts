/**
 * `/export` scenarios: keep steps as one-line glue to
 * `e2e_test/start/pageObjects/cli`. Behavior and assertions belong there.
 */
import { Given, Then, When } from '@badeball/cypress-cucumber-preprocessor'
import type { DataTable } from '@badeball/cypress-cucumber-preprocessor'
import { cli } from '../start/pageObjects/cli'

const { exportDestination } = cli

Given('an empty export destination {string}', (name: string) => {
  exportDestination.emptyDestination(name)
})

When('I export the notebook into {string}', (name: string) => {
  exportDestination.exportNotebook(name)
})

Then(
  'the export destination {string} should hold only:',
  (name: string, data: DataTable) => {
    exportDestination.destinationShouldHoldOnly(
      name,
      data.hashes().map((row) => row.Path!)
    )
  }
)

Then(
  'the file {string} in the export destination {string} should hold {string}',
  (relativePath: string, name: string, expected: string) => {
    exportDestination.destinationFileShouldHold(name, relativePath, expected)
  }
)
