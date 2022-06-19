/// <reference types="cypress" />

// @ts-check

class TestabilityHelper {
  featureToggle(cy: Cypress.cy & CyEventEmitter, enabled: boolean) {
    cy.request({
      method: "POST",
      url: "/api/testability/feature_toggle",
      body: { enabled },
    })
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
}

export default TestabilityHelper
