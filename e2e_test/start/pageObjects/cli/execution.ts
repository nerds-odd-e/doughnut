/**
 * CLI execution page objects.
 * Domain: installation, interactive, access token, Gmail.
 */
import {
  INTERACTIVE_CLI_PTY_KEYSTROKE_TASK,
  type InteractiveCliPtyKeystroke,
} from '../../../config/interactiveCliPtyTypes'
import { e2eAppBaseUrl } from '../../../support/e2eAppUrl'
import { historyOutput, visibleInteractivePtyScreen } from './outputAssertions'

const GOOGLE_MOCK_BASE_URL = 'http://localhost:5003'

export function envForCliWithConfigDir(
  configDir: string
): Record<string, string> {
  return {
    DOUGHNUT_CONFIG_DIR: configDir,
    DOUGHNUT_API_BASE_URL: e2eAppBaseUrl(),
  }
}

/** PTY env for Gmail CLI E2E (mock Google HTTP + optional headless OAuth). */
export function envForInteractiveGmail(
  configDir: string,
  opts: { noBrowser?: boolean } = {}
): Record<string, string> {
  return {
    ...envForCliWithConfigDir(configDir),
    GOOGLE_BASE_URL: GOOGLE_MOCK_BASE_URL,
    ...(opts.noBrowser ? { DOUGHNUT_NO_BROWSER: '1' } : {}),
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
    typeRawKey(char: string) {
      applyInteractiveCliPtyKeystroke({ kind: 'rawKey', char })
    },
    /** Recall Ink y/n: one keypress (a separate Enter would mean “no”). */
    answerToPrompt(answer: string, expectedPromptText: string) {
      visibleInteractivePtyScreen().expectContains(expectedPromptText)
      applyInteractiveCliPtyKeystroke({ kind: 'rawKey', char: answer })
    },
    inputDownArrowSelection(commandLine: string) {
      cy.task<string>(INTERACTIVE_CLI_PTY_KEYSTROKE_TASK, {
        kind: 'slashCommand',
        commandLine,
      })
      applyInteractiveCliPtyKeystroke({ kind: 'line', text: '2' })
    },
  }
}

function accessToken() {
  return {
    addSavedTokenInteractive() {
      cy.get<string>('@savedAccessToken').then((token) =>
        applyInteractiveCliPtyKeystroke({
          kind: 'slashCommand',
          commandLine: `/add-access-token ${token}`,
        })
      )
      historyOutput().expectContains('Token added')
    },
  }
}

export { installation, interactive, accessToken }
