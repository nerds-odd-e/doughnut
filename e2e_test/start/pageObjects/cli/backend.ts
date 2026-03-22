/**
 * CLI backend page object. Domain: backend serving CLI and install script.
 */
import { e2eAppBaseUrl } from '../../../support/e2eAppUrl'

export function backend() {
  return {
    bundleAndCopy() {
      cy.task('bundleAndCopyCli')
    },
    expectInstallScriptServed() {
      cy.request('GET', `${e2eAppBaseUrl()}/install`)
        .its('status')
        .should('eq', 200)
    },
    serveVersion(version: string) {
      cy.task('bundleAndCopyCliWithVersion', version)
    },
  }
}
