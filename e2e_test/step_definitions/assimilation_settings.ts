/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import { Given, Then, When } from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'

Given(
  'I am viewing assimilation settings for note {string}',
  (noteTitle: string) => {
    start.jumpToNotePage(noteTitle).moreOptions().openAssimilationSettings()
  }
)

When('I am assimilating the note {string}', (noteTitle: string) => {
  start.jumpToNotePage(noteTitle).moreOptions().openAssimilationSettings()
})

When('I open assimilation settings', () => {
  start.assumeNotePage().moreOptions().openAssimilationSettings()
})

When('I assimilate on the assimilation panel', () => {
  start.assumeAssimilationPage().assimilateOnPanel()
})

When('I assimilate it as commissioned', () => {
  start.assumeAssimilationPage().assimilateAsCommissioned()
})

When('I remember spelling', () => {
  start.assumeAssimilationPage().rememberSpelling()
})

When('I skip on the assimilation panel', () => {
  start.assumeAssimilationPage().skipOnPanel()
})

When('I return to sequence on the assimilation panel', () => {
  start.assumeAssimilationPage().returnToSequenceOnPanel()
})

Then(
  'assimilate for property {string} should be disabled',
  (propertyKey: string) => {
    start.assumeNotePage().expectRichNotePropertyAssimilateDisabled(propertyKey)
  }
)

Then(
  'assimilate for property {string} should be enabled',
  (propertyKey: string) => {
    start.assumeNotePage().expectRichNotePropertyAssimilateEnabled(propertyKey)
  }
)

Then('I should see Skip for property {string}', (propertyKey: string) => {
  start.assumeNotePage().expectRichNotePropertyPanelAction(propertyKey, 'skip')
})

Then(
  'I should see Return to sequence for property {string}',
  (propertyKey: string) => {
    start
      .assumeNotePage()
      .expectRichNotePropertyPanelAction(propertyKey, 'return-to-sequence')
  }
)

Then(
  'I should see Remove from recall for property {string}',
  (propertyKey: string) => {
    start
      .assumeNotePage()
      .expectRichNotePropertyPanelAction(propertyKey, 'remove-from-recall')
  }
)

Then('I should see Revive for property {string}', (propertyKey: string) => {
  start
    .assumeNotePage()
    .expectRichNotePropertyPanelAction(propertyKey, 'revive')
})

Then('I should see Skip on the assimilation panel', () => {
  start.assumeAssimilationPage().expectSkipOnPanel()
})

Then('I should see Return to sequence on the assimilation panel', () => {
  start.assumeAssimilationPage().expectReturnToSequenceOnPanel()
})

When('I remove from recall on the assimilation panel', () => {
  start.assumeAssimilationPage().removeFromRecallOnPanel()
})

Then('I should see Remove from recall on the assimilation panel', () => {
  start.assumeAssimilationPage().expectRemoveFromRecallOnPanel()
})

Then('I should see Revive on the assimilation panel', () => {
  start.assumeAssimilationPage().expectReviveOnPanel()
})
