import {
  Given,
  And,
  Then,
  When,
  Before,
} from "cypress-cucumber-preprocessor/steps";

When("I create a new circle {string} and copy the invitation code", (circleName) => {
  cy.visit("/circles/new");
  cy.get("#circle-name").type(circleName);
  cy.get('input[value="Submit"]').click();
});

When("I join the circle with the invitation code", () => {
});

When("I should see the circle {string} and it has two members in it", (circleName) => {
});













