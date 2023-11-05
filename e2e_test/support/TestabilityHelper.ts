/// <reference types="cypress" />

// @ts-check

class TestabilityHelper {
  hourOfDay(days: number, hours: number) {
    return new Date(1976, 5, 1 + days, hours)
  }
  seedLink(
    cy: Cypress.cy & CyEventEmitter,
    type: string,
    fromNoteTopic: string,
    toNoteTopic: string,
  ) {
    return cy.get(`@${this.seededNoteIdMapAliasName}`).then((seededNoteIdMap) => {
      expect(seededNoteIdMap).haveOwnPropertyDescriptor(fromNoteTopic)
      expect(seededNoteIdMap).haveOwnPropertyDescriptor(toNoteTopic)
      const fromNoteId = seededNoteIdMap[fromNoteTopic]
      const toNoteId = seededNoteIdMap[toNoteTopic]
      this.postToTestabilityApiSuccessfully(cy, "link_notes", {
        body: {
          type,
          source_id: fromNoteId,
          target_id: toNoteId,
        },
      })
    })
  }
  getSeededNoteIdByTitle(cy: Cypress.cy & CyEventEmitter, noteTopic: string) {
    return cy.get(`@${this.seededNoteIdMapAliasName}`).then((seededNoteIdMap) => {
      expect(seededNoteIdMap).haveOwnPropertyDescriptor(noteTopic)
      return seededNoteIdMap[noteTopic]
    })
  }
  seedNotes(
    cy: Cypress.cy & CyEventEmitter,
    seedNotes: unknown[],
    externalIdentifier: string,
    circleName: string | null,
  ) {
    this.postToTestabilityApi(cy, "seed_notes", {
      body: {
        externalIdentifier,
        circleName,
        seedNotes,
      },
    }).then((response) => {
      expect(Object.keys(response.body).length).to.equal(seedNotes.length)
      cy.wrap(response.body).as(this.seededNoteIdMapAliasName)
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
