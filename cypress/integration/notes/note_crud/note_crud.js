import {
  Given,
  And,
  Then,
  When,
  Before
} from "cypress-cucumber-preprocessor/steps";

When("I create top level note with:", data => {
  cy.visitMyNotebooks();
  cy.findByText("Add New Notebook").click();
  cy.submitNoteFormWith(data.hashes());
});

When(
  "I should see that the note creation is not successful",
  (noteTitle, data) => {
    cy.findByText("size must be between 1 and 100");
  }
);

Then(
  "I should not see note {string} at the top level of all my notes",
  noteTitle => {
    cy.visitMyNotebooks();
    cy.findByText("Notebooks");
    cy.findByText(noteTitle).should("not.exist");
  }
);

Then("I should see {string} in note title", noteTitle => {
  cy.findByText(noteTitle, { selector: ".h1" });
});

Then("I should see these notes belonging to the user", data => {
  cy.expectNoteCards(data.hashes());
});

When("I am creating note under {string}", noteTitles => {
  cy.navigateToNotePage(noteTitles);
  cy.findByText("(Add Child Note)").click();
});

When("I create a sibling note of {string}:", (noteTitle, data) => {
  cy.findByText(noteTitle, { selector: ".h1" });
  cy.findByText("Add Sibling Note").click();
  cy.submitNoteFormWith(data.hashes());
});

When(
  "I am editing note {string} the title is expected to be pre-filled with {string}",
  (noteTitle, oldTitle) => {
    cy.clickNotePageButton(noteTitle, ".edit-card");
    cy.getFormControl("Title").should("have.value", oldTitle);
  }
);

When("I update it to become:", data => {
  cy.submitNoteFormWith(data.hashes());
});

When("I delete top level note {string}", noteTitle => {
  cy.clickNotePageButton(noteTitle, ".delete-card");
});
