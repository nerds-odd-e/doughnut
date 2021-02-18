import {
  Given,
  And,
  Then,
  When,
  Before,
} from "cypress-cucumber-preprocessor/steps";

Before({ tags: "@seed_notes" }, () => {
  cy.seedNotes();
});

When("I create note", () => {
  cy.visit("/note");
});

When("I did not log in", () => {

});

Then("I should be asked to log in", () => {
  cy.location("pathname", { timeout: 10000 }).should("eq", "/");
});

When("I create note with:", (data) => {
  cy.visit("/note");

  data.hashes().forEach((elem) => {
    for (var propName in elem) {
      cy.get(`[data-cy="${propName}"]`).type(elem[propName]);
     }
     cy.get('input[value="Submit"]').click();
    });

});onload="javascript:getNotes()"

Then("I should see a note saved message", () => {
  const stub = cy.stub()

  cy.on('window:alert', stub);
  expect(stub.getCall(0)).to.be.calledWith('Note created!');
})

Then("I should see the note with title and description on the review page", (data) => {
  cy.location("pathname", { timeout: 10000 }).should("eq", "/review");

  data.hashes().forEach((elem) => {
    for (var propName in elem) {
      var domElement = cy.get(`[data-cy="${propName}"]`);
      domElement.should("contain",elem[propName])
     }
  });
})

Given("I have some notes", () => {
  cy.seedNotes();
})

When("I review my notes", () => {
  cy.visit("/review")
})

Then("I click on next note", () => {
  cy.findByText("Next").click();
})
