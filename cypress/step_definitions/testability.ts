/// <reference types="cypress" />
/// <reference types="@testing-library/cypress" />
/// <reference types="../support" />
// @ts-check

import { Given, Then, When } from "@badeball/cypress-cucumber-preprocessor"

When("Someone triggered an exception", () => {
  cy.testability().triggerException()
})

Then("I should see {string} in the failure report", (content) => {
  cy.visit("/failure-report-list")
  cy.get("body").should("contain", content)
})

When("I should see a new open issue on github", () => {
  cy.request({ method: "GET", url: `/api/testability/github_issues` }).then((response) => {
    expect(response.body.length).to.equal(1)
  })
})

Given("Use real github sandbox and there are no open issues on github", () => {
  cy.request({
    method: "POST",
    url: `/api/testability/use_real_sandbox_github_and_close_all_github_issues`,
  })
    .its("body")
    .should("contain", "OK")
})

Then("The {string} alert {string}", (expectedContent, shouldExistOrNot) => {
  cy.visit("/")
  cy.contains("Welcome")
  cy.contains(expectedContent).should(shouldExistOrNot === "should exist" ? "exist" : "not.exist")
})

Then("I go to the testability page to turn on the feature toggle", () => {
  cy.findByText("Testability").click()
  cy.getFormControl("Feature Toggle").click()
})
