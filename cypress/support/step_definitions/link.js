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

When("I go to the notes page", () => {
  cy.visit("/all_my_notes");
});

Then("I should see these notes belonging to the user", (data) => {
    cy.visit("/all_my_notes");
    cy.findByText("Your Notes");
    data.hashes().forEach((elem) => {
         cy.findByText(elem["note-title"]).should("be.visible");
    });
});

When("I create link for note {string}", (noteTitle) => {
    cy.visit("/all_my_notes");
    const card = cy.findByText(noteTitle, { selector: ".card-title"});
    const button = card.parent().findByText("Link Note");
    button.click();
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


And("I should see the source note as Sedition",() => {
    cy.findByText("Link Sedition to:").should("be.visible");
})

And("I should see below notes",(data) => {
    data.hashes().forEach((elem) => {
         cy.findByText(elem["note-title"]).should("be.visible");
    });
})

When("I search for notes with title \"Sedatio\"", () => {
    cy.findByPlaceholderText("Search")
      .type("Sedatio");
    cy.findByText("Search")
      .click();
})

Then("I should see only \"Sedation\"", () =>{
 cy.findByText("Sedation").should("be.visible");
})

