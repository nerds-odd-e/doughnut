/// <reference types="cypress" />
// @ts-check

import { Given, Then, When } from "cypress-cucumber-preprocessor/steps";

When("I comment with {string} on note {string}", (comment, noteTitle) => {
  cy.jumpToNotePage(noteTitle)
  cy.clickNotePageMoreOptionsButton(noteTitle, "Add comment")
  cy.replaceFocusedText(comment)
});

Then("I should see comment posted time", () => {
   cy.get(".comment-timestamp").should((div)=>{
    var timestamp = div.text();
    expect((new Date(timestamp)).getTime()>0).to.be.true;
  })
})
