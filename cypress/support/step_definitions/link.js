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

When("I click Create Link hyperlink on Sedition", () => {
    var hyperlink = cy.findByTestId('link-1');
    hyperlink.should("be.visible");
    hyperlink.click();
});

Then("I should be navigated to the linking page", () =>{
    cy.url().should('include', 'link/1');
});
