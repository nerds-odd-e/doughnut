import {
    Given,
    And,
    Then,
    When,
    Before,
  } from "cypress-cucumber-preprocessor/steps";

Before(() => {
  cy.cleanDBAndSeedData();
});

