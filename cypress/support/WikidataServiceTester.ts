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
    this.stub(`/wiki/Special:EntityData/${wikidataId}.json`, {
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
    })
  }

  stubWikidataSearchResult(wikidataLabel: string, wikidataId: string) {
    this.stub(`/w/api.php`, {
      search: [
        {
          id: wikidataId,
          label: wikidataLabel,
          description:
            'genre of popular music that originated as"rock and roll"in 1950s United States',
        },
      ],
    })
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
  private stub(url: string, data: unknown) {
    const mb = new Mountebank()
    const imposter = new Imposter()
      .withPort(this.port)
      .withStub(new DefaultStub(url, HttpMethod.GET, data, 200))
    return mb.createImposter(imposter)
  }
}

export default WikidataServiceTester
