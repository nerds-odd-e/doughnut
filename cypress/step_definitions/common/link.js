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

When("I link note {string} as {string} note {string}", (fromNoteTitle, linkType, toNoteTitle) => {
    cy.creatingLinkFor(fromNoteTitle);
    cy.searchNote(toNoteTitle);
    cy.clickButtonOnCardBody(toNoteTitle, "Select");
    cy.get('select').select(linkType);
    cy.findByRole('button', {name: "Create Link"}).click();
})

When("there is {string} link between note {string} and {string}", (linkType, fromNoteTitle, toNoteTitle) => {
    cy.createLink(linkType, fromNoteTitle, toNoteTitle);
})

And("I should see the source note as {string}",(noteTitle) => {
    cy.findByText(noteTitle, {selector: 'strong'}).should("be.visible");
})

And("I should see {string} as targets only when searching {string}",(noteTitlesAsString, searchKey) => {
    cy.searchNote(searchKey);
    cy.expectExactLinkTargets(noteTitlesAsString.commonSenseSplit(",").map(i=>i.trim()));
})

And("I should see note cannot be found when searching {string}",(searchKey) => {
    cy.searchNote(searchKey);
    cy.findByText('No linkable notes found.').should("be.visible");
})

Then("On the current page, I should see {string} has link {string} {string}",(noteTitle, linkType, targetNoteTitles) => {
    cy.findByText(linkType).parent().within(()=> {
            targetNoteTitles.commonSenseSplit(",").forEach(
                targetNoteTitle => cy.findByText(targetNoteTitle).should('be.visible')
        )
    })
})

Then("I should see {string} has no link of type {string}",(noteTitle, linkType) => {
    cy.jumpToNotePage(noteTitle)
    cy.findByText(linkType).should("not.exist")
})

When("I open link {string}", (linkTitle) => {
    cy.findByText(linkTitle).click();
})

Then("I should be able to change the link to {string}", (linkType) => {
    cy.get('select').select(linkType);
    cy.findByRole('button', {name: "Update"}).click();
    cy.findByText(linkType).should('be.visible');
});

Then("I should be able to delete the link", () => {
    cy.findByRole('button', {name: "Delete"}).click();
});

Then("I should be able to delete the link to note {string}", (noteTitle) => {
    cy.findByText(noteTitle).click();
    cy.findByRole('button', {name: "Delete"}).click();
    cy.contains(noteTitle).should('not.exist')
});

