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

When('I start assimilation from the menu and observe blocking loading', () => {
  start.assimilation().startAssimilationFromMenuAndObserveBlockingLoading()
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

Then('I should see the refinement layout:', (data: DataTable) => {
  start.assumeAssimilationPage().expectRefinementLayout(data.hashes())
})

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

When('I assimilate with remembering spelling', () => {
  start.assumeAssimilationPage().proceedWithRememberingSpelling()
})

When('I add remember spelling to the note {string}', (noteTitle: string) => {
  start.jumpToNotePage(noteTitle).setRememberSpelling()
})

When('I verify spelling with {string}', (text: string) => {
  start.assumeAssimilationPage().verifySpellingWith(text)
})

Then('the assimilate button should be disabled', () => {
  start.assumeAssimilationPage().expectAssimilateDisabled()
})

Then('the assimilate button should be enabled', () => {
  start.assumeAssimilationPage().expectAssimilateEnabled()
})

When('I jump to the note page of {string}', (noteTitle: string) => {
  start.jumpToNotePage(noteTitle)
})

When('I open assimilation settings from more options', () => {
  start.assumeNotePage().moreOptions().openAssimilationSettings()
})

When('I assimilate on the assimilation panel', () => {
  start.assumeAssimilationPage().assimilateOnPanel()
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

When(
  'I revive recall for property {string} on the assimilation settings panel',
  (propertyKey: string) => {
    start.assumeAssimilationPage().reviveRecallProperty(propertyKey)
  }
)

When('I revive recall on the assimilation panel', () => {
  start.assumeAssimilationPage().reviveRecallOnPanel()
})

Then(
  'assimilate for property {string} should be disabled',
  (propertyKey: string) => {
    start.assumeAssimilationPage().expectPropertyAssimilateDisabled(propertyKey)
  }
)

Then(
  'assimilate for property {string} should be enabled',
  (propertyKey: string) => {
    start.assumeAssimilationPage().expectPropertyAssimilateEnabled(propertyKey)
  }
)

Then(
  'I should see Revive for property {string} on the assimilation settings panel',
  (propertyKey: string) => {
    start.assumeAssimilationPage().expectReviveForProperty(propertyKey)
  }
)

Then(
  'I should see Skip recall for property {string} on the assimilation settings panel',
  (propertyKey: string) => {
    start.assumeAssimilationPage().expectSkipRecallForProperty(propertyKey)
  }
)

Then('I should see Revive on the assimilation panel', () => {
  start.assumeAssimilationPage().expectReviveOnPanel()
})

Then('I should see Skip recall on the assimilation panel', () => {
  start.assumeAssimilationPage().expectSkipRecallOnPanel()
})
