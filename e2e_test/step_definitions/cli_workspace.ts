/**
 * The local Markdown workspace, as the `/sync`, `/push` and `/lint` scenarios
 * set it up and check it. Keep steps as one-line glue to
 * `e2e_test/start/pageObjects/cli`. Behavior and assertions belong there.
 */
import { Given, Then, When } from '@badeball/cypress-cucumber-preprocessor'
import type { DataTable } from '@badeball/cypress-cucumber-preprocessor'
import { cli } from '../start/pageObjects/cli'

const { workspace } = cli

Given('an empty workspace {string}', (workspaceName: string) => {
  workspace.emptyWorkspace(workspaceName)
})

Given(
  'the workspace {string} holds the same content as {string}',
  (workspaceName: string, notebookName: string) => {
    workspace.workspaceMatchingNotebook(notebookName, workspaceName)
  }
)

Given(
  'the workspace {string} is the notebook {string} exported into {string}',
  (workspaceName: string, notebookName: string, destinationName: string) => {
    workspace.exportedNotebookAsWorkspace(
      notebookName,
      destinationName,
      workspaceName
    )
  }
)

Given(
  'the workspace {string} has a file {string} with content:',
  (workspaceName: string, relativePath: string, content: string) => {
    workspace.writeWorkspaceFile(workspaceName, relativePath, content)
  }
)

/** Extra as in beyond what the notebook has, so a pull has to decide about it. */
Given(
  'the workspace {string} has an extra file {string} with content:',
  (workspaceName: string, relativePath: string, content: string) => {
    workspace.writeWorkspaceFile(workspaceName, relativePath, content)
  }
)

Given(
  'the file {string} is removed from the workspace {string}',
  (relativePath: string, workspaceName: string) => {
    workspace.removeWorkspaceFile(workspaceName, relativePath)
  }
)

When(
  'I edit the content of {string} in the workspace {string} to {string}',
  (relativePath: string, workspaceName: string, content: string) => {
    workspace.editWorkspaceNoteBody(workspaceName, relativePath, content)
  }
)

/**
 * The whole file as the user's editor shows it, frontmatter included, for a
 * scenario about what a property looks like locally rather than about a body
 * edit alone.
 */
When(
  'the file {string} in the workspace {string} is:',
  (relativePath: string, workspaceName: string, content: string) => {
    workspace.writeWorkspaceFile(workspaceName, relativePath, content)
  }
)

Then(
  'the file {string} in the workspace {string} should hold {string}',
  (relativePath: string, workspaceName: string, expected: string) => {
    workspace.workspaceFileShouldHold(workspaceName, relativePath, expected)
  }
)

Then(
  'the workspace {string} should not contain {string}',
  (workspaceName: string, relativePath: string) => {
    workspace.workspaceShouldNotContain(workspaceName, relativePath)
  }
)

Then(
  'the workspace {string} should hold only:',
  (workspaceName: string, data: DataTable) => {
    workspace.workspaceShouldHoldOnly(
      workspaceName,
      data.hashes().map((row) => row.Path!)
    )
  }
)
