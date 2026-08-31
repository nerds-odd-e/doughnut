/// <reference types="cypress" />
/// <reference types="@testing-library/cypress" />
/// <reference types="../support" />
// @ts-check

import { Given, Then, When } from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'

When('Someone triggered an exception', () => {
  start.testability().triggerException()
})

Then(
  'an admin should see {string} in the failure report',
  (content: string) => {
    start
      .loginAsAdminAndGoToAdminDashboard()
      .goToFailureReportList()
      .shouldContain(content)
  }
)

Then(
  'an admin should see one {string} in the failure report with occurrence count {int}',
  (content: string, count: number) => {
    start
      .loginAsAdminAndGoToAdminDashboard()
      .goToFailureReportList()
      .shouldHaveOneEntryContaining(content)
      .shouldHaveOccurrenceCount(count)
  }
)

Given('an admin is viewing the failure report', () => {
  return start.establishSessionAs('admin').then(() => {
    start
      .assumeAdminDashboardPage()
      .openFailureReports()
      .assumeFailureReportList()
  })
})

When('I clear the selected failure report item', () => {
  start.assumeAdminDashboardPage().assumeFailureReportList().clearSelected()
})

Then('the failure report should be empty', () => {
  start.assumeAdminDashboardPage().assumeFailureReportList().shouldBeEmpty()
})

When('I visit the home page', () => {
  start.visitHomePage()
})

Then('I should see the unfinished feature indicator', () => {
  start.assumeHomePage().expectUnfinishedFeatureIndicator()
})

Then('I should not see the unfinished feature indicator', () => {
  start.assumeHomePage().expectNoUnfinishedFeatureIndicator()
})

When('I turn on the feature toggle', () => {
  start.assumeHomePage().turnOnFeatureToggle()
})
