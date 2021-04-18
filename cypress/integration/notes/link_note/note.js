import {
  Given,
  And,
  Then,
  When,
  Before
} from "cypress-cucumber-preprocessor/steps";

// This step definition is for demo purpose
Then(
  "*for demo* I should see there are {int} descendants",
  numberOfDescendants => {
    cy.findByText("" + numberOfDescendants, {
      selector: ".descendant-counter"
    });
  }
);
