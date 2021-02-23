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

Cypress.Commands.add("cleanDBAndSeedData", () => {
  cy.request("/api/testability/clean_db_and_seed_data").its("body").should("contain", "OK");
});

Cypress.Commands.add("loginAs", (username) => {
  const password = "password";
  cy.request({
    method: "POST",
    url: "/login",
    form: true,
    body: { username, password }
  }) .then((response) => {
    expect(response.status).to.equal(200);
  });
});

Cypress.Commands.add("seedNotes", (notes) => {
  cy.request({method: "POST", url: "/api/testability/seed_notes", body: notes})
  .then((response) => {
     expect(response.body.length).to.equal(notes.length);
  })
})

Cypress.Commands.add("createNotes", (notes) => {
  cy.visit("/note");
  notes.forEach((elem) => {
    for (var propName in elem) {
      cy.get(`[data-cy="${propName}"]`).type(elem[propName]);
    }
    cy.get('input[value="Submit"]').click();
  });
});

Cypress.Commands.add("linkNote", (sourceNoteId, targetNoteId) => {
  cy.request({
    method: "POST",
    url: "/linkNote",
    form: true,
    body: { sourceNoteId, targetNoteId }
  });
});

Cypress.Commands.add("clickButtonOnCard", (noteTitle, buttonTitle) => {
    const card = cy.findByText(noteTitle, { selector: ".card-title"});
    const button = card.parent().findByText(buttonTitle);
    button.click();
});

Cypress.Commands.add("creatingLinkFor", (noteTitle) => {
    cy.visit("/all_my_notes");
    cy.clickButtonOnCard(noteTitle, "Link Note");
});

Cypress.Commands.add("expectExactLinkTargets", (targets) => {
    targets.forEach((elem) => {
         cy.findByText(elem, {selector: '.card-title'}).should("be.visible");
    });
    cy.findAllByText(/.*/, {selector: '.card-title'}).should("have.length", targets.length);
})

