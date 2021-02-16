import {
  Given,
  And,
  Then,
  When,
  Before,
} from "cypress-cucumber-preprocessor/steps";

When("I create note", () => {
  cy.visit("/note");
});

When("I did not log in", () => {

});


Then("I should be asked to log in", () => {
  cy.location("pathname", { timeout: 10000 }).should("eq", "/");
});