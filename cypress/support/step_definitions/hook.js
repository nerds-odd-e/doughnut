import {
    Given,
    And,
    Then,
    When,
    Before,
  } from "cypress-cucumber-preprocessor/steps";

Before({ tags: "@clean_db" }, () => {
  cy.cleanDB();
});

Before({ tags: "@login_as_existing_user" }, () => {
  cy.loginAsExistingUser();
});

Before({ tags: "@login_as_new_user" }, () => {
  cy.loginAsNewUser();
});

Before({ tags: "@log_out" }, () => {
  cy.logOut();
});

Before({ tags: "@seed_notes" }, () => {
  cy.seedNotes();
});