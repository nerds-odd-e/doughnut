/**
 * CLI execution page objects.
 * Domain: installation.
 */
import { e2eAppBaseUrl } from '../../../support/e2eAppUrl'
import { ttyAssertTerminal } from './ttyAssertTerminal'

function installation() {
  return {
    installFromLocalhost() {
      cy.task<string>('installCli', e2eAppBaseUrl())
        .should('be.a', 'string')
        .and('not.be.empty')
        .as('doughnutPath')
    },
    runVersion() {
      cy.get<string>('@doughnutPath').then((doughnutPath) => {
        cy.task<null>('runInstalledCli', {
          doughnutPath,
          args: ['version'],
        })
      })
    },
    runUpdate(baseUrl = e2eAppBaseUrl()) {
      cy.get<string>('@doughnutPath').then((doughnutPath) => {
        cy.task<null>('runInstalledCli', {
          doughnutPath,
          args: ['update'],
          env: { BASE_URL: baseUrl },
        })
      })
    },
    runInteractiveMode() {
      cy.get<string>('@doughnutPath').then((doughnutPath) => {
        ttyAssertTerminal().startInstalledInteractive({ doughnutPath })
      })
    },
  }
}

export { installation }
