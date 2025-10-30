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

When('I delete the MCP token with label {string}', (label: string) => {
  start
    .mainMenu()
    .userOptions()
    .manageMCPTokens()
    .deleteToken(label)
    .checkTokenWithLabelNotExists(label)
})

Then(
  'I cannot create a note as a child of {string} using the MCP token',
  (parentNote: string) => {
    start
      .mcpApi()
      .createNote(parentNote, { newTitle: 'Child Note', wikidataId: 'Q214665' })
      .shouldBeDenied()
  }
)

When(
  'I can create a note as a child of {string} using the MCP token',
  (parentNote: string) => {
    start
      .mcpApi()
      .createNote(parentNote, { newTitle: 'Child Note', wikidataId: 'Q214666' })
      .shouldBeAccepted()
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

Then('the token is marked as valid', () => {
  start
    .mainMenu()
    .userOptions()
    .manageMCPTokens()
    .checkTokenWithLabelExists('Valid')
})

Then(
  'The last used token timestamp should show {string}',
  (timestamp: string) => {
    start
      .mainMenu()
      .userOptions()
      .manageMCPTokens()
      .checkLastUsedTokenTimestamp(timestamp)
  }
)

Then(
  'the last used token timestamp should show {string}',
  (timestamp: string) => {
    start
      .mainMenu()
      .userOptions()
      .manageMCPTokens()
      .checkLastUsedTokenTimestamp(timestamp)
  }
)

When(
  'I create a note as a child of {string} using the MCP token',
  (parentNote: string) => {
    start
      .mcpApi()
      .createNote(parentNote, { newTitle: 'Child Note', wikidataId: 'Q214667' })
      .shouldBeAccepted()
  }
)

Given(
  'I have an MCP token with expiration date {string}',
  (dateString: string) => {
    const now = new Date(Date.now()) // Let us return to now afterwards

    // travel back to set token with desired expiration date
    const oneMonthBeforeExpDate = new Date(dateString)
    oneMonthBeforeExpDate.setMonth(oneMonthBeforeExpDate.getMonth() - 1) // Move 1 month back
    start.testability().backendTimeTravelToDate(oneMonthBeforeExpDate)

    start
      .mainMenu()
      .userOptions()
      .manageMCPTokens()
      .generateToken('Test-token-with-expiration')
      .as('generatedMcpToken')
    // travel back to now
    start.testability().backendTimeTravelToDate(now)
  }
)

Then('the token is marked as {string}', (status: string) => {
  start
    .mainMenu()
    .userOptions()
    .manageMCPTokens()
    .checkTokenWithLabelExists(status)
})
