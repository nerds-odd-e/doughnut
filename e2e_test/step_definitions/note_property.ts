/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import {
  After,
  Given,
  Then,
  When,
} from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'
import { touchDevice } from '../start/touchDevice'

const conversationQuery = { conversation: 'true' }

Given('I use a touch device', () => {
  touchDevice.use()
})

Then('the device should report a coarse pointer', () => {
  touchDevice.expectCoarsePointer()
})

After(() => {
  touchDevice.reset()
})

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
  'the rich note property {string} should be focused showing {string}',
  (key: string, value: string) => {
    start.assumeNotePage().expectFocusedRichNotePropertyValue(key, value)
  }
)

Then('the value of property {string} should be visible', (key: string) => {
  start.assumeNotePage().expectRichNotePropertyValueVisible(key)
})

Then(
  'the editable value of property {string} should show all its text',
  (key: string) => {
    start.assumeNotePage().expectEditablePropertyValueShowsAllText(key)
  }
)

Then(
  'the key of property {string} should end with an ellipsis',
  (key: string) => {
    start.assumeNotePage().expectRichNotePropertyKeyEndsWithEllipsis(key)
  }
)

Then('the controls of property {string} should not overlap', (key: string) => {
  start.assumeNotePage().expectRichNotePropertyControlsApart(key)
})

Then(
  'the controls of property {string} should be at least 44 px high',
  (key: string) => {
    start.assumeNotePage().expectRichNotePropertyControlHeights(key, true)
  }
)

Then(
  'the controls of property {string} should be under 44 px high',
  (key: string) => {
    start.assumeNotePage().expectRichNotePropertyControlHeights(key, false)
  }
)

Then('the note should not scroll sideways', () => {
  start.assumeNotePage().expectNoteWithoutSidewaysScroll()
})

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

When('I start adding a property with key {string}', (key: string) => {
  start.assumeNotePage().startAddingRichNoteProperty(key)
})

When(
  'I type {string} in the key of property {string}',
  (text: string, key: string) => {
    start.assumeNotePage().typeInRichNotePropertyKey(key, text)
  }
)

Then('the new property value should not be covered', () => {
  start
    .assumeNotePage()
    .expectNewRichNotePropertyControlNotCovered('rich-note-property-value')
})

Then('the new property Choose image button should not be covered', () => {
  start
    .assumeNotePage()
    .expectNewRichNotePropertyControlNotCovered('rich-note-image-insert-choose')
})

Then(
  'the key preset {string} should stay inside the key panel',
  (presetKey: string) => {
    start.assumeNotePage().expectKeyPresetInsideKeyPanel(presetKey)
  }
)
