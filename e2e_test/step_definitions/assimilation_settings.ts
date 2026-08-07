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

When('I open assimilation settings from more options', () => {
  start.assumeNotePage().moreOptions().openAssimilationSettings()
})

When('I reopen assimilation settings from more options', () => {
  start
    .assumeNotePage()
    .moreOptions()
    .reopenAssimilationSettingsWaitingForRecallInfo()
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
