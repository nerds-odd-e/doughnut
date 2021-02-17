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
  cy.visit("/view");
});

Then("I should see 3 notes belonging to the user", (data) => {
    cy.findByText("Your Notes");

    data.hashes().forEach((elem) => {
         cy.findByText(elem["Note Title"]).should("be.visible");
    });

});
