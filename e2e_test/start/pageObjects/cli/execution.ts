/**
 * CLI execution page objects. Domain: installation, non-interactive, interactive, access token, Gmail.
 */
import { backendBaseUrl } from '../../../support/backendUrl'
import { currentGuidance } from './outputAssertions'

const GOOGLE_MOCK_BASE_URL = 'http://localhost:5003'

function cliEnvWithConfigDir(configDir: string): Record<string, string> {
  return {
    DOUGHNUT_CONFIG_DIR: configDir,
    DOUGHNUT_API_BASE_URL: backendBaseUrl(),
  }
}

function runWithConfigDir(args: string[]) {
  cy.get<string>('@cliConfigDir').then((configDir) =>
    cy
      .task('runCliDirectWithArgs', {
        args,
        env: cliEnvWithConfigDir(configDir),
      })
      .as('doughnutOutput')
  )
}

function installation() {
  return {
    installFromLocalhost() {
      cy.task<string>('installCli', backendBaseUrl())
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
    runUpdate(baseUrl = backendBaseUrl()) {
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

function nonInteractive() {
  return {
    runWithInput(input: string) {
      const trimmed = input.trim()
      const exitSuffix =
        trimmed === 'exit' || trimmed === '/exit' ? '' : `\nexit`
      cy.task('runCliDirectWithInput', {
        input: `${input}${exitSuffix}`,
      }).as('doughnutOutput')
    },
    runWithCommand(cmd: string) {
      runWithConfigDir(['-c', cmd])
    },
    runVersion() {
      cy.task('runCliDirectWithArgs', { args: ['version'] }).as(
        'doughnutOutput'
      )
    },
  }
}

function interactive() {
  return {
    input(text: string) {
      cy.task<string>('sendToInteractiveCli', { input: text }).as(
        'doughnutOutput'
      )
    },
    pressEsc() {
      cy.task<string>('sendToInteractiveCli', { input: '\x1b' }).as(
        'doughnutOutput'
      )
    },
    answerToPrompt(answer: string, expectedPromptText: string) {
      currentGuidance().expectContains(expectedPromptText)
      cy.task<string>('sendToInteractiveCli', { input: answer }).as(
        'doughnutOutput'
      )
    },
    inputDownArrowSelection(command: string) {
      cy.task<string>('sendToInteractiveCli', { input: command })
      cy.task<string>('sendToInteractiveCli', { input: '2' }).as(
        'doughnutOutput'
      )
    },
  }
}

function runWithCommandAndSuffix(command: string, suffix: string) {
  runWithConfigDir(['-c', `${command} ${suffix}`])
}

function accessToken() {
  return {
    runWithSavedToken(command: string) {
      cy.get<string>('@savedAccessToken').then((token) =>
        runWithCommandAndSuffix(command, token)
      )
    },
    runWithToken(command: string, token: string) {
      runWithCommandAndSuffix(command, token)
    },
    runWithLabel(command: string, label: string) {
      runWithCommandAndSuffix(command, label)
    },
  }
}

function gmail() {
  return {
    addWithSimulatedOAuth() {
      cy.task('runCliDirectWithGmailAdd', {
        googleBaseUrl: GOOGLE_MOCK_BASE_URL,
      })
        .its('stdout')
        .as('doughnutOutput')
    },
    lastEmailWithPreconfiguredAccount() {
      cy.task('runCliDirectWithLastEmail', {
        googleBaseUrl: GOOGLE_MOCK_BASE_URL,
      }).as('doughnutOutput')
    },
  }
}

export { installation, nonInteractive, interactive, accessToken, gmail }
