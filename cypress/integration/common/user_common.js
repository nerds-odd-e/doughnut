import {
  Given,
  And,
  Then,
  When,
  Before
} from "cypress-cucumber-preprocessor/steps";

Given("I've logged in as {string}", externalIdentifier => {
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

Then("my daily new notes to review is set to {int}", number => {
  cy.updateCurrentUserSettingsWith({ daily_new_notes_count: number });
});

Then("I haven't login", () => {});
