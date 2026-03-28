/**
 * CLI execution page objects.
 * Domain: installation, interactive, access token.
 */
import {
  INTERACTIVE_CLI_PTY_KEYSTROKE_TASK,
  type InteractiveCliPtyKeystroke,
} from '../../../config/interactiveCliPtyTypes'
import { e2eAppBaseUrl } from '../../../support/e2eAppUrl'
export function envForCliWithConfigDir(
  configDir: string
): Record<string, string> {
  return {
    DOUGHNUT_CONFIG_DIR: configDir,
    DOUGHNUT_API_BASE_URL: e2eAppBaseUrl(),
  }
}

function installation() {
  return {
    installFromLocalhost() {
      cy.task<string>('installCli', e2eAppBaseUrl())
        .should('be.a', 'string')
        .and('not.be.empty')
        .as('doughnutPath')
    },
    runInstalled() {
      cy.get<string>('@doughnutPath')
        .should('be.a', 'string')
        .and('not.be.empty')
        .then((doughnutPath) => {
          cy.task<string>('runInstalledCli', {
            doughnutPath,
            input: 'exit\n',
          }).as('doughnutOutput')
        })
    },
    runVersion() {
      cy.get<string>('@doughnutPath').then((doughnutPath) => {
        cy.task<string>('runInstalledCli', {
          doughnutPath,
          args: ['version'],
        }).as('doughnutOutput')
      })
    },
    runUpdate(baseUrl = e2eAppBaseUrl()) {
      cy.get<string>('@doughnutPath').then((doughnutPath) => {
        cy.task<string>('runInstalledCli', {
          doughnutPath,
          args: ['update'],
          env: { BASE_URL: baseUrl },
        }).as('doughnutOutput')
      })
    },
  }
}

function applyInteractiveCliPtyKeystroke(
  keystroke: InteractiveCliPtyKeystroke
) {
  return cy
    .task<string>(INTERACTIVE_CLI_PTY_KEYSTROKE_TASK, keystroke)
    .as('doughnutOutput')
}

function interactive() {
  return {
    enterSlashCommand(commandLine: string) {
      applyInteractiveCliPtyKeystroke({
        kind: 'slashCommand',
        commandLine,
      })
    },
    enterLine(text: string) {
      applyInteractiveCliPtyKeystroke({ kind: 'line', text })
    },
    pressEnter() {
      applyInteractiveCliPtyKeystroke({ kind: 'enter' })
    },
  }
}

export { installation, interactive }
