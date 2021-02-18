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

Then("I should be navigated to the linking page", () =>{
    cy.url().should('include', 'link/1');
});

And("I should see below notes",(data) => {
    data.hashes().forEach((elem) => {
         cy.findByText(elem["note-title"]).should("be.visible");
    });
})