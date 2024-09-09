/// <reference types="cypress" />
/// <reference types="@testing-library/cypress" />
/// <reference types="../support" />
// @ts-check

import { Given, Then, When } from '@badeball/cypress-cucumber-preprocessor'
import { DataTable } from '@cucumber/cucumber'
import start from '../start'

Given('I am logged in as {string}', (externalIdentifier: string) => {
  start.loginAs(externalIdentifier)
})

Given('I am re-logged in as {string}', (externalIdentifier: string) => {
  start.reloginAs(externalIdentifier)
})

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
  // I regret to put an if statement here.
  // The login page is different when the frontend is built into the backend.
  // Use the nonproduction login page when testing with standalone frontend server
  const baseUrl = Cypress.config('baseUrl')
  if (baseUrl && baseUrl.indexOf('5173') > -1) {
    cy.visit('/users/identify')
    return
  }
  // use the backend login page when testing with frontend built into the backend
  cy.visit('/login')
})

When('I identify myself as a new user', () => {
  cy.get('#username').type('user')
  cy.get('#password').type('password')
  cy.get('form.form-signin').submit()
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
  start.systemSidebar().userOptions().userSettings(name)
})

Then('my daily new notes to review is set to {int}', (number: string) => {
  start
    .testability()
    .updateCurrentUserSettingsWith({ daily_new_notes_count: number })
})

Then('my space setting is {string}', (number: string) => {
  start.testability().updateCurrentUserSettingsWith({ space_intervals: number })
})

Then("I haven't login", () => {
  start.logout()
})

When('I visit the falure reports on the admin page', () => {
  cy.visit('/admin-dashboard')
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
    cy.get('form.form-signin').submit()
    start.assumeNotePage(expectation)
  }
)

Then('I edit user profile to change my name to {string}', (name: string) => {
  start
    .systemSidebar()
    .userOptions()
    .userSettings('Old Learner')
    .changeName(name)
})

Then('I logout via the UI', () => {
  cy.visit('/')
  start.systemSidebar().userOptions().logout()
})

Then('I should be on the welcome page and asked to login', () => {
  cy.contains('Welcome')
  cy.findByRole('button', { name: 'Login via Github' }).click()
})

Then('I opt to do only AI generated questions', () => {
  start
    .systemSidebar()
    .userOptions()
    .userSettings('Old Learner')
    .aiQuestionOnlyForReview()
})
