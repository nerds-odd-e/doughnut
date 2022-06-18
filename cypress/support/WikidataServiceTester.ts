/// <reference types="cypress" />

import { Mountebank, Imposter, DefaultStub, HttpMethod } from "@anev/ts-mountebank"

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
  stubWikidataEntityQuery(wikidataId: string, wikidataTitle: string, wikipediaLink: string) {
    const wikipedia = wikipediaLink ? { enwiki: { site: "enwiki", url: wikipediaLink } } : {}
    const mb = new Mountebank()
    const imposter = new Imposter().withPort(5001).withStub(
      new DefaultStub(
        `/wiki/Special:EntityData/${wikidataId}.json`,
        HttpMethod.GET,
        {
          entities: {
            [wikidataId]: {
              labels: {
                en: {
                  language: "en",
                  value: wikidataTitle,
                },
              },
              sitelinks: { ...wikipedia },
            },
          },
        },
        200,
      ),
    )
    return mb.createImposter(imposter)
  }

  stubWikidataSearchResult(wikidataLabel: string, wikidataId: string) {
    const mb = new Mountebank()
    const imposter = new Imposter().withPort(5001).withStub(
      new DefaultStub(
        `/w/api.php`,
        HttpMethod.GET,
        {
          search: [
            {
              id: wikidataId,
              label: wikidataLabel,
              description:
                'genre of popular music that originated as"rock and roll"in 1950s United States',
            },
          ],
        },
        200,
      ),
    )
    return mb.createImposter(imposter)
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
