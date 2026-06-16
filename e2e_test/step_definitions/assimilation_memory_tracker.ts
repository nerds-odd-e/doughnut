/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import { Then, When } from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'
import { assumeMemoryTrackerPage } from '../start/pageObjects/memoryTrackerPage'

Then(
  'the note memory tracker should have recall count {int}',
  (count: number) => {
    start
      .assumeAssimilationPage()
      .expectMemoryTrackerInfo([
        { type: 'normal', 'Recall Count': String(count) },
      ])
  }
)

Then(
  'the property memory tracker for {string} should have recall count {int}',
  (propertyKey: string, count: number) => {
    start
      .assumeAssimilationPage()
      .expectPropertyMemoryTracker(propertyKey, count)
  }
)

Then(
  'I should see a property memory tracker for {string} on the assimilation settings panel',
  (propertyKey: string) => {
    start.assumeAssimilationPage().expectPropertyMemoryTracker(propertyKey)
  }
)

Then(
  'the property memory tracker for {string} should be absent on the assimilation settings panel',
  (propertyKey: string) => {
    start
      .assumeAssimilationPage()
      .expectPropertyMemoryTrackerAbsent(propertyKey)
  }
)

When(
  'I open the property memory tracker for {string} from the assimilation settings panel',
  (propertyKey: string) => {
    start.assumeAssimilationPage().openPropertyMemoryTracker(propertyKey)
  }
)

Then(
  'I should see note {string} on the memory tracker page',
  (noteTitle: string) => {
    assumeMemoryTrackerPage().expectNoteTitle(noteTitle)
  }
)

Then(
  'I should see focused property {string} on the memory tracker page',
  (propertyKey: string) => {
    assumeMemoryTrackerPage().expectFocusedProperty(propertyKey)
  }
)

Then(
  'the spelling verification result for note {string} should be {string}',
  (noteTitle: string, expectedResult: string) => {
    if (expectedResult === 'success') {
      start.assumeAssimilationPage().expectPopupClosed()
      start
        .jumpToNotePage(noteTitle, true)
        .openAssimilationSettings()
        .expectMemoryTrackerInfo([{ type: 'spelling', 'Recall Count': '0' }])
    } else {
      const errorMessage = expectedResult.replace(/^error: /, '')
      start.assumeAssimilationPage().expectSpellingErrorMessage(errorMessage)
    }
  }
)
