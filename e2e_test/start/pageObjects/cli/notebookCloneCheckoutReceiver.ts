/**
 * Second-clone checkout: pull accepted history, then commit and publish
 * from the checkout that received it.
 */
import type { CliNotebookCheckoutState } from '../../../config/cliE2eNotebookCloneTasks'

type CliNotebookCloneDestinationAlias =
  | 'cliCloneDestination'
  | 'cliCloneReceiverDestination'

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

function expectCleanAcceptedHeadAt(
  destinationAlias: CliNotebookCloneDestinationAlias
): Cypress.Chainable<null> {
  return cy.get<string>(`@${destinationAlias}`).then((checkoutDir) =>
    cy.get<string>('@cliNotebookPublishHead').then((acceptedHead) =>
      cy
        .task<CliNotebookCheckoutState>(
          'readCliNotebookCheckoutState',
          checkoutDir
        )
        .then((state) => {
          expect(
            state.head,
            `HEAD should equal accepted commit ${acceptedHead}`
          ).to.equal(acceptedHead)
          expect(state.status, 'checkout should be clean').to.equal('')
          return cy.wrap(null)
        })
    )
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

function notebookCloneCheckoutReceiver() {
  return {
    /**
     * Commits a content-only edit in `@cliCloneReceiverDestination` after
     * that checkout has received accepted history. Sets `@cliNotebookPublishHead`.
     */
    commitReceiverEdit(
      relativePath: string,
      content: string
    ): Cypress.Chainable<null> {
      return commitNoteChangesAt('cliCloneReceiverDestination', [
        { relativePath, content },
      ])
    },
    publishReceiver(): Cypress.Chainable<null> {
      return runInstalledOn(
        'cliCloneReceiverDestination',
        'publish',
        'runInstalledCli'
      )
    },
    /**
     * Pulls accepted history onto `@cliCloneReceiverDestination` without
     * publishing. Captures the pre-pull head for the ancestor assertion and
     * the post-pull tip as `@cliNotebookRebasedCheckout` (same alias as the
     * primary-checkout pull path) for publish acceptance.
     */
    pullReceiver(): Cypress.Chainable<null> {
      return cy
        .get<string>('@cliCloneReceiverDestination')
        .then((checkoutDir) =>
          cy
            .task<CliNotebookCheckoutState>(
              'readCliNotebookCheckoutState',
              checkoutDir
            )
            .then((original) => {
              cy.wrap(original.head).as('cliCloneReceiverOriginalHead')
              return runInstalledOn(
                'cliCloneReceiverDestination',
                'pull',
                'runInstalledCli'
              ).then(() =>
                cy
                  .task<CliNotebookCheckoutState>(
                    'readCliNotebookCheckoutState',
                    checkoutDir
                  )
                  .then((rebased) => {
                    cy.wrap(rebased).as('cliNotebookRebasedCheckout')
                    return cy.wrap(null)
                  })
              )
            })
        )
    },
    expectReceiverCanonicalTreeFor(
      seededEntries: string[]
    ): Cypress.Chainable<null> {
      return expectCanonicalTreeAt('cliCloneReceiverDestination', seededEntries)
    },
    expectReceiverOriginalHeadIsAncestor(): Cypress.Chainable<null> {
      return cy
        .get<string>('@cliCloneReceiverDestination')
        .then((checkoutDir) =>
          cy.get<string>('@cliCloneReceiverOriginalHead').then((originalHead) =>
            cy
              .task<boolean>('cliNotebookCheckoutIsAncestorOfHead', {
                checkoutDir,
                ancestor: originalHead,
              })
              .then((isAncestor) => {
                expect(
                  isAncestor,
                  `original head ${originalHead} should be an ancestor of HEAD in ${checkoutDir}`
                ).to.equal(true)
                return cy.wrap(null)
              })
          )
        )
    },
    expectReceiverAtAcceptedHead(): Cypress.Chainable<null> {
      return expectCleanAcceptedHeadAt('cliCloneReceiverDestination')
    },
    expectReceiverCheckoutFile(
      relativePath: string,
      content: string
    ): Cypress.Chainable<null> {
      return expectCheckoutFileAt(
        'cliCloneReceiverDestination',
        relativePath,
        content
      )
    },
  }
}

export {
  commitNoteChangesAt,
  expectCanonicalTreeAt,
  expectCheckoutFileAt,
  expectCleanAcceptedHeadAt,
  notebookCloneCheckoutReceiver,
  runInstalledOn,
}
