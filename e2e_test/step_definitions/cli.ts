import { Given, When, Then } from '@badeball/cypress-cucumber-preprocessor'
import { mock_services } from '../start'
import start from '../start'

const BASE_URL = 'http://localhost:9081'
const GOOGLE_MOCK_URL = 'http://localhost:5003'

function runCliWithConfig(args: string[]) {
  return cy.get<string>('@cliConfigDir').then((configDir) =>
    cy
      .task('runCliDirectWithArgs', {
        args,
        env: {
          DOUGHNUT_CONFIG_DIR: configDir,
          DOUGHNUT_API_BASE_URL: BASE_URL,
        },
      })
      .as('doughnutOutput')
  )
}

Given('the backend is serving the CLI and install script', () => {
  cy.request('GET', `${BASE_URL}/install`).its('status').should('eq', 200)
})

Given('I have the CLI configured with a valid access token', () => {
  start
    .mainMenu()
    .userOptions()
    .manageAccessTokens()
    .generateToken('Recall CLI Token')
    .then((token) => {
      cy.wrap(token).as('savedAccessToken')
    })
  cy.get<string>('@savedAccessToken').then((token) =>
    runCliWithConfig(['-c', `/add-access-token ${token}`])
  )
  cy.get('@doughnutOutput').should('include', 'Token added')
})

Given('the CLI is built with version {string}', (version: string) => {
  cy.task('bundleAndCopyCliWithVersion', version)
})

When('I install the CLI from localhost without affecting my system', () => {
  cy.task<string>('installCli', BASE_URL).as('doughnutPath')
})

When('I run the installed doughnut command', () => {
  cy.get<string>('@doughnutPath').then((doughnutPath) => {
    cy.task<string>('runInstalledCli', { doughnutPath, input: 'exit\n' }).as(
      'doughnutOutput'
    )
  })
})

When('I run the installed doughnut version command', () => {
  cy.get<string>('@doughnutPath').then((doughnutPath) => {
    cy.task<string>('runInstalledCli', {
      doughnutPath,
      args: ['version'],
    }).as('doughnutOutput')
  })
})

When('I run the doughnut command with input {string}', (input: string) => {
  cy.task('runCliDirectWithInput', { input }).as('doughnutOutput')
})

When(
  'I run the doughnut command in interactive mode with input {string}',
  (input: string) => {
    cy.get<string>('@cliConfigDir').then((configDir) =>
      cy
        .task('runCliDirectWithInput', {
          input: `${input}\nexit\n`,
          env: {
            DOUGHNUT_CONFIG_DIR: configDir,
            DOUGHNUT_API_BASE_URL: BASE_URL,
          },
        })
        .as('doughnutOutput')
    )
  }
)

When(
  'I run the doughnut command in interactive mode with input {string} and {string}',
  (command: string, answer: string) => {
    cy.get<string>('@cliConfigDir').then((configDir) =>
      cy
        .task('runCliDirectWithInput', {
          input: `${command}\n${answer}\nexit\n`,
          env: {
            DOUGHNUT_CONFIG_DIR: configDir,
            DOUGHNUT_API_BASE_URL: BASE_URL,
          },
        })
        .as('doughnutOutput')
    )
  }
)

When(
  'I run the doughnut command in interactive mode with input {string} and {string} and {string}',
  (command: string, answer1: string, answer2: string) => {
    cy.get<string>('@cliConfigDir').then((configDir) =>
      cy
        .task('runCliDirectWithInput', {
          input: `${command}\n${answer1}\n${answer2}\nexit\n`,
          env: {
            DOUGHNUT_CONFIG_DIR: configDir,
            DOUGHNUT_API_BASE_URL: BASE_URL,
          },
        })
        .as('doughnutOutput')
    )
  }
)

When(
  'I run the doughnut command in interactive mode with down-arrow selection for {string}',
  (command: string) => {
    const downArrow = '\x1b[B'
    cy.get<string>('@cliConfigDir').then((configDir) =>
      cy
        .task('runCliDirectWithInputAndPty', {
          input: `${command}\n${downArrow}\r\nexit\n`,
          fallbackInput: `${command}\n2\nexit\n`,
          env: {
            DOUGHNUT_CONFIG_DIR: configDir,
            DOUGHNUT_API_BASE_URL: BASE_URL,
          },
        })
        .as('doughnutOutput')
    )
  }
)

When('I run the doughnut command with -c {string}', (input: string) => {
  runCliWithConfig(['-c', input])
})

When('I run the doughnut version command', () => {
  cy.task('runCliDirectWithArgs', { args: ['version'] }).as('doughnutOutput')
})

When('the backend serves the CLI with version {string}', (version: string) => {
  cy.task('bundleAndCopyCliWithVersion', version)
})

When(
  'I run the installed doughnut update command with BASE_URL from localhost',
  () => {
    cy.get<string>('@doughnutPath').then((doughnutPath) => {
      cy.task<string>('runInstalledCli', {
        doughnutPath,
        args: ['update'],
        env: { BASE_URL },
      }).as('doughnutOutput')
    })
  }
)

When('I run the doughnut CLI add-access-token with the saved token', () => {
  cy.get<string>('@savedAccessToken').then((token) => {
    runCliWithConfig(['-c', `/add-access-token ${token}`])
  })
})

When(
  'I run the doughnut CLI add-access-token with token {string}',
  (token: string) => {
    runCliWithConfig(['-c', `/add-access-token ${token}`])
  }
)

When(
  'I run the doughnut CLI remove-access-token with label {string}',
  (label: string) => {
    runCliWithConfig(['-c', `/remove-access-token ${label}`])
  }
)

When(
  'I run the doughnut CLI create-access-token with label {string}',
  (label: string) => {
    runCliWithConfig(['-c', `/create-access-token ${label}`])
  }
)

When(
  'I run the doughnut CLI remove-access-token-completely with label {string}',
  (label: string) => {
    runCliWithConfig(['-c', `/remove-access-token-completely ${label}`])
  }
)

Then('I should see {string}', (expected: string) => {
  cy.get('@doughnutOutput').should('include', expected)
})

Given(
  'the Google API mock returns tokens and profile for {string}',
  (email: string) => {
    cy.wrap(
      mock_services
        .google()
        .stubTokenExchange('mock_access_token', 'mock_refresh_token')
        .then(() => mock_services.google().stubGmailProfile(email))
    )
  }
)

Given(
  'the Google API mock returns messages and message {string} with subject {string}',
  (messageId: string, subject: string) => {
    cy.wrap(
      mock_services
        .google()
        .stubGmailMessages([{ id: messageId }])
        .then(() => mock_services.google().stubGmailMessage(messageId, subject))
    )
  }
)

When('I run the CLI add gmail command with simulated OAuth callback', () => {
  cy.task('runCliDirectWithGmailAdd', {
    googleBaseUrl: GOOGLE_MOCK_URL,
  })
    .its('stdout')
    .as('doughnutOutput')
})

When('I run the CLI last email command with pre-configured account', () => {
  cy.task('runCliDirectWithLastEmail', {
    googleBaseUrl: GOOGLE_MOCK_URL,
  }).as('doughnutOutput')
})
