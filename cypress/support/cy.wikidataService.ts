// ***********************************************
// custom commands and overwrite existing commands.
//
// For more comprehensive examples of custom
// commands please read more here:
// https://on.cypress.io/custom-commands
// ***********************************************
//
//
// -- This is a parent command --
// Cypress.Commands.add("login", (email, password) => { ... })
//
//
// -- This is a child command --
// Cypress.Commands.add("drag", { prevSubject: 'element'}, (subject, options) => { ... })
//
//
// -- This is a dual command --
// Cypress.Commands.add("dismiss", { prevSubject: 'optional'}, (subject, options) => { ... })
//
//
// -- This will overwrite an existing command --
// Cypress.Commands.overwrite("visit", (originalFn, url, options) => { ... })

/// <reference types="cypress" />
// @ts-check
import "@testing-library/cypress/add-commands"
import "cypress-file-upload"
import "./string.extensions"
import WikidataEntitiesBuilder, { Claim } from "./json/WikidataEntitiesBuilder"
import ServiceMocker from "./ServiceMocker"

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

Cypress.Commands.add(
  "stubWikidataEntityQuery",
  { prevSubject: true },
  (
    serviceMocker: ServiceMocker,
    wikidataId: string,
    wikidataTitle: string,
    wikipediaLink: string,
  ) => {
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
)

Cypress.Commands.add(
  "stubWikidataEntityLocation",
  { prevSubject: true },
  (serviceMocker: ServiceMocker, wikidataId: string, latitude: number, longitude: number) => {
    stubWikidataEntity(serviceMocker, wikidataId, [
      { claimId: "P625", type: "globecoordinate", value: { latitude, longitude } },
    ])
  },
)

Cypress.Commands.add(
  "stubWikidataEntityPerson",
  { prevSubject: true },
  (serviceMocker: ServiceMocker, wikidataId: string, countryId: string, birthday: string) => {
    stubWikidataEntity(serviceMocker, wikidataId, [
      { claimId: "P31", type: "wikibase-entityid", value: { id: "Q5" } },
      { claimId: "P569", type: "time", value: { time: birthday } },
      { claimId: "P27", type: "wikibase-entityid", value: { id: countryId } },
    ])
  },
)

Cypress.Commands.add(
  "stubWikidataEntityBook",
  { prevSubject: true },
  (serviceMocker: ServiceMocker, wikidataId: string, authorWikidataId: string) => {
    stubWikidataEntity(serviceMocker, wikidataId, [
      { claimId: "P50", type: "wikibase-entityid", value: { id: authorWikidataId } },
    ])
  },
)

Cypress.Commands.add(
  "stubWikidataSearchResult",
  { prevSubject: true },
  (serviceMocker: ServiceMocker, wikidataLabel: string, wikidataId: string) => {
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
)
