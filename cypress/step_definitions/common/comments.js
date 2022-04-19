/// <reference types="cypress" />
// @ts-check

import { Given, Then, When } from "cypress-cucumber-preprocessor/steps";

When("I add a comment {string}", (description) => {
  cy.get("#comment-input").click()
  cy.replaceFocusedText(description)
  cy.get("#comment-input").blur()
});

Then("I should see comment posted time", () => {
   cy.get(".comment-timestamp").should((div)=>{
    var timestamp = div.text();
    expect((new Date(timestamp)).getTime()>0).to.be.true;
  })
})
