import { Stub } from '@anev/ts-mountebank';
/// <reference types="cypress" />

import { Mountebank, Imposter, DefaultStub, HttpMethod, FlexiPredicate } from "@anev/ts-mountebank"
import TestabilityHelper from "./TestabilityHelper"

// @ts-check

class WikidataServiceTester {
  imposter = new Imposter().withPort(5001)

  restore(cy: Cypress.cy & CyEventEmitter) {
    cy.get(`@${this.savedServiceUrlName}`).then((saved) =>
      this.setWikidataServiceUrl(cy, saved as unknown as string),
    )
  }
  mock(cy: Cypress.cy & CyEventEmitter) {
    this.setWikidataServiceUrl(cy, `http://localhost:${this.imposter.port}`).as(
      this.savedServiceUrlName,
    )
  }
  stubWikidataEntityQuery(wikidataId: string, wikidataTitle: string, wikipediaLink: string) {
    const wikipedia = wikipediaLink ? { enwiki: { site: "enwiki", url: wikipediaLink } } : {}
    this.stubByUrl(`/wiki/Special:EntityData/${wikidataId}.json`, {
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
    this.stubByPathAndQuery(`/w/api.php`, {action: "wbsearchentities"}, {
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

  private stubByUrl(url: string, data: unknown) {
    return this.stub(new DefaultStub(url, HttpMethod.GET, data, 200));
  }

  private stubByPathAndQuery(path: string, query: Record<string, string>, data: unknown) {
    return this.stub(new DefaultStub(path, HttpMethod.GET, data, 200)
    .withPredicate(new FlexiPredicate().withPath(path).withQuery(query).withMethod(HttpMethod.GET)))
  }

  private async stub(stub: Stub) {
    const mb = new Mountebank()
    await mb.deleteImposter(this.imposter.port)
    this.imposter.withStub(stub)
    await mb.createImposter(this.imposter)
  }

}

export default WikidataServiceTester
