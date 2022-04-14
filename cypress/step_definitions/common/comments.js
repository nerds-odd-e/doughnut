/// <reference types="cypress" />
// @ts-check

import { Given, Then, When } from "cypress-cucumber-preprocessor/steps";

Then("I should see an input box for comment", () => {
  cy.get("#comment-input").should("be.visible");
});

Given("there is a note and some comments of current user", (comment) => {
  cy.seedNotes([
    {
      title: "A",
    },
  ]);
  cy.get("@seededNoteIdMap").then((seededNoteIdMap) => {
    cy.seedComments(seededNoteIdMap["A"], comment.hashes());
  });
});

When("I delete comment {string} under Note {string}", (comment, noteTitle) => {
  cy.navigateToNotePage(noteTitle);
  cy.get("@seededCommentIdMap").then((seededCommentIdMap) => {
    cy.get(`#comment-${seededCommentIdMap[comment]}-delete`).click();
  });
});

Then("Note A only have one comment {string}", () => {
  cy.get(".comment").should("have.length", 1);
});


When(
  "I reply to comment {string} with {string}",
  (commentName, description) => {
    cy.get("#reply-input").type(description).blur();
  }
);

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
