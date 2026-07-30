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

function requireDestinationDir(name: string): string {
  const dir = destinationDirsByName.get(name)
  if (!dir) {
    throw new Error(
      `No export destination named "${name}". Add: Given an empty export destination "${name}"`
    )
  }
  return dir
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

/** Put a file in the destination before an export, to prove export leaves it alone. */
export function addExtraDestinationFile(
  name: string,
  relativePath: string,
  content: string
) {
  return cy.task('writeCliWorkspaceFile', {
    workspace: requireDestinationDir(name),
    relativePath,
    content,
  })
}

/**
 * Read the destination until it says what the scenario expects.
 *
 * Typing `/export` only starts the export, and the reads below go through
 * `cy.task`, which Cypress does not retry: a single read can land while the
 * export is still writing, which on a cold machine it does. Re-reading is what
 * makes the assertion about where the destination ends up rather than about how
 * quickly it got there. The assertion is left to run afterwards so a genuine
 * mismatch still reports what was found.
 */
function readUntil<T>(
  read: () => Cypress.Chainable<T>,
  matches: (found: T) => boolean,
  attemptsLeft = 50
): Cypress.Chainable<T> {
  return read().then((found) =>
    matches(found) || attemptsLeft === 0
      ? cy.wrap(found, { log: false })
      : cy
          .wait(100, { log: false })
          .then(() => readUntil(read, matches, attemptsLeft - 1))
  )
}

export function destinationFileShouldHold(
  name: string,
  relativePath: string,
  expectedBody: string
) {
  return readUntil(
    () =>
      cy.task<string>('readCliWorkspaceFile', {
        workspace: resolveDestinationDir(name),
        relativePath,
      }),
    (found) => found.includes(expectedBody)
  ).should('contain', expectedBody)
}

export function destinationShouldHoldOnly(
  name: string,
  relativePaths: readonly string[]
) {
  const expected = [...relativePaths].sort()
  return readUntil(
    () =>
      cy.task<string[]>('listCliWorkspaceFiles', resolveDestinationDir(name)),
    (found) => Cypress._.isEqual(found, expected)
  ).should('deep.equal', expected)
}
