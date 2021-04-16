import {
  Given,
  And,
  Then,
  When,
  Before,
} from "cypress-cucumber-preprocessor/steps";

Given("I've logged in as {string}", (externalIdentifier) => {
  cy.loginAs(externalIdentifier);
});

When("I open the {string} set address bar", (url) => {
  cy.visit('http://localhost:8081/' + url)
});

When("Someone open the {string} set address bar", (url) => {
  cy.visit('http://localhost:8081/' + url, {failOnStatusCode: false})
});

Then("there shouldn't be any note edit button for {string}", (noteTitle) => {
  cy.findNoteCardEditButton(noteTitle).should("not.exist");
});

Given("I've failure report", () => {
  cy.seedFailureReport();
});

When("I open the {string} set address bar", (url) => {
  cy.visit('http://localhost:8081/' + url)
});

Given("I've logged in as an existing user", () => {
  cy.loginAs('developer');
});

Given("I've logged in as another existing user", () => {
  cy.loginAs('another_old_learner');
});

When("I identify myself as a new user", () => {
  cy.visit("/login");

  cy.get("#username").type("user");
  cy.get("#password").type("password");
  cy.get("form.form-signin").submit();
  cy.location("pathname", { timeout: 10000 }).should("eq", "/");
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

Then("I should not see failure report in the page", () => {
  cy.get(".report-list").children().should('have.length', 0);
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

When("I open the Github issue set address bar", () => {
    cy.visit('http://localhost:8081/testability/issue');
});

Then("I should see Exception in Github issue", () => {
    cy.get("body").should("contain", "Github");
});

When("I click the Doughnut Failure Report link in Github issue", () => {
    cy.visit('http://localhost:8081/failure-report-list/show/1');
});

Then("I should see Exception in the page", () => {
    cy.get("body").should("contain", "RuntimeException");
});

And("I should see issue url {string}", (id) => {
    cy.get(".issue_link").should("have.text", "https://github.com/nerds-odd-e/doughnut_sandbox/issues/" + id);
});

When("I visit failure-report {string} page", (id) => {
    cy.visit('http://localhost:8081/failure-report-list/show/' + id);
});
