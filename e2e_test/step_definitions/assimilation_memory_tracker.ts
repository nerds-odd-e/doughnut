/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import { Then, When } from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'
import { assumeMemoryTrackerPage } from '../start/pageObjects/memoryTrackerPage'
import { followNoteUnderQuestion } from '../start/pageObjects/noteUnderQuestion'

Then(
  'the note memory tracker should have recall count {int}',
  (count: number) => {
    start
      .assumeAssimilationPage()
      .expectNoteLevelMemoryTrackerRecallCount('understanding', count)
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
  'I should see a property memory tracker for {string}',
  (propertyKey: string) => {
    start.assumeAssimilationPage().expectPropertyMemoryTracker(propertyKey)
  }
)

Then(
  'I should see ordinary and commissioned memory trackers for {string}',
  (_noteTitle: string) => {
    start.assumeAssimilationPage().expectOrdinaryAndCommissionedMemoryTrackers()
  }
)

Then('I should see a spelling memory tracker', () => {
  start.assumeAssimilationPage().expectSpellingMemoryTracker()
})

Then(
  'the property memory tracker for {string} should be absent',
  (propertyKey: string) => {
    start
      .assumeAssimilationPage()
      .expectPropertyMemoryTrackerAbsent(propertyKey)
  }
)

When(
  'I open the property memory tracker for {string}',
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

When('I follow the note under question {string}', (noteTitle: string) => {
  followNoteUnderQuestion(noteTitle)
})

Then(
  'the spelling verification result for note {string} should be {string}',
  (noteTitle: string, expectedResult: string) => {
    if (expectedResult === 'success') {
      start.assumeAssimilationPage().expectPopupClosed()
      start
        .jumpToNotePage(noteTitle, true)
        .openAssimilationPanel()
        .openNoteLevelMemoryTracker('spelling')
        .expectRecallCount(0)
    } else {
      const errorMessage = expectedResult.replace(/^error: /, '')
      start.assumeAssimilationPage().expectSpellingErrorMessage(errorMessage)
    }
  }
)
