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

Cypress.Commands.add(
  "timeTravelRelativeToNow",
  { prevSubject: true },
  (testability: TestabilityHelper, hours: number) => {
    testability.timeTravelRelativeToNow(cy, hours)
  },
)

Cypress.Commands.add(
  "randomizerAlwaysChooseLast",
  { prevSubject: true },
  (testability: TestabilityHelper) => {
    testability.randomizerAlwaysChooseLast(cy)
  },
)

Cypress.Commands.add(
  "triggerException",
  { prevSubject: true },
  (testability: TestabilityHelper) => {
    testability.triggerException()
  },
)

Cypress.Commands.add(
  "shareToBazaar",
  { prevSubject: true },
  (testability: TestabilityHelper, noteTitle: string) => {
    testability.shareToBazaar(cy, noteTitle)
  },
)

Cypress.Commands.add(
  "seedCircle",
  { prevSubject: true },
  (testability: TestabilityHelper, circle: string) => {
    testability.seedCircle(cy, circle)
  },
)

Cypress.Commands.add(
  "updateCurrentUserSettingsWith",
  { prevSubject: true },
  (testability: TestabilityHelper, hash: Record<string, string>) => {
    testability.updateCurrentUserSettingsWith(cy, hash)
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
