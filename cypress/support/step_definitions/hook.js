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

