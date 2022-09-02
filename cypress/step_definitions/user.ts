/// <reference types="cypress" />
/// <reference types="@testing-library/cypress" />
/// <reference types="../support" />
// @ts-check

import { Given, Then, When } from "@badeball/cypress-cucumber-preprocessor"

Given("I've logged in as {string}", (externalIdentifier: string) => {
  if (externalIdentifier === "none") {
    return
  }
  cy.loginAs(externalIdentifier)
})

Given("I've logged in as an existing user", () => {
  cy.loginAs("old_learner")
})

Given("I've logged in as another existing user", () => {
  cy.loginAs("another_old_learner")
})

Given("my session is logged out", () => {
  cy.pageIsNotLoading()
  cy.logout()
})

Given("I'm on the login page", () => {
  cy.visit("/login")
})

When("I identify myself as a new user", () => {
  cy.get("#username").type("user")
  cy.get("#password").type("password")
  cy.get("form.form-signin").submit()
})

When("I should be asked to create my profile", () => {
  cy.get("body").should("contain", "Please create your profile")
})

When("I save my profile with:", (data) => {
  data.hashes().forEach((elem) => {
    for (const propName in elem) {
      cy.getFormControl(propName).type(elem[propName])
    }
  })
  cy.get('input[value="Submit"]').click()
})

Then("I should see {string} in the page", (content) => {
  cy.get("body").should("contain", content)
})

Then("My name {string} is in the user action menu", (name: string) => {
  cy.findUserSettingsButton(name)
})

Then("my daily new notes to review is set to {int}", (number: string) => {
  cy.testability().updateCurrentUserSettingsWith({ daily_new_notes_count: number })
})

Then("my space setting is {string}", (number: string) => {
  cy.testability().updateCurrentUserSettingsWith({ space_intervals: number })
})

Then("I haven't login", () => {
  cy.log("I haven't login!!!")
})

When("I visit {string} page", (pageName) => {
  switch (pageName) {
    case "FailureReportPage":
      cy.visit("/failure-report-list", {
        failOnStatusCode: false,
      })
      break
    default:
      cy.failure()
  }
})

Then("The {string} page is displayed", (pageName) => {
  switch (pageName) {
    case "LoginPage":
      cy.findAllByText("Please sign in")
      break
    case "FailureReportPage":
      cy.findAllByText("Failure report list")
      break
    case "ErrorPage":
      cy.findAllByText("It seems you cannot access this page.")
      break
    default:
      cy.failure()
  }
})

Then("I login as {string} I should see {string}", (username: string, expectation: string) => {
  cy.get("#username").type(username)
  cy.get("#password").type("password")
  cy.get("form.form-signin").submit()
  cy.findNoteTitle(expectation)
})

Then("I edit user profile", () => {
  cy.visit("/")
  cy.findUserSettingsButton("Old Learner").click()
})

Then("I change my name to {string}", (name: string) => {
  cy.getFormControl("Name").clear().type(name)
  cy.findByText("Submit").click()
})

Then("I logout via the UI", () => {
  cy.visit("/")
  cy.findByRole("button", { name: "User actions" }).click()
  cy.findByRole("button", { name: "Logout" }).click()
})

Then("I should be on the welcome page and asked to login", () => {
  cy.contains("Welcome")
  cy.findByRole("button", { name: "Login via Github" }).click()
})
