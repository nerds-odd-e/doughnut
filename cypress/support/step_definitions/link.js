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

When("I link note {string} as {string} note {string}", (fromNoteTitle, relation, toNoteTitle) => {
    cy.creatingLinkFor(fromNoteTitle);
    cy.clickButtonOnCardBody(toNoteTitle, "Select");
    cy.get('select').select(relation);
    cy.findByRole('button', {name: "Link"}).click();
})

And("I should see the source note as {string}",(noteTitle) => {
    cy.findByText(`Link ${noteTitle} to:`).should("be.visible");
})

When("I search for notes with title {string}", (searchKey) => {
    cy.findByPlaceholderText("Search").type(searchKey);
    cy.findByText("Search").click();
})

And("I should see {string} as targets only",(noteTitlesAsString) => {
    cy.expectExactLinkTargets(noteTitlesAsString.commonSenseSplit(",").map(i=>i.trim()));
})

Then("I should see {string} has link {string} {string}",(noteTitle, relation, targetNoteTitles) => {
    cy.findByText(relation).should('be.visible');
    targetNoteTitles.commonSenseSplit(",").forEach(
        targetNoteTitle => cy.findByText(targetNoteTitle).should('be.visible')
    );
})

