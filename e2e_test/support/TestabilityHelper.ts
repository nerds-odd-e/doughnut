/// <reference types="cypress" />

// @ts-check

class TestabilityHelper {
  hourOfDay(days: number, hours: number) {
    return new Date(1976, 5, 1 + days, hours)
  }

  getSeededNoteIdByTitle(cy: Cypress.cy & CyEventEmitter, noteTopic: string) {
    return cy.get(`@${this.seededNoteIdMapAliasName}`).then((seededNoteIdMap) => {
      expect(seededNoteIdMap).haveOwnPropertyDescriptor(noteTopic)
      return seededNoteIdMap[noteTopic]
    })
  }

  private get seededNoteIdMapAliasName() {
    return "seededNoteIdMap"
  }

  postToTestabilityApiSuccessfully(
    cy: Cypress.cy & CyEventEmitter,
    path: string,
    options: { body?: Record<string, unknown>; failOnStatusCode?: boolean },
  ) {
    this.postToTestabilityApi(cy, path, options).its("status").should("equal", 200)
  }

  postToTestabilityApi(
    cy: Cypress.cy & CyEventEmitter,
    path: string,
    options: { body?: Record<string, unknown>; failOnStatusCode?: boolean },
  ) {
    return cy.request({
      method: "POST",
      url: `/api/testability/${path}`,
      ...options,
    })
  }

  getTestabilityApiSuccessfully(cy: Cypress.cy & CyEventEmitter, path: string) {
    return cy.request({
      method: "GET",
      url: `/api/testability/${path}`,
    })
  }
}

export default TestabilityHelper
