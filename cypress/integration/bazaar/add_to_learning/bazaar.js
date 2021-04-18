import {
  Given,
  And,
  Then,
  When,
  Before
} from "cypress-cucumber-preprocessor/steps";

When("I go to the bazaar", () => {
  cy.visit("/bazaar");
});

When(
  "I subscribe to note {string} in the bazaar, with target of learning {int} notes per day",
  (noteTitle, count) => {
    cy.visit("/bazaar");
    cy.subscribeToNote(noteTitle, count);
  }
);

Then(
  "I should not see the {string} button on note {string}",
  (btnClass, noteTitle) => {
    cy.findNoteCardButton(noteTitle, "." + btnClass).should("not.exist");
  }
);

Then(
  "I should see the {string} button on note {string}",
  (btnClass, noteTitle) => {
    cy.findNoteCardButton(noteTitle, "." + btnClass).should("exist");
  }
);

Then("I should see readonly note {string} in my notes", noteTitle => {
  cy.visitMyNotebooks();
  cy.findNoteCardButton(noteTitle, ".edit-card").should("not.exist");
});

Then("I should see I've subscribed to {string}", noteTitle => {
  cy.findByText(noteTitle).should("be.visible");
});

When("I change notebook {string} to skip review", noteTitle => {
  cy.visitMyNotebooks();
  cy.findNoteCardButton(noteTitle, ".edit-notebook").click();
  cy.getFormControl("SkipReviewEntirely").check();
  cy.findByRole("button", { name: "Update" }).click();
});
