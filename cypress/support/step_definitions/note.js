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

When("I create top level note with:", (data) => {
  cy.visit("/notes");
  cy.findByText("Add Top Level Note").click();
  cy.submitNoteFormWith(data.hashes());
});

When("I edit note {string} to become:", (noteTitle, data) => {
  cy.visit("/notes");
  cy.findNoteCardButton(noteTitle, ".edit-card").click();
  cy.submitNoteFormWith(data.hashes());
});

When("I create note belonging to {string}:", (noteTitle, data) => {
  cy.visit("/notes");
  cy.findByText(noteTitle).click();
  cy.findByText("(Add Child Note)").click();
  cy.submitNoteFormWith(data.hashes());
});


When("I am creating note under {string}", (noteTitles, data) => {
  cy.visit("/notes");
  noteTitles.split(" > ").forEach(noteTitle => cy.findByText(noteTitle).click());
  cy.findByText("(Add Child Note)").click();
});

When("I should see {string} in breadcrumb", (noteTitles, data) => {
  cy.get('.breadcrumb').within( ()=>
      noteTitles.split(", ").forEach(noteTitle => cy.findByText(noteTitle ))
  )
});

Then("I should see these notes belonging to the user at the top level of all my notes", (data) => {
    cy.visit("/notes");
    cy.expectNoteCards(data.hashes());
});

Then("I should see these notes belonging to the user", (data) => {
    cy.expectNoteCards(data.hashes());
});

When("I delete top level note {string}", (noteTitle) => {
  cy.visit("/notes");
  cy.findNoteCardButton(noteTitle, ".delete-card").click();
});


When("I create a sibling note of {string}:", (noteTitle, data) => {
  cy.findByText(noteTitle, {selector: ".display-4"});
  cy.findByText("Add Sibling Note").click();
  cy.submitNoteFormWith(data.hashes());
});

When("I should see that the note creation is not successful", (noteTitle, data) => {
  cy.findByText("size must be between 1 and 100");
});

Then("I should see {string} in note title", (noteTitle) => {
    cy.findByText(noteTitle, {selector: '.display-4'});
});

Then("I should not see note {string} at the top level of all my notes", (noteTitle) => {
    cy.visit("/notes");
    cy.findByText("Top Level Notes");
    cy.findByText(noteTitle).should('not.exist');
});

When("I open {string} note at top level", (noteTitles) => {
    cy.visit("/notes");
    noteTitles.split("/").forEach(noteTitle =>
      cy.findByText(noteTitle).click()
    );
});

When("I should not be able to go to next sibling", () => {
    cy.findByText("Top");
});

When("I should be able to go to next note {string}", (noteTitle) => {
    cy.findByText("Top");
});




