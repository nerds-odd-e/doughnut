/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check
import { Then, When } from "@badeball/cypress-cucumber-preprocessor"
import "../support/string_util"
import start from "../start"
import NotePath from "support/NotePath"

When('I start the assessment on {notepath} notebook', function (notePath: NotePath) {
  start.routerToNotebooksPage().navigateToPath(notePath)
  cy.get(".assessment-button").should("be.visible")
  cy.get('.assessment-launch-button').click();
});

Then('I should see the first question with 4 options', function () {
  cy.get('.assessment-modal').should('be.visible');
});



// When("I do the assessment on {notepath}", (notePath: NotePath) => {
//   start.routerToNotebooksPage().navigateToPath(notePath)
//   cy.get(".assessment-button").should("be.visible")
// })

// Then("I should see the first question and it should have 4 options", () =>{

// })
