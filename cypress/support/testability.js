/// <reference types="cypress" />
const { rmdirSync } = require("fs")

Cypress.Commands.add("cleanDBAndSeedData", () => {
  cy.request({
    method: "POST",
    url: "/api/testability/clean_db_and_reset_testability_settings",
  })
    .its("body")
    .should("equal", "OK")
})

Cypress.Commands.add("enableFeatureToggle", (enabled) => {
  cy.request({
    method: "POST",
    url: "/api/testability/feature_toggle",
    body: { enabled },
  })
})

Cypress.Commands.add("seedNotes", (notes, externalIdentifier = "", circleName = null) => {
  cy.request({
    method: "POST",
    url: `/api/testability/seed_notes`,
    body: {
      externalIdentifier,
      circleName,
      seedNotes: notes,
    },
  }).then((response) => {
    expect(response.body.length).to.equal(notes.length)
    const titles = notes.map((n) => n["title"])
    const noteMap = Object.assign({}, ...titles.map((t, index) => ({ [t]: response.body[index] })))
    cy.wrap(noteMap).as("seededNoteIdMap")
  })
})

Cypress.Commands.add("timeTravelTo", (day, hour) => {
  const travelTo = new Date(1976, 5, 1, hour).addDays(day)
  cy.request({
    method: "POST",
    url: "/api/testability/time_travel",
    body: { travel_to: JSON.stringify(travelTo) },
  })
    .its("status")
    .should("equal", 200)
})

Cypress.Commands.add("timeTravelRelativeToNow", (hours) => {
  cy.request({
    method: "POST",
    url: "/api/testability/time_travel_relative_to_now",
    body: { hours: JSON.stringify(hours) },
  })
    .its("status")
    .should("equal", 200)
})

Cypress.Commands.add("randomizerAlwaysChooseLast", (day, hour) => {
  cy.request({
    method: "POST",
    url: "/api/testability/randomizer",
    body: { choose: "last" },
  })
    .its("status")
    .should("equal", 200)
})

Cypress.Commands.add("seedCircle", (circle) => {
  cy.request({
    method: "POST",
    url: `/api/testability/seed_circle`,
    body: circle,
  }).then((response) => {
    expect(response.body).to.equal("OK")
  })
})

Cypress.Commands.add("cleanDownloadFolder", () => {
  const downloadsFolder = Cypress.config("downloadsFolder")
  try {
    rmdirSync(downloadsFolder, { maxRetries: 10, recursive: true })
    console.log(`${downloadsFolder} deleted successfully.`)
  } catch (err) {
    console.error(`Error while deleting ${downloadsFolder}!`)
  }
})
