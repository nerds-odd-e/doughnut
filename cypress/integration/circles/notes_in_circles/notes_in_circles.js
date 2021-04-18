import {
  Given,
  And,
  Then,
  When,
  Before
} from "cypress-cucumber-preprocessor/steps";

When(
  "I subscribe to note {string} in the circle {string}, with target of learning {int} notes per day",
  (noteTitle, circleName, count) => {
    cy.navigateToCircle(circleName);
    cy.subscribeToNote(noteTitle, count);
  }
);

When(
  "I should see the note {string} in circle {string}",
  (noteTitle, circleName) => {
    cy.navigateToCircle(circleName);
    cy.findByText(noteTitle).should("be.visible");
  }
);

When("I add a note {string} under {string}", (noteTitle, parentNoteTitle) => {
  cy.findByText(parentNoteTitle).click();
  cy.findByText("(Add Child Note)").click();
  cy.submitNoteFormWith([{ Title: noteTitle }]);
});
