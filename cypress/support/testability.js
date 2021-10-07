/// <reference types="cypress" />

Cypress.Commands.add("cleanDBAndSeedData", () => {
  cy.request({ method: "POST", url: "/api/testability/clean_db_and_seed_data" })
    .its("body")
    .should("equal", "OK");
});

Cypress.Commands.add("enableFeatureToggle", (enabled) => {
  cy.request({ method: "POST", url: "/api/testability/feature_toggle", body: { enabled } })
    .its("body")
    .should("equal", "OK");
});

Cypress.Commands.add("seedNotes", (notes, externalIdentifier = "") => {
  cy.request({
    method: "POST",
    url: `/api/testability/seed_notes?external_identifier=${externalIdentifier}`,
    body: notes,
  }).then((response) => {
    expect(response.body.length).to.equal(notes.length);
    const titles = notes.map((n) => n["title"]);
    const noteMap = Object.assign(
      {},
      ...titles.map((t, index) => ({ [t]: response.body[index] }))
    );
    cy.wrap(noteMap).as("seededNoteIdMap");
  });
});

Cypress.Commands.add("timeTravelTo", (day, hour) => {
  const travelTo = new Date(1976, 5, 1, hour).addDays(day);
  cy.request({
    method: "POST",
    url: "/api/testability/time_travel",
    body: { travel_to: JSON.stringify(travelTo) },
  })
    .its("status")
    .should("equal", 200);
});

Cypress.Commands.add("timeTravelRelativeToNow", (hours) => {
  cy.request({
    method: "POST",
    url: "/api/testability/time_travel_relative_to_now",
    body: { hours: JSON.stringify(hours) },
  })
    .its("status")
    .should("equal", 200);
});

Cypress.Commands.add("randomizerAlwaysChooseLast", (day, hour) => {
  cy.request({
    method: "POST",
    url: "/api/testability/randomizer",
    body: { choose: "last" },
  })
    .its("status")
    .should("equal", 200);
});

Cypress.Commands.add("seedCircle", (circle) => {
  cy.request({
    method: "POST",
    url: `/api/testability/seed_circle`,
    body: circle,
  }).then((response) => {
    expect(response.body).to.equal("OK");
  });
});

Cypress.Commands.add("createNotebook", (title, description) => {
  cy.request({
    method: "POST",
    url: `/api/notebooks/create?title=${title}&description=${description}`,
  }).then((response) => {
    expect(response.status).to.equal(200);
    cy.wrap(response.body).as("notebookId");
  });
});
