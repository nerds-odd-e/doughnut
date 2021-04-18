import {
  Given,
  And,
  Then,
  When,
  Before
} from "cypress-cucumber-preprocessor/steps";

Given(
  "There is a circle {string} with {string} members",
  (circleName, members) => {
    cy.request({
      method: "POST",
      url: `/api/testability/seed_circle`,
      body: { circleName, members }
    }).then(response => {
      expect(response.status).to.equal(200);
    });
  }
);

When("I create a note {string} in circle {string}", (noteTitle, circleName) => {
  cy.navigateToCircle(circleName);
  cy.findByText("Add New Notebook In This Circle").click();
  cy.submitNoteFormWith([{ Title: noteTitle }]);
});
