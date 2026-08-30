/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import { Given, Then, When } from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'

const conversationQuery = { conversation: 'true' }

When(
  'I visit property {string} of note {string}',
  (propertyKey: string, noteTopology: string) => {
    start.jumpToNoteProperty(noteTopology, propertyKey)
  }
)

Given(
  'I visit note {string} with conversation query',
  (noteTopology: string) => {
    start.jumpToNoteShowWithConversationQuery(noteTopology)
  }
)

When('I open the property panel for property {string}', (key: string) => {
  start.assumeNotePage().openRichNotePropertyPanel(key)
})

When('I close the property panel', () => {
  start.assumeNotePage().closeRichNotePropertyPanel()
})

When(
  'I rename the focused property key from {string} to {string}',
  (oldKey: string, newKey: string) => {
    start.assumeNotePage().renameFocusedRichNotePropertyKey(oldKey, newKey)
  }
)

Then(
  'the rich note property {string} should be focused with its property panel open',
  (key: string) => {
    start.assumeNotePage().expectFocusedRichNotePropertyPanel(key)
  }
)

Then(
  'the rich note property {string} should be focused showing {string} without a value dialog',
  (key: string, value: string) => {
    start
      .assumeNotePage()
      .expectFocusedRichNotePropertyValueWithoutDialog(key, value)
  }
)

Then('the property {string} should not be found', (key: string) => {
  start.assumeNotePage().expectRichNotePropertyNotFound(key)
})

Then(
  'I should be at property {string} of note {string}',
  (propertyKey: string, noteTopology: string) => {
    start.assumeNotePage().expectAtNoteProperty(noteTopology, propertyKey)
  }
)

Then(
  'I should be at property {string} of note {string} with conversation query',
  (propertyKey: string, noteTopology: string) => {
    start
      .assumeNotePage()
      .expectAtNoteProperty(noteTopology, propertyKey, conversationQuery)
  }
)

Then('I should be at note {string}', (noteTopology: string) => {
  start.assumeNotePage().expectAtNoteShow(noteTopology)
})

Then(
  'I should be at note {string} with conversation query',
  (noteTopology: string) => {
    start.assumeNotePage().expectAtNoteShow(noteTopology, conversationQuery)
  }
)

Then('the property value dialog should be closed', () => {
  start.assumeNotePage().expectPropertyValueDialogClosed()
})
