/**
 * CLI notebook clone page objects. Domain: `donut notebook clone <notebook-id> <destination>`
 * (non-interactive, installed-binary invocation — same `runInstalledCli` mechanism as
 * `installation()`), and reading back the resulting Git checkout.
 */
import testability from '../../testability'

function notebookClone() {
  return {
    /**
     * Resolves the notebook's numeric id, writes the owner's saved access token into the
     * isolated `@cliConfigDir`, then runs the installed CLI's non-interactive clone command
     * into a fresh test-owned destination (aliased `@cliCloneDestination`).
     */
    cloneNotebookInto(notebookName: string): Cypress.Chainable<null> {
      return testability()
        .getNotebookIdByName(notebookName)
        .then((notebookId) =>
          cy
            .task<string>('createCliNotebookCloneDestination')
            .then((destination) => {
              cy.wrap(destination).as('cliCloneDestination')
              return cy.get<string>('@donutPath').then((donutPath) =>
                cy.get<string>('@cliConfigDir').then((configDir) =>
                  cy.get<string>('@savedAccessToken').then((token) =>
                    cy
                      .task<null>('writeCliAccessToken', {
                        configDir,
                        token,
                      })
                      .then(() =>
                        cy.task<null>('runInstalledCli', {
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
    },
  }
}

function notebookCloneCheckout() {
  return {
    /** Uses the system `git` executable (ADR 0002): one branch, one parentless commit, no dirt. */
    expectCleanSingleCommitCheckoutOnBranch(
      branch: string
    ): Cypress.Chainable<null> {
      return cy.get<string>('@cliCloneDestination').then((destination) => {
        cy.exec(`git -C ${destination} rev-parse --abbrev-ref HEAD`)
          .its('stdout')
          .should('equal', branch)
        cy.exec(`git -C ${destination} rev-list --max-parents=0 --count HEAD`)
          .its('stdout')
          .should('equal', '1')
        cy.exec(`git -C ${destination} status --porcelain`)
          .its('stdout')
          .should('equal', '')
        return cy.wrap(null)
      })
    },
    /** Canonical ADR-0004 tree only: seeded readmes/notes, no `.donut`, manifest, id, sidecar, or db files. */
    expectCanonicalTreeFor(seededEntries: string[]): Cypress.Chainable<null> {
      return cy.get<string>('@cliCloneDestination').then((destination) => {
        cy.task<string[]>('listNotebookCheckoutEntries', destination).should(
          'deep.equal',
          [...seededEntries].sort()
        )
        return cy.wrap(null)
      })
    },
  }
}

export { notebookClone, notebookCloneCheckout }
