/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import {
  type DataTable,
  Given,
  Then,
  When,
} from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'

Given('I open the note {string} for editing', (noteTopology: string) => {
  start.jumpToNotePage(noteTopology)
})

When(
  'I upload an image from fixture {string} to the note {string}',
  (fixturePath: string, noteTopology: string) => {
    start
      .jumpToNotePage(noteTopology)
      .switchToRichContentMode()
      .uploadRichNoteImagePropertyFromFixture(fixturePath)
  }
)

When(
  'I set rich note image property URL {string} on note {string}',
  (url: string, noteTopology: string) => {
    start
      .jumpToNotePage(noteTopology)
      .switchToRichContentMode()
      .setRichNoteImagePropertyUrl(url)
  }
)

When(
  'I add a rich note property with key {string} and value {string}',
  (key: string, value: string) => {
    start.assumeNotePage().addRichNoteProperty(key, value)
  }
)

Then(
  'I should see rich note property {string} with value {string}',
  (key: string, value: string) => {
    start.assumeNotePage().expectRichNotePropertyDisplayed(key, value)
  }
)

Then(
  'the rich note property {string} should show an attachment image path',
  (key: string) => {
    start.assumeNotePage().expectRichNoteImagePropertyAttachmentPath(key)
  }
)

Then('I should not see rich note property {string}', (key: string) => {
  start.assumeNotePage().expectRichNotePropertyAbsent(key)
})

When(
  'I edit the rich note property with key {string} to key {string} and value {string}',
  (oldKey: string, newKey: string, newValue: string) => {
    start.assumeNotePage().editRichNoteProperty(oldKey, newKey, newValue)
  }
)

When(
  'I remove rich note property {string} confirming memory tracker change',
  (key: string) => {
    start.assumeNotePage().removeRichNoteProperty(key)
  }
)

When(
  'I remove markdown note property {string} confirming memory tracker change',
  (key: string) => {
    start
      .assumeNotePage()
      .removeMarkdownNotePropertyConfirmingMemoryTrackerChange(key)
  }
)

When('I visit note {string}', (noteTopology: string) => {
  start.jumpToNotePage(noteTopology)
})

When(
  'I rename rich note property key from {string} to {string} confirming memory tracker change',
  (oldKey: string, newKey: string) => {
    start.assumeNotePage().renameRichNotePropertyKey(oldKey, newKey)
  }
)

When(
  'I update note {string} content using markdown to become:',
  (noteTopology: string, newContent: string) => {
    start.jumpToNotePage(noteTopology).updateContentAsMarkdown(newContent)
  }
)

When(
  'I update the current note content using markdown to become:',
  (newContent: string) => {
    start.assumeNotePage().updateContentAsMarkdown(newContent)
  }
)

When('I reload the current page for note {string}', (noteTopology: string) => {
  cy.reload()
  start.waitUntilAppIsNotBusy()
  start.assumeNotePage(noteTopology)
})

const openNoteMarkdownEditor = (noteTopology?: string) => {
  ;(noteTopology
    ? start.jumpToNotePage(noteTopology)
    : start.assumeNotePage()
  ).openMarkdownContentEditor()
}

When('I open the note content markdown editor', () => {
  openNoteMarkdownEditor()
})

When(
  'I open the note content markdown editor on note {string}',
  (noteTopology: string) => {
    openNoteMarkdownEditor(noteTopology)
  }
)

Then(
  'the note content markdown source should contain {string}',
  (fragment: string) => {
    start.assumeNotePage().expectMarkdownContentSourceContains(fragment)
  }
)

Then(
  'the note content markdown source should not contain {string}',
  (fragment: string) => {
    start.assumeNotePage().expectMarkdownContentSourceDoesNotContain(fragment)
  }
)

When('I view the note content as rich content', () => {
  start.assumeNotePage().switchToRichContentMode()
})

When('I view the note content as markdown', () => {
  start.assumeNotePage().toolbarButton('Edit as markdown').click()
})

When('I track the current note title as {string}', (newTitle: string) => {
  start.testability().renameInjectedNoteTitleForNoteOnPage(newTitle)
})

Then(
  'I should see an error toast containing {string}',
  (messageSubstring: string) => {
    cy.contains('.Vue-Toastification__toast--error', messageSubstring, {
      timeout: 10000,
    }).should('be.visible')
  }
)

Then(
  'I should see the rich content elements in the note content:',
  (data: DataTable) => {
    start.assumeNotePage().expectRichContent(data.hashes())
  }
)

Then(
  'note {string} should show the rich content elements in the note content:',
  (noteTitle: string, data: DataTable) => {
    start.jumpToNotePage(noteTitle, true)
    start.waitUntilAppIsNotBusy()
    start
      .assumeNotePage(noteTitle)
      .switchToRichContent()
      .expectRichContent(data.hashes())
  }
)

Then(
  'I should see the rich content of the note with content:',
  (data: DataTable) => {
    start
      .assumeNotePage()
      .switchToRichContent()
      .expectRichContent(data.hashes())
  }
)
