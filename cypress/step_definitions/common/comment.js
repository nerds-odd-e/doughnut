

import {
  And,
  Before,
  Given,
  Then,
  When,
} from "cypress-cucumber-preprocessor/steps";

And('I click the add comment button', () => {
  cy.findByText("Add Comment").click();
})

And('{string} adds a comment with description {string}',(user, commentDescription) => {
  const noteId = window.location.href.split("/").slice(-1)[0];
  cy.log(noteId);
  cy.request({
    method: "POST",
    url: "/api/comments/{noteId}/add",
    body: { commentDescription },
  }).then((response) => {
    expect(response.status).to.equal(200);
  });
})