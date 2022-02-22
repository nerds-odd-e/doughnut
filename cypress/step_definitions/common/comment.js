

import {
  And,
  Before,
  Given,
  Then,
  When,
} from "cypress-cucumber-preprocessor/steps";

And('I click the add comment button', () => {
  cy.findByText("Add Comment").click();
})