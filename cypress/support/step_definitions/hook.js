import {
    Given,
    And,
    Then,
    When,
    Before,
  } from "cypress-cucumber-preprocessor/steps";

Before({ tags: "@clean_db" }, () => {
  cy.cleanDBAndSeedData();
});

Before({ tags: "@login_as_existing_user1" }, () => {
  cy.loginAs('old_learner');
});

