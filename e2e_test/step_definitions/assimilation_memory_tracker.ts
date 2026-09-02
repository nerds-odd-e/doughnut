/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import { Then, When } from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'
import { waitUntilAppIsNotBusy } from '../start/pageBase'
import { noteIdFromUrl } from '../start/noteIdFromUrl'
import { assumeMemoryTrackerPage } from '../start/pageObjects/memoryTrackerPage'
import { followNoteUnderQuestion } from '../start/pageObjects/noteUnderQuestion'
import router from '../start/router'

/**
 * Reads a note-level tracker's recall count on its own tracker page, then
 * returns to the note the caller came from (by note id, not `cy.go('back')`
 * — the click into the tracker page is a same-origin router-link, but
 * navigating back deterministically by named route is more robust for
 * subsequent steps that read state from the current note's URL).
 */
function readNoteLevelRecallCountAndReturn(
  kind: 'understanding' | 'spelling',
  count: number
) {
  cy.url().then((url) => {
    const noteId = noteIdFromUrl(url)
    start
      .assumeAssimilationPage()
      .openNoteLevelMemoryTracker(kind)
      .expectRecallCount(count)
    router().push('noteShow', { noteId })
    waitUntilAppIsNotBusy()
  })
}

Then(
  'the note memory tracker should have recall count {int}',
  (count: number) => {
    readNoteLevelRecallCountAndReturn('understanding', count)
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
        .openAssimilationSettings()
        .openNoteLevelMemoryTracker('spelling')
        .expectRecallCount(0)
    } else {
      const errorMessage = expectedResult.replace(/^error: /, '')
      start.assumeAssimilationPage().expectSpellingErrorMessage(errorMessage)
    }
  }
)
