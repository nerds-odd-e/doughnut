/// <reference types="cypress" />
/// <reference types="@testing-library/cypress" />
/// <reference types="../support" />
// @ts-check

import { Then, When } from "@badeball/cypress-cucumber-preprocessor"
import start from "../start"

When("Someone triggered an exception", () => {
  start.testability().triggerException()
})

Then(
  "an admin should see {string} in the failure report",
  (content: string) => {
    start
      .loginAsAdminAndGoToAdminDashboard()
      .goToFailureReportList()
      .shouldContain(content)
  },
)

Then(
  "The {string} alert {string}",
  (expectedContent: string, shouldExistOrNot: string) => {
    cy.visit("/")
    cy.contains("Welcome")
    cy.contains(expectedContent).should(
      shouldExistOrNot === "should exist" ? "exist" : "not.exist",
    )
  },
)

Then("I go to the testability page to turn on the feature toggle", () => {
  cy.findByText("Testability").click()
  cy.formField("Feature Toggle").click()
})
