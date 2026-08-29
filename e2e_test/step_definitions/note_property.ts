/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import { Then, When } from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'

When(
  'I visit property {string} of note {string}',
  (propertyKey: string, noteTopology: string) => {
    start.jumpToNoteProperty(noteTopology, propertyKey)
  }
)

Then(
  'the rich note property {string} should be focused with its value dialog open',
  (key: string) => {
    start.assumeNotePage().expectFocusedRichNoteProperty(key)
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
