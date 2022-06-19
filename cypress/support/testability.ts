/// <reference types="cypress" />
// @ts-check
import WikidataServiceTester from "./WikidataServiceTester"
import TestabilityHelper from "./TestabilityHelper"

Cypress.Commands.add("testability", () => {
  cy.wrap(new TestabilityHelper())
})

Cypress.Commands.add(
  "cleanDBAndResetTestabilitySettings",
  { prevSubject: true },
  (testability: TestabilityHelper) => {
    testability.cleanDBAndResetTestabilitySettings(cy)
  },
)

Cypress.Commands.add(
  "featureToggle",
  { prevSubject: true },
  (testability: TestabilityHelper, enabled: boolean) => {
    testability.featureToggle(cy, enabled)
  },
)

Cypress.Commands.add(
  "seedNotes",
  { prevSubject: true },
  (
    testability: TestabilityHelper,
    seedNotes: unknown[],
    externalIdentifier = "",
    circleName = null,
  ) => {
    testability.seedNotes(cy, seedNotes, externalIdentifier, circleName)
  },
)

Cypress.Commands.add(
  "seedLink",
  { prevSubject: true },
  (testability: TestabilityHelper, type: string, fromNoteTitle: string, toNoteTitle: string) => {
    testability.seedLink(cy, type, fromNoteTitle, toNoteTitle)
  },
)

Cypress.Commands.add(
  "getSeededNoteIdByTitle",
  { prevSubject: true },
  (testability: TestabilityHelper, noteTitle: string) => {
    testability.getSeededNoteIdByTitle(cy, noteTitle)
  },
)

Cypress.Commands.add(
  "timeTravelTo",
  { prevSubject: true },
  (testability: TestabilityHelper, day: number, hour: number) => {
    testability.timeTravelTo(cy, day, hour)
  },
)

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
    wikidataServiceTester.mock(cy)
  },
)

Cypress.Commands.add(
  "restore",
  { prevSubject: true },
  (wikidataServiceTester: WikidataServiceTester) => {
    wikidataServiceTester.restore(cy)
  },
)
