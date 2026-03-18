/**
 * CLI setup page object. Domain: test lifecycle (config dir, interactive session).
 * Used by Before/After hooks for CLI scenarios.
 */
import { envForCliWithConfigDir } from './execution'

export function setup() {
  return {
    createConfigDir() {
      cy.task('createCliConfigDir').as('cliConfigDir')
    },
    startInteractiveSession() {
      cy.task('stopInteractiveCli')
      cy.get<string>('@cliConfigDir').then((configDir) =>
        cy.task('startInteractiveCli', {
          env: envForCliWithConfigDir(configDir),
        })
      )
    },
    stopInteractiveSession() {
      cy.task('stopInteractiveCli')
    },
  }
}
