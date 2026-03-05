import { Given, When, Then } from '@badeball/cypress-cucumber-preprocessor'

const BASE_URL = 'http://localhost:9081'

Given('the backend is serving the CLI and install script', () => {
  // CLI is bundled and copied to static by Before hook with @cli tag
  cy.request('GET', `${BASE_URL}/install`).its('status').should('eq', 200)
})

When('I install the CLI from localhost without affecting my system', () => {
  cy.task<string>('installCli', BASE_URL).as('doughnutPath')
})

When('I run the installed doughnut command', () => {
  cy.get<string>('@doughnutPath').then((doughnutPath) => {
    cy.exec(doughnutPath, { timeout: 5000 }).its('stdout').as('doughnutOutput')
  })
})

Then('I should see {string}', (expected: string) => {
  cy.get('@doughnutOutput').should('include', expected)
})
