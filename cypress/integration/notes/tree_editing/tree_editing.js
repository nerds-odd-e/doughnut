import {
  Given,
  And,
  Then,
  When,
  Before
} from "cypress-cucumber-preprocessor/steps";

When("I move note {string} left", noteTitle => {
  cy.jumpToNotePage(noteTitle);
  cy.findByText("Move This Note").click();
  cy.findByRole("button", { name: "Move Left" }).click();
});

When(
  "I should see {string} is before {string} in {string}",
  (noteTitle1, noteTitle2, parentNoteTitle) => {
    cy.jumpToNotePage(parentNoteTitle);
    var matcher = new RegExp(noteTitle1 + ".*" + noteTitle2, "g");

    cy.get(".card-title").then($els => {
      const texts = Array.from($els, el => el.innerText);
      expect(texts).to.match(matcher);
    });
  }
);

When("I move note {string} right", noteTitle => {
  cy.jumpToNotePage(noteTitle);
  cy.findByText("Move This Note").click();
  cy.findByRole("button", { name: "Move Right" }).click();
});
