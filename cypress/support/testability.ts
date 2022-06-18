/// <reference types="cypress" />
// @ts-check
import WikidataServiceTester from "./WikidataServiceTester"

Cypress.Commands.add("cleanDBAndSeedData", () => {
  cy.request({
    method: "POST",
    url: "/api/testability/clean_db_and_reset_testability_settings",
    failOnStatusCode: false,
  }).then((response) => {
    if (response.status !== 200) {
      cy.request({
        method: "POST",
        url: "/api/testability/clean_db_and_reset_testability_settings",
      })
    }
  })
})

Cypress.Commands.add("enableFeatureToggle", (enabled) => {
  cy.request({
    method: "POST",
    url: "/api/testability/feature_toggle",
    body: { enabled },
  })
})

Cypress.Commands.add(
  "seedNotes",
  (seedNotes: unknown[], externalIdentifier = "", circleName = null) => {
    cy.request({
      method: "POST",
      url: `/api/testability/seed_notes`,
      body: {
        externalIdentifier,
        circleName,
        seedNotes,
      },
    }).then((response) => {
      expect(Object.keys(response.body).length).to.equal(seedNotes.length)
      cy.wrap(response.body).as("seededNoteIdMap")
    })
  },
)

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

Cypress.Commands.add("randomizerAlwaysChooseLast", () => {
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

Cypress.Commands.add("wikidataService", () => {
  cy.wrap(new WikidataServiceTester())
})

Cypress.Commands.add(
  "mock",
  { prevSubject: true },
  (wikidataServiceTester: WikidataServiceTester) => {
    cy.setWikidataServiceUrl(`http://localhost:${wikidataServiceTester.port}`).as(
      wikidataServiceTester.savedServiceUrlName,
    )
  },
)

Cypress.Commands.add(
  "restore",
  { prevSubject: true },
  (wikidataServiceTester: WikidataServiceTester) => {
    cy.get(`@${wikidataServiceTester.savedServiceUrlName}`).then((saved) =>
      cy.setWikidataServiceUrl(saved),
    )
  },
)

Cypress.Commands.add("setWikidataServiceUrl", (wikidataServiceUrl: string) => {
  return cy
    .request({
      method: "POST",
      url: `/api/testability/use_wikidata_service`,
      body: { wikidataServiceUrl },
    })
    .then((response) => {
      expect(response.body).to.include("http")
      cy.wrap(response.body)
    })
})
