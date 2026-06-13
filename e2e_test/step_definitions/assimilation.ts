/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import { Given, Then, When } from '@badeball/cypress-cucumber-preprocessor'
import type { DataTable } from '@cucumber/cucumber'
import { assumeMemoryTrackerPage } from '../start/pageObjects/memoryTrackerPage'
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

Then(
  'I assimilated one note {string} at the current time',
  (noteTitle: string) => {
    start.testability().assimilateNote(noteTitle)
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

Then(
  'I should see pending assimilation property {string}',
  (propertyKey: string) => {
    start
      .assumeAssimilationPage()
      .expectPendingAssimilationProperty(propertyKey)
  }
)

Then(
  'I should not see pending assimilation property {string}',
  (propertyKey: string) => {
    start
      .assumeAssimilationPage()
      .expectPendingAssimilationPropertyAbsent(propertyKey)
  }
)

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

Then('the keep for recall button should be enabled', () => {
  start.assumeAssimilationPage().expectKeepForRecallEnabled()
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

When(
  'I expand assimilation properties on the assimilation settings panel',
  () => {
    start.assumeAssimilationPage().expandAssimilationPropertiesSection()
  }
)

When(
  'I assimilate the property {string} on the assimilation settings panel',
  (propertyKey: string) => {
    start.assumeAssimilationPage().assimilateProperty(propertyKey)
  }
)

When(
  'I skip recall on property {string} on the assimilation settings panel',
  (propertyKey: string) => {
    start.assumeAssimilationPage().skipRecallProperty(propertyKey)
  }
)

Then(
  'keep for recall for property {string} should be disabled',
  (propertyKey: string) => {
    start
      .assumeAssimilationPage()
      .expectPropertyKeepForRecallDisabled(propertyKey)
  }
)

Then(
  'keep for recall for property {string} should be enabled',
  (propertyKey: string) => {
    start
      .assumeAssimilationPage()
      .expectPropertyKeepForRecallEnabled(propertyKey)
  }
)

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
