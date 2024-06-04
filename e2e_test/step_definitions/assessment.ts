/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check
import {
  Then,
  When,
} from "@badeball/cypress-cucumber-preprocessor"
import "../support/string_util"
import start from "../start"
import NotePath from 'support/NotePath';

When("I do the assessment on {notepath}", (notePath: NotePath) => {
  start.routerToNotebooksPage().navigateToPath(notePath)
  cy.get(".assessment-button").should("be.visible")
})

Then("I should see 5 questions and the mcq questions have 4 options each", () =>{
  
})