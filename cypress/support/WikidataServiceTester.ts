import { Stub } from "@anev/ts-mountebank"
/// <reference types="cypress" />

import request from "superagent"
import { Mountebank, Imposter, DefaultStub, HttpMethod, FlexiPredicate } from "@anev/ts-mountebank"
import WikidataEntitiesBuilder, { Claim } from "./json/WikidataEntitiesBuilder"
import TestabilityHelper from "./TestabilityHelper"

// @ts-check

class MountebankWrapper {
  mountebank = new Mountebank()
  port: number

  constructor(port: number) {
    this.port = port
  }

  private async tryDeleteImposter(): Promise<void> {
    try {
      // just try to delete in case an imposter is there
      await this.mountebank.deleteImposter(this.port)
    } catch (error) {} // eslint-disable-line
  }

  public async createImposter(): Promise<void> {
    const imposter = new Imposter().withPort(this.port)
    this.tryDeleteImposter()
    const response = await request
      .post(`${this.mountebank.mountebankUrl}/imposters`)
      .send(JSON.stringify(imposter))

    if (response.statusCode != 201)
      throw new Error(`Problem creating imposter: ${JSON.stringify(response?.error)}`)
  }

  public async addStubToImposter(stub: Stub): Promise<void> {
    const response = await request
      .post(`${this.mountebank.mountebankUrl}/imposters/${this.port}/stubs`)
      .send(JSON.stringify({ stub }))

    if (response.statusCode != 200)
      throw new Error(`Problem adding stub to imposter: ${JSON.stringify(response?.error)}`)
  }
}

class WikidataServiceTester {
  port = 5001
  mountebank = new MountebankWrapper(this.port)

  mock(cy: Cypress.cy & CyEventEmitter) {
    this.mountebank.createImposter()
    this.setWikidataServiceUrl(cy, `http://localhost:${this.port}`).as(this.savedServiceUrlName)
  }

  restore(cy: Cypress.cy & CyEventEmitter) {
    cy.get("@savedWikidataServiceUrl")
    cy.get(`@${this.savedServiceUrlName}`).then((saved) =>
      this.setWikidataServiceUrl(cy, saved as unknown as string),
    )
  }

  async stubWikidataEntityQuery(wikidataId: string, wikidataTitle: string, wikipediaLink: string) {
    const wikipedia = wikipediaLink ? { enwiki: { site: "enwiki", url: wikipediaLink } } : {}
    return await this.stubByUrl(`/wiki/Special:EntityData/${wikidataId}.json`, {
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

  async stubWikidataEntity(wikidataId: string, claims: Claim[]) {
    return await this.stubWikidataApi(
      "wbgetentities",
      { ids: wikidataId },
      new WikidataEntitiesBuilder(wikidataId).wclaims(claims).build(),
    )
  }

  async stubWikidataEntityLocation(wikidataId: string, latitude: number, longitude: number) {
    return await this.stubWikidataEntity(wikidataId, [
      { claimId: "P625", type: "globecoordinate", value: { latitude, longitude } },
    ])
  }

  async stubWikidataEntityPerson(wikidataId: string, countryId: string, birthday: string) {
    return await this.stubWikidataEntity(wikidataId, [
      { claimId: "P31", type: "wikibase-entityid", value: { id: "Q5" } },
      { claimId: "P569", type: "time", value: { time: birthday } },
      { claimId: "P27", type: "wikibase-entityid", value: { id: countryId } },
    ])
  }

  async stubWikidataEntityBook(wikidataId: string, authorWikidataId: string) {
    return await this.stubWikidataEntity(wikidataId, [
      { claimId: "P50", type: "wikibase-entityid", value: { id: authorWikidataId } },
    ])
  }

  async stubWikidataSearchResult(wikidataLabel: string, wikidataId: string) {
    return await this.stubWikidataApi(
      "wbsearchentities",
      {},
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
    )
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
    return this.stub(new DefaultStub(url, HttpMethod.GET, data, 200))
  }

  private stubWikidataApi(action: string, query: Record<string, string>, data: unknown) {
    const path = `/w/api.php`
    return this.stub(
      new DefaultStub(path, HttpMethod.GET, data, 200).withPredicate(
        new FlexiPredicate()
          .withPath(path)
          .withQuery({ action, ...query })
          .withMethod(HttpMethod.GET),
      ),
    )
  }

  // each stub step in Gherkin will call mountebank API again,
  // however only the last step will take affect.
  // But we have to wait until the previous stubs are done.
  // Alternatively, we can combine all stubs in one step, or have an additional step.
  private async stub(stub: Stub) {
    this.mountebank.addStubToImposter(stub)
  }
}

export default WikidataServiceTester
