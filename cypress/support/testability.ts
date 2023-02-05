/// <reference types="cypress" />
// @ts-check
import WikidataServiceTester from "./WikidataServiceTester"
import OpenAiServiceTester from "./OpenAiServiceTester"
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

Cypress.Commands.add(
  "timeTravelTo",
  { prevSubject: true },
  (testability: TestabilityHelper, day: number, hour: number) => {
    cy.wrap(testability).backendTimeTravelTo(day, hour)
    cy.window().then((window) => {
      cy.tick(testability.hourOfDay(day, hour).getTime() - new window.Date().getTime())
    })
  },
)

Cypress.Commands.add(
  "backendTimeTravelTo",
  { prevSubject: true },
  (testability: TestabilityHelper, day: number, hour: number) => {
    testability.postToTestabilityApiSuccessfully(cy, "time_travel", {
      body: { travel_to: JSON.stringify(testability.hourOfDay(day, hour)) },
    })
  },
)

Cypress.Commands.add(
  "backendTimeTravelRelativeToNow",
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

Cypress.Commands.add("openAiService", () => {
  cy.wrap(new OpenAiServiceTester())
})

Cypress.Commands.add("setServiceUrl", (serviceName: string, serviceUrl: string) => {
  return new TestabilityHelper()
    .postToTestabilityApi(cy, `replace_service_url`, {
      body: { [serviceName]: serviceUrl },
    })
    .then((response) => {
      expect(response.body).to.haveOwnProperty(serviceName)
      expect(response.body[serviceName]).to.include("http")
      cy.wrap(response.body[serviceName])
    })
})

Cypress.Commands.add(
  "mock",
  { prevSubject: true },
  (wikidataServiceTester: WikidataServiceTester) => {
    cy.setServiceUrl(wikidataServiceTester.serviceName, wikidataServiceTester.serviceUrl).as(
      wikidataServiceTester.savedServiceUrlName,
    )
    wikidataServiceTester.install()
  },
)

Cypress.Commands.add(
  "restore",
  { prevSubject: true },
  (wikidataServiceTester: WikidataServiceTester) => {
    wikidataServiceTester.restore(cy)
  },
)
