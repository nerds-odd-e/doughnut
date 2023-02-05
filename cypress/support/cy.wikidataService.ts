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

Cypress.Commands.add(
  "stubWikidataEntityLocation",
  { prevSubject: true },
  (wikidataServiceTester: WikidataServiceTester, wikidataId: string, lat: number, lng: number) => {
    wikidataServiceTester.stubWikidataEntityLocation(wikidataId, lat, lng)
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
    wikidataServiceTester.stubWikidataEntityPerson(wikidataId, countryId, birthday)
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
  "stubWikidataEntityQuery",
  { prevSubject: true },
  (
    wikidataServiceTester: WikidataServiceTester,
    wikidataId: string,
    wikidataTitle: string,
    wikipediaLink: string,
  ) => {
    wikidataServiceTester.stubWikidataEntityQuery(wikidataId, wikidataTitle, wikipediaLink)
  },
)

Cypress.Commands.add(
  "stubWikidataSearchResult",
  { prevSubject: true },
  (wikidataServiceTester: WikidataServiceTester, wikidataLabel: string, wikidataId: string) => {
    wikidataServiceTester.stubWikidataSearchResult(wikidataLabel, wikidataId)
  },
)
