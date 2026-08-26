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
        .as('donutPath')
    },
    runVersion() {
      cy.get<string>('@donutPath').then((donutPath) => {
        cy.task<null>('runInstalledCli', {
          donutPath,
          args: ['version'],
        })
      })
    },
    runUpdate(baseUrl = e2eAppBaseUrl()) {
      cy.get<string>('@donutPath').then((donutPath) => {
        cy.task<null>('runInstalledCli', {
          donutPath,
          args: ['update'],
          env: { BASE_URL: baseUrl },
        })
      })
    },
    runInteractiveMode() {
      cy.get<string>('@donutPath').then((donutPath) => {
        ttyAssertTerminal().startInstalledInteractive({ donutPath })
      })
    },
  }
}

export { installation }
