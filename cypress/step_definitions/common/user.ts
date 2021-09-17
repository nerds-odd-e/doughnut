/// <reference types="cypress" />
/// <reference types="@testing-library/cypress" />
/// <reference types="../../support" />
// @ts-check

import {
  And,
  Before,
  Given,
  Then,
  When,
} from "cypress-cucumber-preprocessor/steps";

Given("I've logged in as {string}", (externalIdentifier) => {
  if (externalIdentifier === "none") {
    return;
  }
  cy.loginAs(externalIdentifier);
});

Given("I've logged in as an existing user", () => {
  cy.loginAs("old_learner");
});

Given("I've logged in as another existing user", () => {
  cy.loginAs("another_old_learner");
});

Given("my session is logged out", () => {
  cy.pageIsLoaded();
  cy.logout();
});

Given("I'm on the login page", () => {
  cy.visit("/login");
});

When("I identify myself as a new user", () => {
  cy.get("#username").type("user");
  cy.get("#password").type("password");
  cy.get("form.form-signin").submit();
});

When("I should be asked to create my profile", () => {
  cy.get("body").should("contain", "Please create your profile");
});

When("I save my profile with:", (data) => {
  data.hashes().forEach((elem) => {
    for (var propName in elem) {
      cy.getFormControl(propName).type(elem[propName]);
    }
  });
  cy.get('input[value="Submit"]').click();
});

Then("I should see {string} in the page", (content) => {
  cy.get("body").should("contain", content);
});

Then("My name {string} is in the top bar", (name) => {
  cy.get("nav").should("contain", name);
});

Then("my daily new notes to review is set to {int}", (number) => {
  cy.updateCurrentUserSettingsWith({ daily_new_notes_count: number });
});

Then("my space setting is {string}", (number) => {
  cy.updateCurrentUserSettingsWith({ space_intervals: number });
});

Then("I haven't login", () => {});

When("I visit {string} page", (pageName) => {
  switch (pageName) {
    case "FailureReportPage":
      cy.visit("/failure-report-list", {
        failOnStatusCode: false,
      });
      break;
    default:
      cy.failure();
  }
});

Then("The {string} page is displayed", (pageName) => {
  switch (pageName) {
    case "LoginPage":
      cy.findAllByText("Please sign in");
      break;
    case "FailureReportPage":
      cy.findAllByText("Failure report list");
      break;
    case "ErrorPage":
      cy.findAllByText("It seems you cannot access this page.");
      break;
    default:
      cy.failure();
  }
});

Then("I should be asked to log in again.", () => {});

Then(
  "when I login as {string} I should see {string}",
  (username, expectation) => {
    cy.get("#username").type(username);
    cy.get("#password").type("password");
    cy.get("form.form-signin").submit();
    cy.findByText(expectation, { selector: "[role=title]" }).should("be.visible");
  },
);

Then("I edit user profile", () => {
  cy.visit("/");
  cy.get(".user-profile-link").click();
});

Then("I change my name to {string}", (name) => {
  cy.getFormControl("Name").clear().type(name);
  cy.findByText("Submit").click();
});
