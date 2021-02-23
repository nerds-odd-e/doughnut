import {
  Given,
  And,
  Then,
  When,
  Before,
  Background
} from "cypress-cucumber-preprocessor/steps";

Then("I should see these notes belonging to the user", (data) => {
    cy.visit("/all_my_notes");
    cy.findByText("Your Notes");
    data.hashes().forEach((elem) => {
         cy.findByText(elem["note-title"]).should("be.visible");
    });
});

When("I am creating link for note {string}", (noteTitle) => {
    cy.creatingLinkFor(noteTitle);
});

When("I link to note {string}", (noteTitle) => {
    cy.clickButtonOnCard(noteTitle, "Select");
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

