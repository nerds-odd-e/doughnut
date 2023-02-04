/// <reference types="cypress" />

import TestabilityHelper from "./TestabilityHelper"
import MountebankWrapper from "./MountebankWrapper"

// @ts-check

class OpenAiServiceTester {
  mountebank = new MountebankWrapper(5002)

  mock(cy: Cypress.cy & CyEventEmitter) {
    this.setOpenAiServiceUrl(cy, this.mountebank.serviceUrl).as(this.savedServiceUrlName)
  }

  restore(cy: Cypress.cy & CyEventEmitter) {
    cy.get(`@${this.savedServiceUrlName}`).then((saved) =>
      this.setOpenAiServiceUrl(cy, saved as unknown as string),
    )
  }

  get savedServiceUrlName() {
    return "OpenAiServiceTester"
  }

  private setOpenAiServiceUrl(cy: Cypress.cy & CyEventEmitter, openAiServiceUrl: string) {
    return new TestabilityHelper()
      .postToTestabilityApi(cy, `use_openai_service`, {
        body: { openAiServiceUrl: openAiServiceUrl },
      })
      .then((response) => {
        expect(response.body).to.include("http")
        cy.wrap(response.body)
      })
  }
}

export default OpenAiServiceTester
