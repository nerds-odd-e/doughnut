/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import { Then, When } from '@badeball/cypress-cucumber-preprocessor'
import type { DataTable } from '@cucumber/cucumber'
import start from '../start'

Then('I assimilate these in sequence:', (data: DataTable) => {
  start.assimilation().navigateToAssimilationPage().assimilate(data.hashes())
})

Then('I should see {int} due for assimilation', (numberOfNotes: number) => {
  start.assimilation().expectCount(numberOfNotes)
})

Then('I assimilate {string}', (noteTopology: string) => {
  start
    .assimilation()
    .navigateToAssimilationPage()
    .assimilateNotes(noteTopology)
})

Then(
  'I added and learned one note {string} on day {int}',
  (noteTopology: string, day: number) => {
    cy.get<string>('@currentLoginUser').then((username) => {
      start.testability().injectNotes([{ Title: noteTopology }], username)
    })
    start.testability().backendTimeTravelTo(day, 8)
    start
      .assimilation()
      .navigateToAssimilationPage()
      .assimilateNotes(noteTopology)
  }
)

Then(
  'I assimilated one note {string} on day {int}',
  (noteTopology: string, day: number) => {
    start.testability().backendTimeTravelTo(day, 8)
    start
      .assimilation()
      .navigateToAssimilationPage()
      .assimilateNotes(noteTopology)
  }
)

Then('I am assimilating new note on day {int}', (day: number) => {
  start.testability().backendTimeTravelTo(day, 8)
  start.assimilation().navigateToAssimilationPage()
})

When('I am assimilating the note {string}', (noteTitle: string) => {
  start.jumpToNotePage(noteTitle).moreOptions().openAssimilationPage()
})

Then('I assimilate the note {string}', (noteTitle: string) => {
  start.jumpToNotePage(noteTitle).moreOptions().assimilateNote()
})

Then(
  'I assimilate the note {string} with the option of remembering spelling',
  (noteTitle: string) => {
    start
      .jumpToNotePage(noteTitle)
      .moreOptions()
      .openAssimilationPage()
      .assimilateWithSpellingOption()
  }
)

Then(
  'I set the level of {string} to be {int}',
  (noteTopology: string, level: number) => {
    start.assumeNotePage(noteTopology).setLevel(level)
  }
)

Then(
  'I should see an understanding checklist with {int} points',
  (count: number) => {
    start.assumeAssimilationPage().expectUnderstandingPointsCount(count)
  }
)

When(
  'I delete understanding points {int} and {int}',
  (index1: number, index2: number) => {
    start.assumeAssimilationPage().deleteUnderstandingPointsAt([index1, index2])
  }
)

When(
  'I ignore these understanding points and complete assimilation:',
  (data: DataTable) => {
    const pointTexts = data
      .raw()
      .map((row) => row[0])
      .filter((s): s is string => s !== undefined)
    start
      .assumeAssimilationPage()
      .ignoreUnderstandingPointsAndComplete(pointTexts)
  }
)

Then('remembering spelling should be unavailable', () => {
  start.assumeAssimilationPage().expectRememberingSpellingUnavailable()
})

Then('remembering spelling should be available', () => {
  start.assumeAssimilationPage().expectRememberingSpellingAvailable()
})

When('I keep for repetition with remembering spelling', () => {
  start.assumeAssimilationPage().proceedWithRememberingSpelling()
})

When('I verify spelling with {string}', (text: string) => {
  start.assumeAssimilationPage().verifySpellingWith(text)
})

Then(
  'the spelling verification result for note {string} should be {string}',
  (noteTitle: string, expectedResult: string) => {
    if (expectedResult === 'success') {
      start.assumeAssimilationPage().expectPopupClosed()
      start
        .jumpToNotePage(noteTitle)
        .moreOptions()
        .expectMemoryTrackerInfo([
          { type: 'spelling', 'Repetition Count': '0' },
        ])
    } else {
      const errorMessage = expectedResult.replace(/^error: /, '')
      start.assumeAssimilationPage().expectSpellingErrorMessage(errorMessage)
    }
  }
)
