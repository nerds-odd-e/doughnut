/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check
import { Then, When } from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'

When('I open the bazaar admin list', () => {
  return start.assumeAdminDashboardPage().openBazaarAdminList()
})

When('I choose model {string} for {string}', (model: string, task: string) => {
  return start
    .assumeAdminDashboardPage()
    .goToModelManagement()
    .chooseModel(model, task)
})

Then(
  'the model for {string} should be {string}',
  (task: string, model: string) => {
    start.form.getField(task).shouldHaveValue(model)
  }
)

Then('the bazaar admin list shows {string}', (notebooks: string) => {
  return start.assumeAdminDashboardPage().expectBazaarAdminNotebooks(notebooks)
})

When('I remove {string} from the bazaar admin list', (notebook: string) => {
  return start
    .assumeAdminDashboardPage()
    .removeNotebookFromBazaarAdminList(notebook)
})

When('I open the failure reports', () => {
  start.assumeAdminDashboardPage().openFailureReports()
})

Then('the failure reports access outcome is {string}', (outcome: string) => {
  start.assumeAdminDashboardPage().expectFailureReportsAccessOutcome(outcome)
})
