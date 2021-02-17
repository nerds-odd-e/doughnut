import {
  Given,
  And,
  Then,
  When,
  Before,
  Background
} from "cypress-cucumber-preprocessor/steps";

Before({ tags: "@clean_db" }, () => {
  cy.cleanDB();
});

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

Then("I should see all notes belonging to the user", () => {
    cy.findByText("Your Notes");
});