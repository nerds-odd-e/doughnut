import {
  Given,
  And,
  Then,
  When,
  Before,
  Background
} from "cypress-cucumber-preprocessor/steps";
Given("I create note with:", (data) => {
   data.hashes().forEach((elem) => {
        cy.visit("/note");
        for (var propName in elem) {

            cy.get(`[data-cy="${propName}"]`).type(elem[propName]);
        }
        cy.get('input[value="Submit"]').click();
  });
});

Then("I should see these notes belonging to the user", (data) => {
    cy.visit("/all_my_notes");
    cy.findByText("Your Notes");
    data.hashes().forEach((elem) => {
         cy.findByText(elem["note-title"]).should("be.visible");
    });
});

When("I create link for note {string}", (noteTitle) => {
    cy.creatingLinkFor(noteTitle);
});

And("I should be able to see the buttons for linking note", () => {
    cy.findAllByText("Select").should("have.lengthOf", 2);
})

When("I select a Sedation note", () => {
    cy.findByTestId("button-2").click();
})

Then("I should be redirected to review page", () => {
    cy.url().should('include', 'review');
})

And("I should see the Sedition note linked to Sedation",() => {
    cy.findByText("Sedition").should("be.visible");
    cy.findByText("Sedation").should("be.visible");
})


And("I should see the source note as {string}",(noteTitle) => {
    cy.findByText(`Link ${noteTitle} to:`).should("be.visible");
})

When("I search for notes with title {string}", (searchKey) => {
    cy.findByPlaceholderText("Search").type(searchKey);
    cy.findByText("Search").click();
})

And("I should see below notes as targets only",(data) => {
    cy.expectExactLinkTargets(data.rows().flat());
})

