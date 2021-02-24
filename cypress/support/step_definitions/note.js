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

Then("I should see these notes belonging to the user", (data) => {
    cy.visit("/all_my_notes");
    cy.findByText("Your Notes");
    data.hashes().forEach((elem) => {
         cy.findByText(elem["note-title"]).should("be.visible");
    });
});

When("I create note belonging to {string}:", (noteTitle, data) => {
  cy.createNotes(data.hashes(), noteTitle);
});

Then("I should not see note {string} at the top level of all my notes", (noteTitle) => {
    cy.visit("/all_my_notes");
    cy.findByText("Your Notes");
    cy.findByText(noteTitle).should('not.exist');
});

