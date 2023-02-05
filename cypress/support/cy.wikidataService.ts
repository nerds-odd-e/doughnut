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
    wikidataServiceTester.stubWikidataApi(
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
    wikidataServiceTester.stubWikidataEntityBook(wikidataId, authorWikidataId)
  },
)

Cypress.Commands.add(
  "stubWikidataSearchResult",
  { prevSubject: true },
  (wikidataServiceTester: WikidataServiceTester, wikidataLabel: string, wikidataId: string) => {
    wikidataServiceTester.stubWikidataSearchResult(wikidataLabel, wikidataId)
  },
)
