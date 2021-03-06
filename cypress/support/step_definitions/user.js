import {
  Given,
  And,
  Then,
  When,
  Before,
} from "cypress-cucumber-preprocessor/steps";

Given("I've logged in as an existing user", () => {
  cy.loginAs('old_learner');
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
      cy.get("#" + propName).type(elem[propName]);
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

Then("I haven't login", () => {
});






