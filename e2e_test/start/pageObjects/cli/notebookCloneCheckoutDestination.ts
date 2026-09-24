/**
 * Shared operations and observations for a cloned checkout, addressed by its
 * Cypress destination alias (`@cliCloneDestination`,
 * `@cliCloneReceiverDestination`, `@cliCloneFreshDestination`). Each
 * per-destination page object names its own methods in terms of these, so the
 * same checkout knowledge is not restated once per destination.
 */
import type { CliNotebookCheckoutState } from '../../../config/cliE2eNotebookCloneTasks'
import {
  hexFromSpacedHex,
  hexFromUtf8Text,
} from '../../../config/spacedHexBytes'

export type CliNotebookCloneDestinationAlias =
  | 'cliCloneDestination'
  | 'cliCloneReceiverDestination'
  | 'cliCloneFreshDestination'

function runInstalledOn(
  destinationAlias: CliNotebookCloneDestinationAlias,
  subcommand: 'publish' | 'pull',
  task: 'runInstalledCli' | 'runInstalledCliExpectingRejection'
): Cypress.Chainable<null> {
  return cy.get<string>(`@${destinationAlias}`).then((checkoutDir) =>
    cy.get<string>('@donutPath').then((donutPath) =>
      cy.get<string>('@cliConfigDir').then((configDir) =>
        cy.task<null>(task, {
          donutPath,
          args: ['notebook', subcommand, checkoutDir],
          env: { DONUT_CONFIG_DIR: configDir },
        })
      )
    )
  )
}

function expectCanonicalTreeAt(
  destinationAlias: CliNotebookCloneDestinationAlias,
  seededEntries: string[]
): Cypress.Chainable<null> {
  return cy.get<string>(`@${destinationAlias}`).then((destination) => {
    cy.task<string[]>('listNotebookCheckoutEntries', destination).should(
      'deep.equal',
      [...seededEntries].sort()
    )
    return cy.wrap(null)
  })
}

function expectCheckoutFileAt(
  destinationAlias: CliNotebookCloneDestinationAlias,
  relativePath: string,
  content: string
): Cypress.Chainable<null> {
  return cy.get<string>(`@${destinationAlias}`).then((destination) => {
    cy.readFile(`${destination}/${relativePath}`).should('equal', content)
    return cy.wrap(null)
  })
}

/**
 * The file's bytes on disk, read as hex and never decoded as text, so a
 * scenario can state bytes that are not valid UTF-8 and no reader guesses a
 * format from the file's extension.
 */
function expectCheckoutFileBytesAt(
  destinationAlias: CliNotebookCloneDestinationAlias,
  relativePath: string,
  spacedHex: string
): Cypress.Chainable<null> {
  const expected = hexFromSpacedHex(spacedHex)
  return cy.get<string>(`@${destinationAlias}`).then((destination) =>
    cy
      .task<string>('readCliNotebookCheckoutFileHex', {
        checkoutDir: destination,
        relativePath,
      })
      .then((actual) => {
        expect(
          actual,
          `${relativePath} on disk should hold exactly these bytes`
        ).to.equal(expected)
        return cy.wrap(null)
      })
  )
}

/** The same on-disk byte read, for a file whose exact bytes are UTF-8 text. */
function expectCheckoutFileExactTextAt(
  destinationAlias: CliNotebookCloneDestinationAlias,
  relativePath: string,
  text: string
): Cypress.Chainable<null> {
  return expectCheckoutFileBytesAt(
    destinationAlias,
    relativePath,
    hexFromUtf8Text(text)
  )
}

function readCheckoutStateAt(
  destinationAlias: CliNotebookCloneDestinationAlias
): Cypress.Chainable<CliNotebookCheckoutState> {
  return cy
    .get<string>(`@${destinationAlias}`)
    .then((checkoutDir) =>
      cy.task<CliNotebookCheckoutState>(
        'readCliNotebookCheckoutState',
        checkoutDir
      )
    )
}

function expectCleanCheckoutAtHead(
  destinationAlias: CliNotebookCloneDestinationAlias,
  acceptedHead: string
): Cypress.Chainable<null> {
  return readCheckoutStateAt(destinationAlias).then((state) => {
    expect(
      state.head,
      `HEAD should equal accepted commit ${acceptedHead}`
    ).to.equal(acceptedHead)
    expect(state.status, 'checkout should be clean').to.equal('')
    return cy.wrap(null)
  })
}

function expectCleanAcceptedHeadAt(
  destinationAlias: CliNotebookCloneDestinationAlias
): Cypress.Chainable<null> {
  return cy
    .get<string>('@cliNotebookPublishHead')
    .then((acceptedHead) =>
      expectCleanCheckoutAtHead(destinationAlias, acceptedHead)
    )
}

/**
 * The notebook's current accepted head, which is ahead of the last CLI
 * publication whenever web work was accepted after it.
 */
function expectCleanNotebookAcceptedHeadAt(
  destinationAlias: CliNotebookCloneDestinationAlias,
  notebookName: string
): Cypress.Chainable<null> {
  return cy
    .task<string>('readNotebookAcceptedGitObjectId', notebookName)
    .then((acceptedHead) =>
      expectCleanCheckoutAtHead(destinationAlias, acceptedHead)
    )
}

function commitNoteChangesAt(
  destinationAlias: CliNotebookCloneDestinationAlias,
  files: { relativePath: string; content: string }[]
): Cypress.Chainable<null> {
  return cy.get<string>(`@${destinationAlias}`).then((checkoutDir) =>
    cy
      .task<string>('commitCliNotebookCheckoutNoteChange', {
        checkoutDir,
        files,
      })
      .then((head) => {
        cy.wrap(head).as('cliNotebookPublishHead')
        cy.wrap(
          files.map(({ relativePath, content }) => ({
            relativePath,
            content: `${content}\n`,
          }))
        ).as('cliNotebookProposalFiles')
        return cy.wrap(null)
      })
  )
}

export {
  commitNoteChangesAt,
  expectCanonicalTreeAt,
  expectCheckoutFileAt,
  expectCheckoutFileBytesAt,
  expectCheckoutFileExactTextAt,
  expectCleanAcceptedHeadAt,
  expectCleanNotebookAcceptedHeadAt,
  readCheckoutStateAt,
  runInstalledOn,
}
