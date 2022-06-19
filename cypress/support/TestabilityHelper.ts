/// <reference types="cypress" />

// @ts-check

const addDays = function (date: Date, days: number) {
  const newDate = new Date(date.valueOf())
  newDate.setDate(newDate.getDate() + days)
  return newDate
}

class TestabilityHelper {
  seedCircle(cy: Cypress.cy & CyEventEmitter, circle: string) {
    this.postToTestabilityApiSuccessfully(cy, "seed_circle", { body: circle })
  }
  triggerException() {
    this.postToTestabilityApi(cy, "trigger_exception", { failOnStatusCode: false })
  }
  randomizerAlwaysChooseLast(cy: Cypress.cy & CyEventEmitter) {
    this.postToTestabilityApiSuccessfully(cy, "randomizer", { body: { choose: "last" } })
  }
  timeTravelRelativeToNow(cy: Cypress.cy & CyEventEmitter, hours: number) {
    this.postToTestabilityApiSuccessfully(cy, "time_travel_relative_to_now", {
      body: { hours: JSON.stringify(hours) },
    })
  }
  seedLink(
    cy: Cypress.cy & CyEventEmitter,
    type: string,
    fromNoteTitle: string,
    toNoteTitle: string,
  ) {
    return cy.get(`@${this.seededNoteIdMapAliasName}`).then((seededNoteIdMap) => {
      expect(seededNoteIdMap).haveOwnPropertyDescriptor(fromNoteTitle)
      expect(seededNoteIdMap).haveOwnPropertyDescriptor(toNoteTitle)
      const fromNoteId = seededNoteIdMap[fromNoteTitle]
      const toNoteId = seededNoteIdMap[toNoteTitle]
      this.postToTestabilityApiSuccessfully(cy, "link_notes", {
        body: {
          type,
          source_id: fromNoteId,
          target_id: toNoteId,
        },
      })
    })
  }
  getSeededNoteIdByTitle(cy: Cypress.cy & CyEventEmitter, noteTitle: string) {
    return cy.get(`@${this.seededNoteIdMapAliasName}`).then((seededNoteIdMap) => {
      expect(seededNoteIdMap).haveOwnPropertyDescriptor(noteTitle)
      return seededNoteIdMap[noteTitle]
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
  timeTravelTo(cy: Cypress.cy & CyEventEmitter, day: number, hour: number) {
    const travelTo = addDays(new Date(1976, 5, 1, hour), day)
    this.postToTestabilityApiSuccessfully(cy, "time_travel", {
      body: { travel_to: JSON.stringify(travelTo) },
    })
  }

  featureToggle(cy: Cypress.cy & CyEventEmitter, enabled: boolean) {
    this.postToTestabilityApiSuccessfully(cy, "feature_toggle", { body: { enabled } })
  }

  cleanDBAndResetTestabilitySettings(cy: Cypress.cy & CyEventEmitter) {
    this.cleanAndReset(cy, 5)
  }

  private get seededNoteIdMapAliasName() {
    return "seededNoteIdMap"
  }

  private async cleanAndReset(cy: Cypress.cy & CyEventEmitter, countdown: number) {
    this.postToTestabilityApi(cy, "clean_db_and_reset_testability_settings", {
      failOnStatusCode: countdown === 1,
    }).then((response) => {
      if (countdown > 0 && response.status !== 200) {
        this.cleanAndReset(cy, countdown - 1)
      }
    })
  }

  private postToTestabilityApiSuccessfully(
    cy: Cypress.cy & CyEventEmitter,
    path: string,
    options: { body?: Record<string, unknown> | string; failOnStatusCode?: boolean },
  ) {
    this.postToTestabilityApi(cy, path, options).its("status").should("equal", 200)
  }

  private postToTestabilityApi(
    cy: Cypress.cy & CyEventEmitter,
    path: string,
    options: { body?: Record<string, unknown> | string; failOnStatusCode?: boolean },
  ) {
    return cy.request({
      method: "POST",
      url: `/api/testability/${path}`,
      ...options,
    })
  }
}

export default TestabilityHelper
