import {
  Given,
  And,
  Then,
  When,
  Before,
  Background
} from "cypress-cucumber-preprocessor/steps";

When("I am creating link for note {string}", (noteTitle) => {
    cy.creatingLinkFor(noteTitle);
});

When("I link to note {string}", (noteTitle) => {
    cy.clickButtonOnCard(noteTitle, "Select");
})

When("I link note {string} to note {string}", (fromNoteTitle, toNoteTitle) => {
    cy.creatingLinkFor(fromNoteTitle);
    cy.clickButtonOnCard(toNoteTitle, "Select");
})

And("I should see the source note as {string}",(noteTitle) => {
    cy.findByText(`Link ${noteTitle} to:`).should("be.visible");
})

When("I search for notes with title {string}", (searchKey) => {
    cy.findByPlaceholderText("Search").type(searchKey);
    cy.findByText("Search").click();
})

And("I should see {string} as targets only",(noteTitlesAsString) => {
    cy.expectExactLinkTargets(noteTitlesAsString.split(",").map(i=>i.trim()));
})

