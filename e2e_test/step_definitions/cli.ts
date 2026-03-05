import { Given, When, Then } from '@badeball/cypress-cucumber-preprocessor'

const BASE_URL = 'http://localhost:9081'

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
    cy.exec(doughnutPath, { timeout: 5000 }).its('stdout').as('doughnutOutput')
  })
})

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
