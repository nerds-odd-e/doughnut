/**
 * CLI notebook clone page objects. Domain: `donut notebook clone <notebook-id> <destination>`
 * (non-interactive, installed-binary invocation — same `runInstalledCli` mechanism as
 * `installation()`).
 */
import testability from '../../testability'

function notebookClone() {
  function cloneWithTask(
    notebookName: string,
    task: 'runInstalledCli' | 'runInstalledCliExpectingRejection',
    destinationAlias: 'cliCloneDestination' | 'cliCloneReceiverDestination'
  ): Cypress.Chainable<null> {
    return testability()
      .getNotebookIdByName(notebookName)
      .then((notebookId) =>
        cy
          .task<string>('createCliNotebookCloneDestination')
          .then((destination) => {
            cy.wrap(destination).as(destinationAlias)
            return cy.get<string>('@donutPath').then((donutPath) =>
              cy.get<string>('@cliConfigDir').then((configDir) =>
                cy.get<string>('@savedAccessToken').then((token) =>
                  cy
                    .task<null>('writeCliAccessToken', {
                      configDir,
                      token,
                    })
                    .then(() =>
                      cy.task<null>(task, {
                        donutPath,
                        args: [
                          'notebook',
                          'clone',
                          String(notebookId),
                          destination,
                        ],
                        env: { DONUT_CONFIG_DIR: configDir },
                      })
                    )
                )
              )
            )
          })
      )
  }
  return {
    useAccessTokenOf(userIdentifier: string) {
      return cy
        .wrap(`access-token-of-${userIdentifier}`, { log: false })
        .as('savedAccessToken')
    },
    /**
     * Test-only setup helper: rebuilds the notebook's `NotebookGitBinding` after a fixture seeds
     * unsupported structural changes such as folders or readmes. It establishes the initial
     * accepted baseline; do not use it after the content edit under test.
     */
    resnapshotGitBinding(notebookName: string) {
      return testability().resnapshotNotebookGitBindingForTestability(
        notebookName
      )
    },

    /**
     * Resolves the notebook's numeric id, writes the owner's saved access token into the
     * isolated `@cliConfigDir`, then runs the installed CLI's non-interactive clone command
     * into a fresh test-owned destination (aliased `@cliCloneDestination`).
     */
    cloneNotebookInto(notebookName: string): Cypress.Chainable<null> {
      return cloneWithTask(
        notebookName,
        'runInstalledCli',
        'cliCloneDestination'
      )
    },
    /**
     * A second clean checkout of the same accepted head, aliased
     * `@cliCloneReceiverDestination`. Does not publish. Use before the
     * publisher checkout advances accepted history, then pull this checkout.
     */
    cloneNotebookIntoReceiver(notebookName: string): Cypress.Chainable<null> {
      return cloneWithTask(
        notebookName,
        'runInstalledCli',
        'cliCloneReceiverDestination'
      )
    },
    cloneNotebookExpectingRejection(
      notebookName: string
    ): Cypress.Chainable<null> {
      return cloneWithTask(
        notebookName,
        'runInstalledCliExpectingRejection',
        'cliCloneDestination'
      )
    },
    expectDestinationAbsent(): Cypress.Chainable<null> {
      return cy.get<string>('@cliCloneDestination').then((destination) => {
        cy.task<boolean>(
          'cliNotebookCloneDestinationExists',
          destination
        ).should('equal', false)
        return cy.wrap(null)
      })
    },
  }
}

export { notebookClone }
