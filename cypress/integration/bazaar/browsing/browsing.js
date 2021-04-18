import {
  Given,
  And,
  Then,
  When,
  Before
} from "cypress-cucumber-preprocessor/steps";

When("I should not see {string} in the article", content => {
  cy.findByText(content).should("not.exist");
});

When("I should see two bullets in the article", () => {
  cy.get("body")
    .find("li.article-view")
    .should("have.length", 2);
});

When("I should see {string} as non-title in the article", content => {
  cy.findByText(content, { selector: ".note-body" });
});

Then("there shouldn't be any note edit button for {string}", noteTitle => {
  cy.findNoteCardEditButton(noteTitle).should("not.exist");
});

When("I open the note {string} in the Bazaar", noteTitle => {
  cy.findByText(noteTitle).click();
});
