import {
  Given,
  And,
  Then,
  When,
  Before
} from "cypress-cucumber-preprocessor/steps";

When("I click {string} in article view", noteTitle => {
  cy.findByText(noteTitle).click();
});
