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

When("I navigate to the notes page", () => {
  cy.findByText("View").click();
});

Then("I should see 3 notes belonging to the user", (data) => {
    cy.findByText("Your Notes");

    data.hashes().forEach((elem) => {
         cy.findByText(elem["note-title"]).should("be.visible");
    });

});

When("I click Create Link button on Sedition", () => {
    var button = cy.findByTestId('button-1');
    button.should("be.visible");
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


Then("I should be navigated to the linking page", () =>{
    cy.url().should('include', 'link/1');
});

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

