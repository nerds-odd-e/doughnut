/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import { Given, Then, When } from '@badeball/cypress-cucumber-preprocessor'
import type { DataTable } from '@cucumber/cucumber'
import start from '../start'

Then('I assimilate these in sequence:', (data: DataTable) => {
  start.assimilation().startAssimilationFromMenu()
  start.assumeAssimilationPage().assimilate(data.hashes())
})

Then('I should see {int} due for assimilation', (numberOfNotes: number) => {
  start.assimilation().expectCount(numberOfNotes)
})

Then(
  'I added and learned one note {string} on day {int}',
  (noteTopology: string, day: number) => {
    cy.get<string>('@currentLoginUser').then((username) => {
      start
        .testability()
        .injectNotes([{ Title: noteTopology }], username, noteTopology)
    })
    start.testability().backendTimeTravelTo(day, 8)
    start.testability().assimilateNote(noteTopology)
  }
)

Then(
  'I assimilated one note {string} on day {int}',
  (noteTopology: string, day: number) => {
    start.testability().backendTimeTravelTo(day, 8)
    start.testability().assimilateNote(noteTopology)
  }
)

Given(
  'the note {string} was assimilated on day {int}',
  (noteTitle: string, day: number) => {
    start.testability().backendTimeTravelTo(day, 8)
    start.testability().assimilateNote(noteTitle)
  }
)

When('I am assimilating the note {string}', (noteTitle: string) => {
  start.jumpToNotePage(noteTitle).moreOptions().openAssimilationSettings()
})

When('I start assimilation from the menu', () => {
  start.assimilation().startAssimilationFromMenu()
})

Then('I should be assimilating the note {string}', (noteTitle: string) => {
  start.assumeAssimilationPage().expectAssimilatingNote(noteTitle)
})

Then('I should see assimilation progress {string}', (triple: string) => {
  start.assumeAssimilationPage().expectAssimilationProgressSummary(triple)
})

Then('I should see the daily assimilation goal toast', () => {
  start.assimilation().expectDailyAssimilationGoalToast()
})

Then('I should see the no more notes to assimilate toast', () => {
  start.assimilation().expectNoMoreNotesToAssimilateToast()
})

Then('I should still be on the note page for {string}', (noteTitle: string) => {
  start.assumeNotePage(noteTitle).expectWithoutAssimilationPanel()
})

Then('I should see assimilation menu progress', () => {
  start.assimilation().expectAssimilationMenuProgress()
})

Then('I assimilate the note {string} via more options', (noteTitle: string) => {
  start.jumpToNotePage(noteTitle).moreOptions().assimilateNote()
})

Then(
  'I assimilate the note {string} with the option of remembering spelling',
  (noteTitle: string) => {
    start
      .jumpToNotePage(noteTitle)
      .moreOptions()
      .openAssimilationSettings()
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
  'I should see refinement suggestions with {int} items',
  (count: number) => {
    start.assumeAssimilationPage().expectRefinementSuggestionsCount(count)
  }
)

When(
  'I remove refinement suggestions {int} and {int}',
  (index1: number, index2: number) => {
    start
      .assumeAssimilationPage()
      .removeRefinementSuggestionsAt([index1, index2])
  }
)

Then('remembering spelling should be unavailable', () => {
  start.assumeAssimilationPage().expectRememberingSpellingUnavailable()
})

Then('remembering spelling should be available', () => {
  start.assumeAssimilationPage().expectRememberingSpellingAvailable()
})

When('I keep for recall with remembering spelling', () => {
  start.assumeAssimilationPage().proceedWithRememberingSpelling()
})

When('I add remember spelling to the note {string}', (noteTitle: string) => {
  start.jumpToNotePage(noteTitle).setRememberSpelling()
})

When('I verify spelling with {string}', (text: string) => {
  start.assumeAssimilationPage().verifySpellingWith(text)
})

Then('the keep for recall button should be disabled', () => {
  start.assumeAssimilationPage().expectKeepForRecallDisabled()
})

When('I jump to the note page of {string}', (noteTitle: string) => {
  start.jumpToNotePage(noteTitle)
})

When('I open assimilation settings from more options', () => {
  start.assumeNotePage().moreOptions().openAssimilationSettings()
})

When('I keep for recall on the assimilation panel', () => {
  start.assumeAssimilationPage().keepForRecallOnPanel()
})

When('I skip recall on the assimilation panel', () => {
  start.assumeAssimilationPage().skipRecallOnPanel()
})

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
