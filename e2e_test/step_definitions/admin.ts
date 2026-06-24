/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check
import { Then, When } from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'

When(
  'I navigate to the {string} section in the admin dashboard',
  (tabName: string) => {
    return start.assumeAdminDashboardPage().openAdminDashboardTab(tabName)
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

Then('I should see {string} in the bazaar admin list', (notebooks: string) => {
  return start.assumeAdminDashboardPage().expectBazaarAdminNotebooks(notebooks)
})

When(
  'I remove the notebook {string} from the bazaar admin list',
  (notebook: string) => {
    return start
      .assumeAdminDashboardPage()
      .removeNotebookFromBazaarAdminList(notebook)
  }
)

When('I run the admin data migration to completion', () => {
  start.testability().resetAdminDataMigrationProgress()
  return start
    .goToAdminDashboard()
    .openAdminDashboardTab('Data migration')
    .runDataMigrationToCompletion()
})
