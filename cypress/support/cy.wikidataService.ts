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
import WikidataServiceTester from "./WikidataServiceTester"
import "./string.extensions"
import ServiceTester from "./ServiceTester"
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
Cypress.Commands.add(
  "stubWikidataEntityQuery",
  { prevSubject: true },
  (
    serviceTester: ServiceTester,
    wikidataId: string,
    wikidataTitle: string,
    wikipediaLink: string,
  ) => {
    const wikipedia = wikipediaLink ? { enwiki: { site: "enwiki", url: wikipediaLink } } : {}
    serviceTester.serviceMocker.stubByUrl(`/wiki/Special:EntityData/${wikidataId}.json`, {
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
  "stubWikidataEntity",
  { prevSubject: true },
  (wikidataServiceTester: WikidataServiceTester, wikidataId: string, claims: Claim[]) => {
    stubWikidataApi(
      wikidataServiceTester.serviceMocker,
      "wbgetentities",
      { ids: wikidataId },
      new WikidataEntitiesBuilder(wikidataId).wclaims(claims).build(),
    )
  },
)

Cypress.Commands.add(
  "stubWikidataEntityLocation",
  { prevSubject: true },
  (
    wikidataServiceTester: WikidataServiceTester,
    wikidataId: string,
    latitude: number,
    longitude: number,
  ) => {
    cy.wrap(wikidataServiceTester).stubWikidataEntity(wikidataId, [
      { claimId: "P625", type: "globecoordinate", value: { latitude, longitude } },
    ])
  },
)

Cypress.Commands.add(
  "stubWikidataEntityPerson",
  { prevSubject: true },
  (
    wikidataServiceTester: WikidataServiceTester,
    wikidataId: string,
    countryId: string,
    birthday: string,
  ) => {
    cy.wrap(wikidataServiceTester).stubWikidataEntity(wikidataId, [
      { claimId: "P31", type: "wikibase-entityid", value: { id: "Q5" } },
      { claimId: "P569", type: "time", value: { time: birthday } },
      { claimId: "P27", type: "wikibase-entityid", value: { id: countryId } },
    ])
  },
)

Cypress.Commands.add(
  "stubWikidataEntityBook",
  { prevSubject: true },
  (wikidataServiceTester: WikidataServiceTester, wikidataId: string, authorWikidataId: string) => {
    cy.wrap(wikidataServiceTester).stubWikidataEntity(wikidataId, [
      { claimId: "P50", type: "wikibase-entityid", value: { id: authorWikidataId } },
    ])
  },
)

Cypress.Commands.add(
  "stubWikidataSearchResult",
  { prevSubject: true },
  (wikidataServiceTester: WikidataServiceTester, wikidataLabel: string, wikidataId: string) => {
    stubWikidataApi(
      wikidataServiceTester.serviceMocker,
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
