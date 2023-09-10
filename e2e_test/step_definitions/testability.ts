/// <reference types="cypress" />
/// <reference types="@testing-library/cypress" />
/// <reference types="../support" />
// @ts-check

import { Given, Then, When } from "@badeball/cypress-cucumber-preprocessor"
import TestabilityHelper from "../support/TestabilityHelper"
import pageObjects from 'page_objects';

When("Someone triggered an exception", () => {
  cy.testability().triggerException()
})

Then("an admin should see {string} in the failure report", (content: string) => {
  pageObjects
  .loginAsAdminAndGoToAdminDashboard()
  .goToFailureReportList()
  .shouldContain(content)
})

When("I should see a new open issue on github", () => {
  cy.testability().then((testability: TestabilityHelper) => {
    testability.getTestabilityApiSuccessfully(cy, "github_issues").then((response) => {
      expect(response.body.length).to.equal(1)
    })
  })
})

Given("Use real github sandbox and there are no open issues on github", () => {
  cy.testability().then((testability: TestabilityHelper) =>
    testability.postToTestabilityApiSuccessfully(
      cy,
      "use_real_sandbox_github_and_close_all_github_issues",
      {},
    ),
  )
})

Then("The {string} alert {string}", (expectedContent: string, shouldExistOrNot: string) => {
  cy.visit("/")
  cy.contains("Welcome")
  cy.contains(expectedContent).should(shouldExistOrNot === "should exist" ? "exist" : "not.exist")
})

Then("I go to the testability page to turn on the feature toggle", () => {
  cy.findByText("Testability").click()
  cy.formField("Feature Toggle").click()
})
