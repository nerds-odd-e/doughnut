/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check
import {
  type DataTable,
  Given,
  Then,
  When,
} from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'

Given(
  'I navigate to the {string} section in the admin dashboard',
  (tabName: string) => {
    return start.goToAdminDashboard().goToTabInAdminDashboard(tabName)
  }
)

Then('I should see the message {string}', (message: string) => {
  cy.contains(message)
})

When('I choose model {string} for {string}', (model: string, task: string) => {
  return start
    .goToAdminDashboard()
    .goToModelManagement()
    .chooseModel(model, task)
})

When('I remove the notebook {string} from the bazaar', (notebook: string) => {
  return start
    .goToAdminDashboard()
    .goToBazaarManagement()
    .removeFromBazaar(notebook)
})

When('I approve notebook {string} to become certified', (notebook: string) => {
  return start
    .goToAdminDashboard()
    .goToCertificationRequestPage()
    .approve(notebook)
})

Then(
  'I should see following notebooks waiting for approval only:',
  (notebooks: DataTable) => {
    return start
      .goToAdminDashboard()
      .goToCertificationRequestPage()
      .listContainsExactly(notebooks.raw().map((row) => row[0]!))
  }
)
