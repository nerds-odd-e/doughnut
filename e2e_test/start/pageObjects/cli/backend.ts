/**
 * CLI backend page object. Domain: backend serving CLI and install script.
 */
import { backendBaseUrl } from '../../../support/backendUrl'

export function backend() {
  return {
    expectInstallScriptServed() {
      cy.request('GET', `${backendBaseUrl()}/install`)
        .its('status')
        .should('eq', 200)
    },
    serveVersion(version: string) {
      cy.task('bundleAndCopyCliWithVersion', version)
    },
  }
}
