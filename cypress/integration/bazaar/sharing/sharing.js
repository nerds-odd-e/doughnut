import {
  Given,
  And,
  Then,
  When,
  Before
} from "cypress-cucumber-preprocessor/steps";

Given("I choose to share my note {string}", noteTitle => {
  cy.visitMyNotebooks();
  cy.findNoteCardButton(noteTitle, ".share-card").click();
});
