/// <reference types="cypress" />

import { Mountebank, Imposter, DefaultStub, HttpMethod } from "@anev/ts-mountebank"
import TestabilityHelper from "./TestabilityHelper"

// @ts-check

class WikidataServiceTester {
  imposter = new Imposter().withPort(5001)

  restore(cy: Cypress.cy & CyEventEmitter) {
    cy.get(`@${this.savedServiceUrlName}`).then((saved: string) =>
      this.setWikidataServiceUrl(cy, saved),
    )
  }
  mock(cy: Cypress.cy & CyEventEmitter) {
    this.setWikidataServiceUrl(cy, `http://localhost:${this.imposter.port}`).as(
      this.savedServiceUrlName,
    )
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
  private setWikidataServiceUrl(cy: Cypress.cy & CyEventEmitter, wikidataServiceUrl: string) {
    return new TestabilityHelper()
      .postToTestabilityApi(cy, `use_wikidata_service`, { body: { wikidataServiceUrl } })
      .then((response) => {
        expect(response.body).to.include("http")
        cy.wrap(response.body)
      })
  }
  private async stub(url: string, data: unknown) {
    const mb = new Mountebank()
    await mb.deleteImposter(this.imposter.port)
    this.imposter.withStub(new DefaultStub(url, HttpMethod.GET, data, 200))
    await mb.createImposter(this.imposter)
  }
}

export default WikidataServiceTester
