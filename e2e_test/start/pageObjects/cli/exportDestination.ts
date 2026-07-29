import { interactiveCli } from './interactiveCli'

/**
 * Local directories for `/export` to write into.
 *
 * Scenarios name a destination the way a user would (`./ExportTarget`); each
 * name is backed by a real temporary directory, so the command writes actual
 * files without scenarios having to know where they are. A name with no
 * directory behind it is passed through, which is how a scenario asks about a
 * destination that is supposed to be missing.
 */
const destinationDirsByName = new Map<string, string>()

export function resolveDestinationDir(name: string): string {
  return destinationDirsByName.get(name) ?? name
}

export function emptyDestination(name: string) {
  return cy.task<string>('createCliEmptyDirectory').then((dir) => {
    destinationDirsByName.set(name, dir)
  })
}

export function exportNotebook(name: string) {
  return interactiveCli().enterSlashCommandInInteractiveCli(
    `/export ${resolveDestinationDir(name)}`
  )
}

export function destinationFileShouldHold(
  name: string,
  relativePath: string,
  expectedBody: string
) {
  return cy
    .task<string>('readCliWorkspaceFile', {
      workspace: resolveDestinationDir(name),
      relativePath,
    })
    .should('contain', expectedBody)
}

export function destinationShouldHoldOnly(
  name: string,
  relativePaths: readonly string[]
) {
  return cy
    .task<string[]>('listCliWorkspaceFiles', resolveDestinationDir(name))
    .should('deep.equal', [...relativePaths].sort())
}
