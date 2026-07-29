import { e2eAppBaseUrl } from '../../../support/e2eAppUrl'
import testability from '../../testability'
import { cliAssertTask } from './cliAssertTask'
import { interactiveCli } from './interactiveCli'

/**
 * Local Markdown workspaces for a command to read: `/sync --dry-run` compares
 * against one, `/lint` checks one.
 *
 * Scenarios name a workspace the way a user would (`./BenNotebook`); each name
 * is backed by a real temporary directory, so the command runs against actual
 * files without scenarios having to know where they are. A name with no
 * directory behind it is passed through, which is how a scenario asks about a
 * workspace that is supposed to be missing.
 */
const workspaceDirsByName = new Map<string, string>()

export function resolveWorkspaceDir(name: string): string {
  return workspaceDirsByName.get(name) ?? name
}

/**
 * Build `name` as an empty directory, for a scenario that spells out every file
 * it needs rather than starting from what a notebook exports.
 */
export function emptyWorkspace(name: string) {
  return cy.task<string>('createCliEmptyDirectory').then((dir) => {
    workspaceDirsByName.set(name, dir)
  })
}

/**
 * Swap workspace names in a slash command for the directories behind them, so
 * a scenario can write the command the way a user types it and still run
 * against the temporary directory the export was unzipped into.
 *
 * Only whole arguments are matched; an unregistered one is left as it is.
 */
export function resolveWorkspaceNames(command: string): string {
  return command.split(' ').map(resolveWorkspaceDir).join(' ')
}

/**
 * Build `name` as a directory holding exactly what the notebook exports today.
 *
 * Taking it from the export itself is what makes "the same content" true: it
 * needs no second implementation of how a note title becomes a filename, which
 * would drift from the one the export uses.
 */
export function workspaceMatchingNotebook(notebookName: string, name: string) {
  return testability()
    .getNotebookIdByName(notebookName)
    .then((notebookId) =>
      cy.request({
        method: 'GET',
        url: `${e2eAppBaseUrl()}/api/notebooks/${notebookId}/export`,
        encoding: 'base64',
        timeout: 120_000,
      })
    )
    .then((response) =>
      cy.task<string>('createCliWorkspaceFromZip', {
        zipBase64: response.body as string,
      })
    )
    .then((dir) => {
      workspaceDirsByName.set(name, dir)
    })
}

/** Replace a workspace file outright, or create one that was not there. */
export function editWorkspaceFile(
  workspaceName: string,
  relativePath: string,
  content: string
) {
  return cy.task('writeCliWorkspaceFile', {
    workspace: resolveWorkspaceDir(workspaceName),
    relativePath,
    content,
  })
}

/**
 * Retype a note's body in the workspace, leaving the frontmatter and title
 * heading the export writes in place, so the diff is about the content the
 * user changed rather than about the whole file being replaced.
 */
export function editWorkspaceNoteBody(
  workspaceName: string,
  relativePath: string,
  content: string
) {
  return cy.task('writeCliWorkspaceNoteBody', {
    workspace: resolveWorkspaceDir(workspaceName),
    relativePath,
    content,
  })
}

export function pullIntoWorkspace(workspaceName: string) {
  return interactiveCli().enterSlashCommandInInteractiveCli(
    `/sync ${resolveWorkspaceDir(workspaceName)}`
  )
}

export function pullIntoWorkspaceWithinSeconds(
  workspaceName: string,
  seconds: number
) {
  const startedAt = Date.now()
  const dir = resolveWorkspaceDir(workspaceName)
  return interactiveCli()
    .enterSlashCommandInInteractiveCli(`/sync ${dir}`)
    .then(() =>
      cliAssertTask({
        strict: false,
        needle: 'note updated.',
        surface: 'strippedTranscript',
        messagePrefix: 'Past CLI assistant messages (pull timing).',
        timeoutMs: 120_000,
      })
    )
    .then(() => {
      const elapsedMs = Date.now() - startedAt
      expect(
        elapsedMs,
        `sync should finish within ${seconds}s but took ${elapsedMs}ms`
      ).to.be.lessThan(seconds * 1000)
    })
}

export function addExtraWorkspaceFile(
  workspaceName: string,
  relativePath: string,
  content: string
) {
  return editWorkspaceFile(workspaceName, relativePath, content)
}

export function removeWorkspaceFile(
  workspaceName: string,
  relativePath: string
) {
  return cy.task('deleteCliWorkspaceFile', {
    workspace: resolveWorkspaceDir(workspaceName),
    relativePath,
  })
}

export function workspaceFileShouldHold(
  workspaceName: string,
  relativePath: string,
  expectedBody: string
) {
  return cy
    .task<string>('readCliWorkspaceFile', {
      workspace: resolveWorkspaceDir(workspaceName),
      relativePath,
    })
    .should('contain', expectedBody)
}

export function workspaceShouldNotContain(
  workspaceName: string,
  relativePath: string
) {
  return cy
    .task<string[]>('listCliWorkspaceFiles', resolveWorkspaceDir(workspaceName))
    .should('not.include', relativePath)
}

export function workspaceShouldHoldOnly(
  workspaceName: string,
  relativePaths: readonly string[]
) {
  return cy
    .task<string[]>('listCliWorkspaceFiles', resolveWorkspaceDir(workspaceName))
    .should('deep.equal', [...relativePaths].sort())
}
