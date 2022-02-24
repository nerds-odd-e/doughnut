

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

And('I add a comment with description {string}',(commentDescription) => {
  cy.url().then(url => {
    const noteId = url.split("/").slice(-1)[0];
    cy.request({
      method: "POST",
      url: `/api/comments/${noteId}/add`,
      body: { commentDescription },
    }).then((response) => {
      expect(response.status).to.equal(200);
    });
  });
})

When('{string} adds a comment for note with description {string}',(externalIdentifier, commentDescription) => {
  cy.url().then(url => {
    const noteId = url.split("/").slice(-1)[0];
    cy.request({
      method: "POST",
      url: `/api/testability/add_comment_by_user`,
      body: { externalIdentifier, note: noteId, commentDescription },
    }).then((response) => {
      expect(response.status).to.equal(200);
    });
  });
})

And('I should see a comment with description {string}', (commentDescription) => {
  cy.findByText("Show Comment").click();
  cy.findByText(commentDescription);
});

And('I should be able to add a comment with description {string}', (comment) => {
  cy.focused().type(comment).blur();
  cy.findByText(comment);
})
