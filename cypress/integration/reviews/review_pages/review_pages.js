import {
  Given,
  And,
  Then,
  When,
  Before
} from "cypress-cucumber-preprocessor/steps";

Given("I go to the reviews page", () => {
  cy.visit("/reviews");
});

Then("I have selected the option {string} in review setting", option => {
  cy.getFormControl(option).check();
  cy.findByRole("button", { name: "Update" }).click();
});

Then("I should see that I have old notes to repeat", () => {
  cy.findByRole("button", { name: "Start reviewing old notes" });
});

Then("I should see that I have new notes to learn", () => {
  cy.findByRole("button", { name: "Start reviewing new notes" });
});
