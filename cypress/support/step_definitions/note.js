import {
  Given,
  And,
  Then,
  When,
  Before,
} from "cypress-cucumber-preprocessor/steps";

Given("there are some notes for the current user", (data) => {
  cy.seedNotes(data.hashes());
})

When("I create note with:", (data) => {
  cy.createNotes(data.hashes());
});

Then("Reviews should include single note pages:", (data) => {
  let examples = data.rowsHash();
  let matched = new Set();
  cy.recursiveLookUpInReview(
      5,
      () =>cy.get('.single-note-review #note-title').invoke("text"),
      (history, currentNoteTitle, done) => {
           if(currentNoteTitle in examples) {
                matched.add(currentNoteTitle);
                cy.get('#note-description').should("contain", examples[currentNoteTitle]);
                if(matched.size == Object.keys(examples).length) done();
           }
      }
  );
})

And("Reviews should include related notes page from {string} to {string}",(noteTitle1, noteTitle2) => {
    cy.findByText(noteTitle1).should("be.visible");
    cy.findByText(noteTitle2).should("be.visible");
})
