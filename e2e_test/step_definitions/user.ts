/// <reference types="cypress" />
/// <reference types="@testing-library/cypress" />
/// <reference types="../support" />
// @ts-check

import { Given, Then, When } from "@badeball/cypress-cucumber-preprocessor"

Given("I am logged in as {string}", (externalIdentifier: string) => {
  if (externalIdentifier === "none") {
    return
  }
  cy.loginAs(externalIdentifier)
})

Given("I am logged in as an existing user", () => {
  cy.loginAs("old_learner")
})

Given("I am logged in as another existing user", () => {
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
      cy.formField(propName).assignFieldValue(elem[propName])
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
      cy.visit("/admin-dashboard")
      cy.findByRole("button", { name: "Failure Reports" }).click()
      break
    default:
      cy.failure()
  }
})

Then("The {string} page is displayed", (pageName) => {
  switch (pageName) {
    case "LoginPage":
      cy.contains("Please sign in")
      break
    case "FailureReportPage":
      cy.findAllByText("Failure report list")
      break
    case "ErrorPage":
      cy.findAllByText("It seems you cannot access this page.")
      cy.dismissLastErrorMessage()
      break
    default:
      cy.failure()
  }
})

Then("I login as {string} I should see {string}", (username: string, expectation: string) => {
  cy.get("#username").type(username)
  cy.get("#password").type("password")
  cy.get("form.form-signin").submit()
  cy.findNoteTopic(expectation)
})

Then("I edit user profile", () => {
  cy.visit("/")
  cy.openSidebar()
  cy.findUserSettingsButton("Old Learner").click()
})

Then("I change my name to {string}", (name: string) => {
  cy.formField("Name").assignFieldValue(name)
  cy.findByText("Submit").click()
})

Then("I logout via the UI", () => {
  cy.visit("/")
  cy.openSidebar()
  cy.findByRole("button", { name: "User actions" }).click()
  cy.findByRole("button", { name: "Logout" }).click()
})

Then("I should be on the welcome page and asked to login", () => {
  cy.contains("Welcome")
  cy.findByRole("button", { name: "Login via Github" }).click()
})

Then("I opt to do only AI generated questions", () => {
  cy.visit("/")
  cy.findUserSettingsButton("Old Learner").click()
  cy.formField("Ai Question Type Only For Review").check()
  cy.findByText("Submit").click()
})
