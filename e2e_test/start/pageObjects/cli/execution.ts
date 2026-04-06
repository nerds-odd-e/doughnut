/**
 * CLI execution page objects.
 * Domain: installation.
 */
import { e2eAppBaseUrl } from '../../../support/e2eAppUrl'
import { DOUGHNUT_OUTPUT_CY_ALIAS } from './nonInteractiveOutputAssertions'
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
        cy.task<string>('runInstalledCli', {
          doughnutPath,
          args: ['version'],
        }).as(DOUGHNUT_OUTPUT_CY_ALIAS)
      })
    },
    runUpdate(baseUrl = e2eAppBaseUrl()) {
      cy.get<string>('@doughnutPath').then((doughnutPath) => {
        cy.task<string>('runInstalledCli', {
          doughnutPath,
          args: ['update'],
          env: { BASE_URL: baseUrl },
        }).as(DOUGHNUT_OUTPUT_CY_ALIAS)
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
