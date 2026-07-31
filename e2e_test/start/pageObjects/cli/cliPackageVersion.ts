/**
 * Reads the bundled CLI version from cli/package.json (single source of truth).
 */

import {
  cliUpdateSuccessMessage,
  formatCliVersionBanner,
  newerCliVersionThan,
} from '../../../config/cliVersion'

export function cliPackageVersion(): Cypress.Chainable<string> {
  return cy.readFile('cli/package.json').its('version')
}

export function installedCliVersionBanner(): Cypress.Chainable<string> {
  return cliPackageVersion().then((version) => formatCliVersionBanner(version))
}

export function newerCliThanInstalled(): Cypress.Chainable<{
  installed: string
  newer: string
}> {
  return cliPackageVersion().then((installed) => ({
    installed,
    newer: newerCliVersionThan(installed),
  }))
}

export function installedToNewerUpdateMessage(): Cypress.Chainable<string> {
  return newerCliThanInstalled().then(({ installed, newer }) =>
    cliUpdateSuccessMessage(installed, newer)
  )
}
