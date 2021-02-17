import {
  Given,
  And,
  Then,
  When,
  Before,
} from "cypress-cucumber-preprocessor/steps";

Before({ tags: "@login_as_new_user" }, () => {
  cy.loginAsNewUser();
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
  });
  cy.get('input[value="Submit"]').click();
});

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