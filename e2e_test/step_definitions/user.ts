/// <reference types="cypress" />
/// <reference types="@testing-library/cypress" />
/// <reference types="../support" />
// @ts-check

import { Given, Then, When } from '@badeball/cypress-cucumber-preprocessor'
import type { DataTable } from '@cucumber/cucumber'
import start from '../start'

Given('I am logged in as {string}', (externalIdentifier: string) => {
  start.loginAs(externalIdentifier)
})

Given('I am re-logged in as {string}', (externalIdentifier: string) => {
  start.reloginAs(externalIdentifier)
})

Given(
  'I am re-logged in as {string} and reload the page',
  (externalIdentifier: string) => {
    start.reloginAndEnsureHomePage(externalIdentifier)
  }
)

Given('I am logged in as an existing user', () => {
  start.loginAs('old_learner')
})

Given('I am logged in as an admin', (_tabName: string) => {
  start.loginAsAdmin()
})

Given('I am re-logged in as an admin', (_tabName: string) => {
  start.reloginAsAdmin()
})

Given('my session is logged out', () => {
  cy.pageIsNotLoading()
  start.logout()
})

Given("I'm on the login page", () => {
  cy.visit('/users/identify')
})

Given('I have a MCP Token', () => {
  start
    .mainMenu()
    .userOptions()
    .generateMcpToken()
    .then((token) => {
      cy.wrap(token).as('savedTokenValue')
    })
})

When('I identify myself as a new user', () => {
  cy.get('#username').type('user')
  cy.get('#password').type('password')
  cy.get('form').submit()
})

When('I should be asked to create my profile', () => {
  cy.get('body').should('contain', 'Please create your profile')
})

When('I save my profile with:', (data: DataTable) => {
  data.hashes().forEach((elem) => {
    for (const propName in elem) {
      cy.formField(propName).assignFieldValue(elem[propName] ?? '')
    }
  })
  cy.get('input[value="Submit"]').click()
})

When('I generate a MCP Token', () => {
  start.mainMenu().userOptions().generateMcpToken()
})

When('I delete MCP Token', () => {
  cy.findByRole('button', { name: 'Delete' }).click()
})

When('I delete MCP Token and I reload page', () => {
  cy.findByRole('button', { name: 'Delete' }).click()
  cy.reload()
})

Then('I should see {string} in the page', (content) => {
  cy.get('body').should('contain', content)
})

Then('My name {string} is in the user action menu', (name: string) => {
  start.mainMenu().userOptions().userSettings(name)
})

Then('my daily new notes to review is set to {int}', (number: string) => {
  start
    .testability()
    .updateCurrentUserSettingsWith({ daily_assimilation_count: number })
})

Then('my space setting is {string}', (number: string) => {
  start.testability().updateCurrentUserSettingsWith({ space_intervals: number })
})

Then("I haven't login", () => {
  start.logout()
})

Then('I should see empty MCP Token', () => {
  cy.findByTestId('mcp-token').should('have.value', '')
})

When('I visit the falure reports on the admin page', () => {
  cy.visit('/d/admin-dashboard')
  cy.findByRole('button', { name: 'Failure Reports' }).click()

  // prevent the test from failing due to uncaught exceptions
  cy.on('uncaught:exception', () => {
    return false
  })
})

Then('The {string} page is displayed', (pageName) => {
  switch (pageName) {
    case 'LoginPage':
      cy.contains('Please sign in')
      break
    case 'FailureReportPage':
      cy.findAllByText('Failure report list')
      break
    case 'ErrorPage':
      cy.findAllByText('It seems you cannot access this page.')
      break
    default:
      cy.failure()
  }
})

Then(
  'I login as {string} I should see {string}',
  (username: string, expectation: string) => {
    cy.get('#username').type(username)
    cy.get('#password').type('password')
    cy.get('form').submit()
    start.assumeNotePage(expectation)
  }
)

Then('I edit user profile to change my name to {string}', (name: string) => {
  start.mainMenu().userOptions().userSettings('Old Learner').changeName(name)
})

Then('I logout via the UI', () => {
  cy.visit('/')
  start.mainMenu().userOptions().logout()
})

Then('I should be on the welcome page and asked to login', () => {
  cy.contains('Welcome')
  cy.findByRole('button', { name: 'Login via Github' }).click()
})

Then('I should see generated Token', () => {
  const uuidPattern =
    /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/
  cy.get('input[data-testid="mcp-token"]')
    .should('be.visible')
    .invoke('val')
    .should('match', uuidPattern)
})

Then('I should see MCP token', () => {
  cy.get('input[data-testid="mcp-token"]')
    .should('be.visible')
    .should('have.value')
})
