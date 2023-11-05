import testability from "start/testability"
import ServiceMocker from "../../support/ServiceMocker"
import WikidataEntitiesBuilder, { Claim } from "../../support/json/WikidataEntitiesBuilder"

const stubWikidataApi = (
  serviceMocker: ServiceMocker,
  action: string,
  query: Record<string, string>,
  data: unknown,
) => {
  return serviceMocker.stubGetter(`/w/api.php`, { action, ...query }, data)
}

const stubWikidataEntity = (serviceMocker: ServiceMocker, wikidataId: string, claims: Claim[]) => {
  stubWikidataApi(
    serviceMocker,
    "wbgetentities",
    { ids: wikidataId },
    new WikidataEntitiesBuilder(wikidataId).wclaims(claims).build(),
  )
}

const wikidataService = () => {
  const serviceMocker = new ServiceMocker("wikidata", 5001)
  return {
    mock() {
      testability().mock(serviceMocker)
    },
    restore() {
      testability().restore(serviceMocker)
    },

    stubWikidataEntityQuery(wikidataId: string, wikidataTitle: string, wikipediaLink: string) {
      const wikipedia = wikipediaLink ? { enwiki: { site: "enwiki", url: wikipediaLink } } : {}
      serviceMocker.stubByUrl(`/wiki/Special:EntityData/${wikidataId}.json`, {
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
    },

    stubWikidataEntityLocation(wikidataId: string, latitude: number, longitude: number) {
      stubWikidataEntity(serviceMocker, wikidataId, [
        { claimId: "P625", type: "globecoordinate", value: { latitude, longitude } },
      ])
    },

    stubWikidataEntityPerson(wikidataId: string, countryId: string, birthday: string) {
      stubWikidataEntity(serviceMocker, wikidataId, [
        { claimId: "P31", type: "wikibase-entityid", value: { id: "Q5" } },
        { claimId: "P569", type: "time", value: { time: birthday } },
        { claimId: "P27", type: "wikibase-entityid", value: { id: countryId } },
      ])
    },

    stubWikidataEntityBook(wikidataId: string, authorWikidataIds: Array<string>) {
      stubWikidataEntity(
        serviceMocker,
        wikidataId,
        authorWikidataIds.map((id) => ({
          claimId: "P50",
          type: "wikibase-entityid",
          value: { id },
        })),
      )
    },

    stubWikidataSearchResult(wikidataLabel: string, wikidataId: string) {
      stubWikidataApi(
        serviceMocker,
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
    },
  }
}

export default wikidataService
