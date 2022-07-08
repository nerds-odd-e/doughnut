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
    testability.cleanAndReset(cy, 5)
  },
)

Cypress.Commands.add(
  "featureToggle",
  { prevSubject: true },
  (testability: TestabilityHelper, enabled: boolean) => {
    testability.postToTestabilityApiSuccessfully(cy, "feature_toggle", { body: { enabled } })
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

const addDays = function (date: Date, days: number) {
  const newDate = new Date(date.valueOf())
  newDate.setDate(newDate.getDate() + days)
  return newDate
}

Cypress.Commands.add(
  "backendTimeTravelTo",
  { prevSubject: true },
  (testability: TestabilityHelper, day: number, hour: number) => {
    const travelTo = addDays(new Date(1976, 5, 1, hour), day)
    testability.postToTestabilityApiSuccessfully(cy, "time_travel", {
      body: { travel_to: JSON.stringify(travelTo) },
    })
  },
)

Cypress.Commands.add(
  "timeTravelRelativeToNow",
  { prevSubject: true },
  (testability: TestabilityHelper, hours: number) => {
    testability.postToTestabilityApiSuccessfully(cy, "time_travel_relative_to_now", {
      body: { hours: JSON.stringify(hours) },
    })
  },
)

Cypress.Commands.add(
  "randomizerAlwaysChooseLast",
  { prevSubject: true },
  (testability: TestabilityHelper) => {
    testability.postToTestabilityApiSuccessfully(cy, "randomizer", { body: { choose: "last" } })
  },
)

Cypress.Commands.add(
  "triggerException",
  { prevSubject: true },
  (testability: TestabilityHelper) => {
    testability.postToTestabilityApi(cy, "trigger_exception", { failOnStatusCode: false })
  },
)

Cypress.Commands.add(
  "shareToBazaar",
  { prevSubject: true },
  (testability: TestabilityHelper, noteTitle: string) => {
    testability.postToTestabilityApiSuccessfully(cy, "share_to_bazaar", { body: { noteTitle } })
  },
)

Cypress.Commands.add(
  "seedCircle",
  { prevSubject: true },
  (testability: TestabilityHelper, circleInfo: Record<string, string>) => {
    testability.postToTestabilityApiSuccessfully(cy, "seed_circle", { body: circleInfo })
  },
)

Cypress.Commands.add(
  "updateCurrentUserSettingsWith",
  { prevSubject: true },
  (testability: TestabilityHelper, hash: Record<string, string>) => {
    testability.postToTestabilityApiSuccessfully(cy, "update_current_user", { body: hash })
  },
)

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
