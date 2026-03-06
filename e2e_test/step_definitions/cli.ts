import { Given, When, Then } from '@badeball/cypress-cucumber-preprocessor'
import { mock_services } from '../start'

const BASE_URL = 'http://localhost:9081'
const GOOGLE_MOCK_URL = 'http://localhost:5003'

Given('the backend is serving the CLI and install script', () => {
  cy.request('GET', `${BASE_URL}/install`).its('status').should('eq', 200)
})

Given('the CLI is built with version {string}', (version: string) => {
  cy.task('bundleAndCopyCliWithVersion', version)
})

When('I install the CLI from localhost without affecting my system', () => {
  cy.task<string>('installCli', BASE_URL).as('doughnutPath')
})

When('I run the installed doughnut command', () => {
  cy.get<string>('@doughnutPath').then((doughnutPath) => {
    cy.exec(`echo "exit" | ${doughnutPath}`, { timeout: 5000 })
      .its('stdout')
      .as('doughnutOutput')
  })
})

When(
  'I run the installed doughnut command with input {string}',
  (input: string) => {
    cy.get<string>('@doughnutPath').then((doughnutPath) => {
      cy.exec(`printf '%s\\n' "${input}" | ${doughnutPath}`, { timeout: 5000 })
        .its('stdout')
        .as('doughnutOutput')
    })
  }
)

When('I run the installed doughnut version command', () => {
  cy.get<string>('@doughnutPath').then((doughnutPath) => {
    cy.exec(`${doughnutPath} version`, { timeout: 5000 })
      .its('stdout')
      .as('doughnutOutput')
  })
})

When('the backend serves the CLI with version {string}', (version: string) => {
  cy.task('bundleAndCopyCliWithVersion', version)
})

When(
  'I run the installed doughnut update command with BASE_URL from localhost',
  () => {
    cy.get<string>('@doughnutPath').then((doughnutPath) => {
      cy.exec(`${doughnutPath} update`, {
        timeout: 15000,
        env: { BASE_URL },
      })
        .its('stdout')
        .as('doughnutOutput')
    })
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

When('I run the CLI add gmail command with simulated OAuth callback', () => {
  cy.get<string>('@doughnutPath').then((doughnutPath) => {
    cy.task('runCliWithGmailAdd', {
      doughnutPath,
      googleBaseUrl: GOOGLE_MOCK_URL,
    })
      .its('stdout')
      .as('doughnutOutput')
  })
})

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

When('I run the CLI last email command with pre-configured account', () => {
  cy.get<string>('@doughnutPath').then((doughnutPath) => {
    cy.task('runCliWithLastEmail', {
      doughnutPath,
      googleBaseUrl: GOOGLE_MOCK_URL,
    }).as('doughnutOutput')
  })
})
