/**
 * CLI setup page object. Domain: test lifecycle (config dir, interactive session).
 * Used by Before/After hooks for CLI scenarios.
 */
import { envForCliWithConfigDir, envForInteractiveGmail } from './execution'

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
    /** Gmail E2E: mock Google HTTP; optional OAuth callback simulation on the PTY stdout stream. */
    startInteractiveGmailSession(opts: { oauthSimulated: boolean }) {
      cy.task('stopInteractiveCli')
      cy.get<string>('@cliConfigDir').then((configDir) =>
        cy.task('startInteractiveCli', {
          env: envForInteractiveGmail(configDir, {
            noBrowser: opts.oauthSimulated,
          }),
          simulateOAuthCallback: opts.oauthSimulated,
        })
      )
    },
    stopInteractiveSession() {
      cy.task('stopInteractiveCli')
    },
  }
}
