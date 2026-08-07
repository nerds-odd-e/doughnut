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

When('I reopen assimilation settings', () => {
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

When('I expand assimilation properties', () => {
  start.assumeAssimilationPage().expandAssimilationPropertiesSection()
})

When('I skip recall on property {string}', (propertyKey: string) => {
  start.assumeAssimilationPage().skipRecallProperty(propertyKey)
})

When('I revive recall for property {string}', (propertyKey: string) => {
  start.assumeAssimilationPage().reviveRecallProperty(propertyKey)
})

When('I revive recall on the assimilation panel', () => {
  start.assumeAssimilationPage().reviveRecallOnPanel()
})

Then(
  'assimilate for property {string} should be disabled',
  (propertyKey: string) => {
    start.assumeAssimilationPage().expectPropertyAssimilateDisabled(propertyKey)
  }
)

Then('I should see Revive for property {string}', (propertyKey: string) => {
  start.assumeAssimilationPage().expectReviveForProperty(propertyKey)
})

Then(
  'I should see Skip recall for property {string}',
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
