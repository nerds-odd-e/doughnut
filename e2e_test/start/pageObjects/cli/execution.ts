/**
 * CLI execution page objects.
 * Domain: installation, non-interactive, interactive, access token, Gmail.
 */
import { backendBaseUrl } from '../../../support/backendUrl'
import { currentGuidance } from './outputAssertions'

const GOOGLE_MOCK_BASE_URL = 'http://localhost:5003'

export function envForCliWithConfigDir(
  configDir: string
): Record<string, string> {
  return {
    DOUGHNUT_CONFIG_DIR: configDir,
    DOUGHNUT_API_BASE_URL: backendBaseUrl(),
  }
}

function envForGmail(
  configDir: string,
  noBrowser = false
): Record<string, string> {
  return {
    DOUGHNUT_CONFIG_DIR: configDir,
    GOOGLE_BASE_URL: GOOGLE_MOCK_BASE_URL,
    ...(noBrowser && { DOUGHNUT_NO_BROWSER: '1' }),
  }
}

function runCliWithScenarioConfigDir(args: string[]) {
  cy.get<string>('@cliConfigDir').then((configDir) =>
    cy
      .task('runCliDirectWithArgs', {
        args,
        env: envForCliWithConfigDir(configDir),
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
      runCliWithScenarioConfigDir(['-c', cmd])
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
    enterSlashCommand(command: string) {
      cy.task<string>('sendInteractiveCliSlashCommand', { command }).as(
        'doughnutOutput'
      )
    },
    enterLine(line: string) {
      cy.task<string>('sendInteractiveCliLine', { line }).as('doughnutOutput')
    },
    pressEnter() {
      cy.task<string>('sendInteractiveCliEnter').as('doughnutOutput')
    },
    pressEsc() {
      cy.task<string>('sendInteractiveCliEsc').as('doughnutOutput')
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

function accessToken() {
  const runAccessTokenCommand = (command: string, value: string) =>
    runCliWithScenarioConfigDir(['-c', `${command} ${value}`])
  return {
    runWithSavedToken(command: string) {
      cy.get<string>('@savedAccessToken').then((token) =>
        runAccessTokenCommand(command, token)
      )
    },
    runWithToken(command: string, token: string) {
      runAccessTokenCommand(command, token)
    },
    runWithLabel(command: string, label: string) {
      runAccessTokenCommand(command, label)
    },
  }
}

const gmailConfigForAdd = {
  clientId: 'e2e-test-client',
  clientSecret: 'e2e-test-secret',
  accounts: [],
}
const gmailConfigWithMockAccount = {
  accounts: [
    {
      email: 'e2e@gmail.com',
      accessToken: 'mock_access_token',
      refreshToken: 'mock_refresh_token',
      expiresAt: Date.now() + 3600_000,
    },
  ],
}

function gmail() {
  return {
    addWithSimulatedOAuth() {
      cy.task<string>('createCliConfigDirWithGmail', gmailConfigForAdd).then(
        (configDir) =>
          cy
            .task('runCliDirectWithInput', {
              input: '/add gmail\nexit',
              env: envForGmail(configDir, true),
              simulateOAuthCallback: true,
            })
            .as('doughnutOutput')
      )
    },
    lastEmailWithPreconfiguredAccount() {
      cy.task<string>(
        'createCliConfigDirWithGmail',
        gmailConfigWithMockAccount
      ).then((configDir) =>
        cy
          .task('runCliDirectWithInput', {
            input: '/last email\nexit',
            env: envForGmail(configDir),
          })
          .as('doughnutOutput')
      )
    },
  }
}

export { installation, nonInteractive, interactive, accessToken, gmail }
