/// <reference types="cypress" />

import WikidataEntitiesBuilder, { Claim } from "./json/WikidataEntitiesBuilder"
import ServiceTester from "./ServiceTester"

class WikidataServiceTester extends ServiceTester {
  constructor() {
    super("wikidata")
  }

  async stubWikidataEntity(wikidataId: string, claims: Claim[]) {
    return await this.stubWikidataApi(
      "wbgetentities",
      { ids: wikidataId },
      new WikidataEntitiesBuilder(wikidataId).wclaims(claims).build(),
    )
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

  public stubWikidataApi(action: string, query: Record<string, string>, data: unknown) {
    return this.serviceMocker.stubGetter(`/w/api.php`, { action, ...query }, data)
  }
}

export default WikidataServiceTester
