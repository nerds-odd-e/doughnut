/**
 * `/lint` scenarios: keep steps as one-line glue to
 * `e2e_test/start/pageObjects/cli`. Behavior and assertions belong there.
 */
import { Given } from '@badeball/cypress-cucumber-preprocessor'
import { cli } from '../start/pageObjects/cli'

const { syncWorkspace } = cli

Given('an empty workspace {string}', (workspaceName: string) => {
  syncWorkspace.emptyWorkspace(workspaceName)
})

Given(
  'the workspace {string} has a file {string} with content:',
  (workspaceName: string, relativePath: string, content: string) => {
    syncWorkspace.editWorkspaceFile(workspaceName, relativePath, content)
  }
)
