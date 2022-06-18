/// <reference types="cypress" />
// @ts-check

class WikidataServiceTester {
  restore(cy: Cypress.cy & CyEventEmitter) {
    cy.get(`@${this.savedServiceUrlName}`).then((saved: string) =>
      this.setWikidataServiceUrl(cy, saved),
    )
  }
  mock(cy: Cypress.cy & CyEventEmitter) {
    this.setWikidataServiceUrl(cy, `http://localhost:${this.port}`).as(this.savedServiceUrlName)
  }
  get savedServiceUrlName() {
    return "savedWikidataServiceUrl"
  }
  get port() {
    return 5001
  }
  private setWikidataServiceUrl(cy: Cypress.cy & CyEventEmitter, wikidataServiceUrl: string) {
    return cy
      .request({
        method: "POST",
        url: `/api/testability/use_wikidata_service`,
        body: { wikidataServiceUrl },
      })
      .then((response) => {
        expect(response.body).to.include("http")
        cy.wrap(response.body)
      })
  }
}

export default WikidataServiceTester
