/**
 * `/sync` scenarios: keep steps as one-line glue to
 * `e2e_test/start/pageObjects/cli`. Behavior and assertions belong there.
 */
import {
  After,
  Before,
  Given,
  Then,
  When,
} from '@badeball/cypress-cucumber-preprocessor'
import type { DataTable } from '@badeball/cypress-cucumber-preprocessor'
import { cli } from '../start/pageObjects/cli'
import start from '../start'

const { syncWorkspace } = cli

Before({ tags: '@perfSync' }, function () {
  this.previousDefaultCommandTimeout = Cypress.config('defaultCommandTimeout')
  Cypress.config('defaultCommandTimeout', 180_000)
})

After({ tags: '@perfSync' }, function () {
  if (this.previousDefaultCommandTimeout !== undefined) {
    Cypress.config('defaultCommandTimeout', this.previousDefaultCommandTimeout)
  }
})

Given(
  'the workspace {string} holds the same content as {string}',
  (workspaceName: string, notebookName: string) => {
    syncWorkspace.workspaceMatchingNotebook(notebookName, workspaceName)
  }
)

When(
  'the note {string} is changed in Doughnut to {string}',
  (noteTitle: string, content: string) => {
    start.testability().setInjectedNoteContent(noteTitle, content)
  }
)

When(
  'I edit {string} in the workspace {string} to {string}',
  (relativePath: string, workspaceName: string, content: string) => {
    syncWorkspace.editWorkspaceFile(workspaceName, relativePath, content)
  }
)

When(
  'I preview the pull into the workspace {string}',
  (workspaceName: string) => {
    syncWorkspace.previewPull(workspaceName)
  }
)

When('I pull into the workspace {string}', (workspaceName: string) => {
  syncWorkspace.pullIntoWorkspace(workspaceName)
})

Then(
  'pulling into the workspace {string} should complete within {int} seconds',
  (workspaceName: string, seconds: number) => {
    syncWorkspace.pullIntoWorkspaceWithinSeconds(workspaceName, seconds)
  }
)

Given(
  'the workspace {string} has an extra file {string} with content:',
  (workspaceName: string, relativePath: string, content: string) => {
    syncWorkspace.addExtraWorkspaceFile(workspaceName, relativePath, content)
  }
)

Given(
  'the file {string} is removed from the workspace {string}',
  (relativePath: string, workspaceName: string) => {
    syncWorkspace.removeWorkspaceFile(workspaceName, relativePath)
  }
)

Given(
  'I have a notebook {string} with {int} numbered notes',
  (notebookName: string, count: number) => {
    const notes = Array.from({ length: count }, (_, index) => {
      const n = index + 1
      const title = `note-${String(n).padStart(4, '0')}`
      return { Title: title, Content: 'seed' }
    })
    cy.get<string>('@currentLoginUser').then((username) =>
      start.testability().injectNotes(notes, username, notebookName)
    )
  }
)

Then(
  'I should see the preview in past CLI assistant messages:',
  (expected: string) => {
    cli.interactiveCli().pastCliAssistantMessages().expectContains(expected)
  }
)

Then(
  'the file {string} in the workspace {string} should hold {string}',
  (relativePath: string, workspaceName: string, expected: string) => {
    syncWorkspace.workspaceFileShouldHold(workspaceName, relativePath, expected)
  }
)

Then(
  'the workspace {string} should not contain {string}',
  (workspaceName: string, relativePath: string) => {
    syncWorkspace.workspaceShouldNotContain(workspaceName, relativePath)
  }
)

Then(
  'the workspace {string} should hold only:',
  (workspaceName: string, data: DataTable) => {
    syncWorkspace.workspaceShouldHoldOnly(
      workspaceName,
      data.hashes().map((row) => row.Path!)
    )
  }
)
