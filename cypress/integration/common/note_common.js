import {
  Given,
  And,
  Then,
  When,
  Before
} from "cypress-cucumber-preprocessor/steps";

Given("there are some notes for the current user", data => {
  cy.seedNotes(data.hashes());
});

Given(
  "there are some notes for existing user {string}",
  (externalIdentifier, data) => {
    cy.seedNotes(data.hashes(), externalIdentifier);
  }
);

Given("there are notes from Note {int} to Note {int}", (from, to) => {
  const notes = Array(to - from + 1)
    .fill(0)
    .map((_, i) => {
      return { title: `Note ${i + from}` };
    });
  cy.seedNotes(notes);
});

Then("I should see {string} in breadcrumb", noteTitles => {
  cy.get(".breadcrumb").within(() =>
    noteTitles
      .commonSenseSplit(", ")
      .forEach(noteTitle => cy.findByText(noteTitle))
  );
});

When(
  "I should be able to go to the {string} note {string}",
  (button, noteTitle) => {
    cy.findByRole("button", { name: button }).click();
    cy.get(".jumbotron").within(() => cy.findByText(noteTitle).should("exist"));
  }
);

Then(
  "I should see these notes belonging to the user at the top level of all my notes",
  data => {
    cy.visitMyNotebooks();
    cy.expectNoteCards(data.hashes());
  }
);

When("I open {string} note from top level", noteTitles => {
  cy.navigateToNotePage(noteTitles);
});

When("I should see the screenshot matches", () => {
  // cy.get('.content').toMatchImageSnapshot({ imageConfig: { threshold: 0.001, }, });
});

When("I open the note {string} in my notes in article view", noteTitle => {
  cy.visitMyNotebooks();
  cy.findByRole("button", { name: "Article View" }).click();
});

When("I create note belonging to {string}:", (noteTitle, data) => {
  cy.jumpToNotePage(noteTitle);
  cy.findByText("(Add Child Note)").click();
  cy.submitNoteFormWith(data.hashes());
});
