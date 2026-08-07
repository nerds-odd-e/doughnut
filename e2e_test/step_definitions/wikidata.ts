/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import { Given, Then, When } from '@badeball/cypress-cucumber-preprocessor'
import start, { mock_services } from '../start'
import noteCreationForm from '../start/pageObjects/forms/noteCreationForm'
import { assumeAssociateWikidataDialog } from '../start/pageObjects/associateWikidataDialog'

When(
  'I associate the note {string} with Wikidata ID {string}',
  (title: string, wikidataId: string) => {
    const page = start.jumpToNotePage(title)
    page.associateWikidataDialog().associate(wikidataId)
    page.flushPendingContentSave()
  }
)

When(
  'I confirm the association using the suggested title {string}',
  (suggestedTitle: string) => {
    assumeAssociateWikidataDialog().confirmAssociationWithSuggestedTitle(
      suggestedTitle
    )
    start.assumeNotePage(suggestedTitle).flushPendingContentSave()
  }
)

Given(
  'Wikidata.org has an entity {string} with label {string} and link to wikipedia {string}',
  (wikidataId: string, wikidataTitle: string, wikipediaLink: string) => {
    mock_services
      .wikidata()
      .stubWikidataEntityQuery(wikidataId, wikidataTitle, wikipediaLink)
  }
)

Given(
  'Wikidata.org has an entity {string} with label {string}',
  (wikidataId: string, wikidataTitle: string) => {
    mock_services
      .wikidata()
      .stubWikidataEntityQuery(wikidataId, wikidataTitle, undefined)
  }
)

Given(
  'Wikidata.org entity {string} is a person from {string} and birthday is {string}',
  (wikidataId: string, countryId: string, birthday: string) => {
    mock_services
      .wikidata()
      .stubWikidataEntityPerson(wikidataId, countryId, birthday)
  }
)

Given(
  'Wikidata.org entity {string} is a location at {float}, {float}',
  (wikidataId: string, lat: number, lng: number) => {
    mock_services.wikidata().stubWikidataEntityLocation(wikidataId, lat, lng)
  }
)

Given('The Wikidata service is not available', () => {
  // The service should be mocked, but no stubbing is done
})

Then(
  'I should see the error {string} on Wikidata ID when creating a note',
  (message: string) => {
    noteCreationForm.wikidataSearch().expectErrorOnWikidataId(message)
  }
)

Then(
  'I should see the error {string} on Wikidata ID when associating',
  (message: string) => {
    assumeAssociateWikidataDialog().expectErrorOnWikidataId(message)
  }
)

Then(
  'the Wikidata association of note {string} should link to {string}',
  (noteTitle: string, associationUrl: string) => {
    start
      .jumpToNotePage(noteTitle, true)
      .expectWikidataBrowseLinkOpensUrl(associationUrl)
  }
)

Then(
  'the Wikidata association on the current note should link to {string}',
  (associationUrl: string) => {
    start.assumeNotePage().expectWikidataBrowseLinkOpensUrl(associationUrl)
  }
)

Given(
  'Wikidata search result always has {string} with ID {string}',
  (wikidataLabel: string, wikidataId: string) => {
    mock_services.wikidata().stubWikidataSearchResult(wikidataLabel, wikidataId)
  }
)

When('I search Wikidata for {string}', (phrase: string) => {
  noteCreationForm.searchWikidata(phrase)
})

When(
  'I select Wikidata ID {string} from the search results',
  (wikidataId: string) => {
    assumeAssociateWikidataDialog().selectResult(wikidataId)
  }
)

Then('the note creation Title should be {string}', (value: string) => {
  start.form.getField('Title').shouldHaveValue(value)
})

Then('the note creation Wikidata ID should be {string}', (value: string) => {
  noteCreationForm.wikidataSearch().expectWikidataIdValue(value)
})
