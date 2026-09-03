/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import { Given, Then, When } from '@badeball/cypress-cucumber-preprocessor'
import type { DataTable } from '@cucumber/cucumber'
import { commonSenseSplit } from 'support/string_util'
import start from '../start'

When('I assimilate these in sequence:', (data: DataTable) => {
  start.assimilation().startAssimilationFromMenu()
  start.assumeAssimilationPage().assimilate(data.hashes())
})

Then(
  'I assimilated one note {string} at the current time',
  (noteTitle: string) => {
    start.testability().assimilateNote(noteTitle)
  }
)

function assimilateTitlesOnDay(
  noteTitles: string,
  day: number,
  assimilateTitle: (title: string) => void
) {
  start.testability().backendTimeTravelTo(day, 8)
  commonSenseSplit(noteTitles, ', ').forEach(assimilateTitle)
}

Given(
  'the note {string} was assimilated on day {int}',
  (noteTitle: string, day: number) => {
    assimilateTitlesOnDay(noteTitle, day, (title) =>
      start.testability().assimilateNote(title)
    )
  }
)

Given(
  'the notes {string} are assimilated on day {int}',
  (noteTitles: string, day: number) => {
    assimilateTitlesOnDay(noteTitles, day, (title) =>
      start.testability().assimilateNote(title)
    )
  }
)

Given(
  'the note {string} was assimilated as spelling on day {int}',
  (noteTitle: string, day: number) => {
    assimilateTitlesOnDay(noteTitle, day, (title) =>
      start.testability().assimilateNoteAsSpelling(title)
    )
  }
)

Given(
  'the notes {string} are skipped from the assimilation sequence',
  (noteTitles: string) => {
    commonSenseSplit(noteTitles, ', ').forEach((title) => {
      start.testability().skipNoteFromAssimilationSequence(title)
    })
  }
)

Given(
  'the notes {string} are assimilated as commissioned on day {int}',
  (noteTitles: string, day: number) => {
    assimilateTitlesOnDay(noteTitles, day, (title) =>
      start.testability().assimilateNoteAsCommissioned(title)
    )
  }
)

Given(
  'the notes {string} in notebook {string} are assimilated as commissioned on day {int}',
  (noteTitles: string, _notebookTitle: string, day: number) => {
    assimilateTitlesOnDay(noteTitles, day, (title) =>
      start.testability().assimilateNoteAsCommissioned(title)
    )
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

Then('I should see assimilation progress {string}', (dueOverTotal: string) => {
  start.assimilation().expectAssimilationNavBadge(dueOverTotal)
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

When('I assimilate the note {string}', (noteTitle: string) => {
  start.jumpToNotePage(noteTitle).moreOptions().assimilateNote()
})

Given(
  'I assimilate the note {string} with the option of remembering spelling',
  (noteTitle: string) => {
    start.jumpToNotePage(noteTitle).moreOptions().openAssimilationPanel()
    start.assumeAssimilationPage().waitForAssimilationReady().rememberSpelling()
    start.assumeAssimilationPage().verifySpellingWith(noteTitle)
    start.assumeAssimilationPage().expectPopupClosed()
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

Then('no refinement layout items should be selected', () => {
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
