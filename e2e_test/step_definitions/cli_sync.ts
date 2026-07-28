/**
 * `/sync --dry-run` scenarios: keep steps as one-line glue to
 * `e2e_test/start/pageObjects/cli`. Behavior and assertions belong there.
 */
import { Given, Then, When } from '@badeball/cypress-cucumber-preprocessor'
import type { DataTable } from '@badeball/cypress-cucumber-preprocessor'
import { cli } from '../start/pageObjects/cli'
import start from '../start'

const { syncWorkspace } = cli

Given(
  'the workspace {string} holds the same content as {string}',
  (workspaceName: string, notebookName: string) => {
    syncWorkspace.workspaceMatchingNotebook(notebookName, workspaceName)
  }
)

Given(
  'I have a notebook {string} with note {string} holding:',
  (notebookName: string, noteTitle: string, content: string) => {
    cy.get<string>('@currentLoginUser').then((username) =>
      start
        .testability()
        .injectNotes([{ Title: noteTitle }], username, notebookName)
    )
    start.testability().setInjectedNoteContent(noteTitle, content)
  }
)

When(
  'the note {string} is changed in Doughnut to {string}',
  (noteTitle: string, content: string) => {
    start.testability().setInjectedNoteContent(noteTitle, content)
  }
)

When(
  'the note {string} is changed in Doughnut to:',
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

When(
  'I run sync without --dry-run on the workspace {string}',
  (workspaceName: string) => {
    syncWorkspace.runSyncWithoutDryRun(workspaceName)
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
  'the workspace {string} should hold only:',
  (workspaceName: string, data: DataTable) => {
    syncWorkspace.workspaceShouldHoldOnly(
      workspaceName,
      data.hashes().map((row) => row.Path!)
    )
  }
)
