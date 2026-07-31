/**
 * `/sync` pulling a notebook down into a workspace. Keep steps as one-line glue
 * to `e2e_test/start/pageObjects/cli`. Behavior and assertions belong there.
 */
import {
  After,
  Before,
  Then,
  When,
} from '@badeball/cypress-cucumber-preprocessor'
import { cli } from '../start/pageObjects/cli'

const { syncPull } = cli

Before({ tags: '@perfSync' }, function () {
  this.previousDefaultCommandTimeout = Cypress.config('defaultCommandTimeout')
  Cypress.config('defaultCommandTimeout', 180_000)
})

After({ tags: '@perfSync' }, function () {
  if (this.previousDefaultCommandTimeout !== undefined) {
    Cypress.config('defaultCommandTimeout', this.previousDefaultCommandTimeout)
  }
})

When('I pull into the workspace {string}', (workspaceName: string) => {
  syncPull.pullIntoWorkspace(workspaceName)
})

Then(
  'pulling into the workspace {string} should complete within {int} seconds',
  (workspaceName: string, seconds: number) => {
    syncPull.pullIntoWorkspaceWithinSeconds(workspaceName, seconds)
  }
)
