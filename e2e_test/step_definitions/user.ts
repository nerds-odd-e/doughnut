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

When('I generate a new MCP token with label {string}', (label: string) => {
  start
    .mainMenu()
    .userOptions()
    .manageMCPTokens()
    .generateToken(label)
    .as('generatedMcpToken')
})

Then('the new MCP Token should be a valid UUID', () => {
  cy.get('@generatedMcpToken').should(
    'match',
    /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i
  )
})

Given('I have a valid MCP token with label {string}', (label: string) => {
  start
    .mainMenu()
    .userOptions()
    .manageMCPTokens()
    .generateToken(label)
    .then((token) => {
      cy.wrap(token).as('savedMcpToken')
    })
})

Given('the MCP token {string} has not been used', (label: string) => {
  start
    .mainMenu()
    .userOptions()
    .manageMCPTokens()
    .checkTokenWithLabelNotUsed(label)
})

When('I delete the MCP token with label {string}', (label: string) => {
  start
    .mainMenu()
    .userOptions()
    .manageMCPTokens()
    .deleteToken(label)
    .checkTokenWithLabelNotExists(label)
})

Then('I cannot create a note as a child of {string}', (parentNote: string) => {
  start
    .mcpApi()
    .createNote(parentNote, { newTitle: 'Child Note', wikidataId: 'Q214665' })
    .then((response) => {
      expect(response.status).to.eq(401)
    })
})

When(
  'I create a note as a child of {string} using the MCP',
  (parentNote: string) => {
    start
      .mcpApi()
      .createNote(parentNote, { newTitle: 'Child Note', wikidataId: 'Q214666' })
      .then((response) => {
        expect(response.status).to.eq(200)
      })
  }
)

Then(
  'I can see the token with label {string} in the list of tokens',
  (label: string) => {
    start
      .mainMenu()
      .userOptions()
      .manageMCPTokens()
      .checkTokenWithLabelExists(label)
  }
)

When('it is 90 days later', () => {
  start.testability().backendTimeTravelRelativeToNow(21610) // 90 days in seconds
})

Then(
  'the MCP token {string} should be marked as expired',
  (tokenLabel: string) => {
    cy.reload()
    cy.get('table').within(() => {
      cy.contains('tr', tokenLabel).within(() => {
        cy.contains('Expired')
      })
    })
  }
)
