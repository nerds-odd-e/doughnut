/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import { Given, Then, When } from '@badeball/cypress-cucumber-preprocessor'
import type { DataTable } from '@cucumber/cucumber'
import start from '../start'

When('I assimilate these in sequence:', (data: DataTable) => {
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

Given(
  'the note {string} has assimilated property {string}',
  (noteTitle: string, propertyKey: string) => {
    start.testability().assimilateNoteProperty(noteTitle, propertyKey)
  }
)

When('I start assimilation from the menu', () => {
  start.assimilation().startAssimilationFromMenu()
})

When(
  'I start assimilation from the menu while the next note loads slowly',
  () => {
    start.assimilation().startAssimilationFromMenuWhileNextNoteLoadsSlowly()
  }
)

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

When('I assimilate the note {string}', (noteTitle: string) => {
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

When(
  'I set the level of {string} to be {int}',
  (noteTopology: string, level: number) => {
    start.assumeNotePage(noteTopology).setLevel(level)
  }
)

Then('I should see the refinement layout:', (data: DataTable) => {
  start.assumeAssimilationPage().expectRefinementLayout(data.hashes())
})

Then('no refinement layout points should be selected', () => {
  start.assumeAssimilationPage().expectNoRefinementLayoutSelection()
})

When(
  'I remove refinement layout items {int} and {int}',
  (index1: number, index2: number) => {
    start
      .assumeAssimilationPage()
      .removeRefinementLayoutItemsAt([index1, index2])
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

Then('assimilate should be disabled', () => {
  start.assumeAssimilationPage().expectAssimilateDisabled()
})

Then('assimilate should be enabled', () => {
  start.assumeAssimilationPage().expectAssimilateEnabled()
})

When('I jump to the note page of {string}', (noteTitle: string) => {
  start.jumpToNotePage(noteTitle)
})
