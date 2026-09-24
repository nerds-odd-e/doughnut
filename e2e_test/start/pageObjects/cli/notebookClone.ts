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
    destinationAlias:
      | 'cliCloneDestination'
      | 'cliCloneReceiverDestination'
      | 'cliCloneFreshDestination',
    options?: { useExistingDestination?: boolean }
  ): Cypress.Chainable<null> {
    return testability()
      .getNotebookIdByName(notebookName)
      .then((notebookId) => {
        const destination =
          options?.useExistingDestination === true
            ? cy.get<string>(`@${destinationAlias}`)
            : cy
                .task<string>('createCliNotebookCloneDestination')
                .then((path) => {
                  cy.wrap(path).as(destinationAlias)
                  return cy.wrap(path)
                })
        return destination.then((checkoutPath) =>
          cy.get<string>('@donutPath').then((donutPath) =>
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
                        checkoutPath,
                      ],
                      env: { DONUT_CONFIG_DIR: configDir },
                    })
                  )
              )
            )
          )
        )
      })
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
     * Explicit legacy-raw fixture: demotes product-created LFS notebooks so pull-based scenarios
     * keep proving the raw journey rather than silently becoming LFS.
     */
    forceRawGitBinding(notebookName: string) {
      return testability().forceRawNotebookGitBindingForTestability(
        notebookName
      )
    },

    /** Runs the one-time conversion of a legacy raw notebook to LFS storage. */
    convertToLfs(notebookName: string) {
      return testability().convertRawNotebookToLfsForTestability(notebookName)
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
    /**
     * A checkout cloned after accepted history has advanced, aliased
     * `@cliCloneFreshDestination`. It holds no local history of its own.
     */
    cloneNotebookIntoFresh(notebookName: string): Cypress.Chainable<null> {
      return cloneWithTask(
        notebookName,
        'runInstalledCli',
        'cliCloneFreshDestination'
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
    /**
     * Clones into an already-aliased `@cliCloneDestination` that already exists,
     * expecting the CLI to refuse without modifying it.
     */
    cloneNotebookExpectingRejectionIntoExisting(
      notebookName: string
    ): Cypress.Chainable<null> {
      return cloneWithTask(
        notebookName,
        'runInstalledCliExpectingRejection',
        'cliCloneDestination',
        { useExistingDestination: true }
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
