/**
 * CLI backend page object. Domain: backend serving CLI and install script.
 */
import { e2eAppBaseUrl } from '../../../support/e2eAppUrl'
import { newerCliThanInstalled } from './cliPackageVersion'

export function backend() {
  return {
    bundleCliForE2eInstall() {
      cy.task('bundleCliE2eInstall')
    },
    expectInstallScriptServed() {
      cy.request('GET', `${e2eAppBaseUrl()}/install`)
        .its('status')
        .should('eq', 200)
    },
    serveNewerThanInstalled() {
      newerCliThanInstalled().then(({ newer }) => {
        cy.task('bundleCliE2eInstallWithVersion', newer)
      })
    },
    removeE2eInstallCliBundle() {
      cy.task('removeE2eInstallCliBundle')
    },
  }
}
