// ***********************************************
// custom commands and overwrite existing commands.
//
// For more comprehensive examples of custom
// commands please read more here:
// https://on.cypress.io/custom-commands
// ***********************************************
//
//
// -- This is a parent command --
// Cypress.Commands.add("login", (email, password) => { ... })
//
//
// -- This is a child command --
// Cypress.Commands.add("drag", { prevSubject: 'element'}, (subject, options) => { ... })
//
//
// -- This is a dual command --
// Cypress.Commands.add("dismiss", { prevSubject: 'optional'}, (subject, options) => { ... })
//
//
// -- This will overwrite an existing command --
// Cypress.Commands.overwrite("visit", (originalFn, url, options) => { ... })

import '@testing-library/cypress/add-commands'

Cypress.Commands.add("cleanDB", () => {
  cy.request("/api/testability/clean_db").its("body").should("contain", "OK");
});

Cypress.Commands.add("loginAsNewUser", () => {
  cy.visit("/login");

  cy.get("#username").type("user");
  cy.get("#password").type("password");
  cy.get("form.form-signin").submit();
  cy.location("pathname", { timeout: 10000 }).should("eq", "/");
  cy.get('input[type="submit"][value="Logout"]').should("be.visible");

  cy.get("#name").type("Learner A");
  cy.get('input[value="Submit"]').click();
});

Cypress.Commands.add("loginAsExistingUser", () => {
  cy.visit("/login");

  cy.get("#username").type("user");
  cy.get("#password").type("password");
  cy.get("form.form-signin").submit();
  cy.location("pathname", { timeout: 10000 }).should("eq", "/");
  cy.get('input[type="submit"][value="Logout"]').should("be.visible");
});

Cypress.Commands.add("seedNotes", (notes) => {
  let now = Date.now();

  const createNotes = (notes) =>{
    cy.request({method: "POST", url: "/api/testability/seed_notes", body: notes})
    .then((response) => {
        expect(response.body.length).to.equal(notes.length);
    })
  }

  if (!notes){
    cy.fixture('notes').then(notes =>createNotes(notes));
  } else {
    createNotes(notes);
  }
})

Cypress.Commands.add("linkNote", (sourceNoteId, targetNoteId) => {
  cy.request({
    method: "POST",
    url: "/linkNote",
    form: true,
    body: { sourceNoteId, targetNoteId }
  });
});
