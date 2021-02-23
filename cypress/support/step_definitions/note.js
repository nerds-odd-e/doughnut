import {
  Given,
  And,
  Then,
  When,
  Before,
} from "cypress-cucumber-preprocessor/steps";

Given("there are some notes for the current user", (data) => {
  cy.seedNotes(data.hashes());
})

When("I create note with:", (data) => {
  cy.createNotes(data.hashes());
});

Then("Reviews should include single note pages:", (data) => {
  let examples = data.rowsHash();
  let matched = new Set();
  cy.recursiveLookUpInReview(
      5,
      (history, currentNoteTitle, done) => {
           if(currentNoteTitle in examples) {
                matched.add(currentNoteTitle);
                cy.get(`[data-cy="note-description"]`).should("contain", examples[currentNoteTitle]);
                if(matched.size == Object.keys(examples).length) done();
           }
      }
  );
})

Then("Reviews should include note page with:", (data) => {
  cy.location("pathname", { timeout: 10000 }).should("eq", "/review");
  data.hashes().forEach((elem) => {
    for (var propName in elem) {
      var domElement = cy.get(`[data-cy="${propName}"]`);
      domElement.should("contain", elem[propName])
    }
  });
})

