/// <reference types="cypress" />

// @ts-check

const addDays = function (date: Date, days: number) {
  const newDate = new Date(date.valueOf())
  newDate.setDate(newDate.getDate() + days)
  return newDate
}

class TestabilityHelper {
  timeTravelTo(cy: Cypress.cy & CyEventEmitter, day: number, hour: number) {
    const travelTo = addDays(new Date(1976, 5, 1, hour), day)
    this.postToTestabilityApi(cy, "time_travel", { travel_to: JSON.stringify(travelTo) })
  }

  featureToggle(cy: Cypress.cy & CyEventEmitter, enabled: boolean) {
    this.postToTestabilityApi(cy, "feature_toggle", { enabled })
  }
  cleanDBAndResetTestabilitySettings(cy: Cypress.cy & CyEventEmitter) {
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
  }

  private postToTestabilityApi(
    cy: Cypress.cy & CyEventEmitter,
    path: string,
    body: Record<string, unknown>,
  ) {
    cy.request({
      method: "POST",
      url: `/api/testability/{path}`,
      body,
    })
      .its("status")
      .should("equal", 200)
  }
}

export default TestabilityHelper
