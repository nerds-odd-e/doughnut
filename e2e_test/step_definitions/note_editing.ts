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

When(
  'I update note {string} to become:',
  (noteTopology: string, data: DataTable) => {
    start.jumpToNotePage(noteTopology).editTextContent(data.hashes()[0]!)
  }
)

When(
  'I insert a soft line break in note {string} between {string} and {string}',
  (noteTopology: string, before: string, after: string) => {
    start
      .jumpToNotePage(noteTopology)
      .insertSoftLineBreakInContent(before, after)
  }
)

Then('I should see note {string} has an image', (noteTopology: string) => {
  start.jumpToNotePage(noteTopology).expectHeaderImage()
})

When(
  'I change the title from {string} to {string}',
  (noteTopology: string, newNoteTitle: string) => {
    start.assumeNotePage(noteTopology).editTextContent({ title: newNoteTitle })
  }
)

Given(
  'I update note title {string} to become {string}',
  (noteTopology: string, newNoteTitle: string) => {
    start.jumpToNotePage(noteTopology).editTextContent({ title: newNoteTitle })
  }
)

When(
  'I set the note title to {string} keeping visible reference text',
  (newTitle: string) => {
    start
      .assumeNotePage()
      .saveReferencedNoteTitle(newTitle, 'KEEP_VISIBLE_TEXT')
  }
)

When(
  'I set the note title to {string} updating visible reference text',
  (newTitle: string) => {
    start
      .assumeNotePage()
      .saveReferencedNoteTitle(newTitle, 'UPDATE_VISIBLE_TEXT')
  }
)

Given(
  'I update note {string} content to become {string}',
  (noteTopology: string, newContent: string) => {
    start.assumeNotePage(noteTopology).editTextContent({ Content: newContent })
  }
)

Given(
  'I update note {string} content from {string} to become {string}',
  (noteTopology: string, previousContent: string, newContent: string) => {
    cy.findByText(previousContent).click({ force: true })
    start.assumeNotePage(noteTopology).editTextContent({ Content: newContent })
  }
)

When(
  'I update note {string} with content {string}',
  (noteTopology: string, newContent: string) => {
    start.jumpToNotePage(noteTopology).editTextContent({ Content: newContent })
    start.assumeNotePage().findNoteContent(newContent)
  }
)

Then('I should see {string} in breadcrumb', (noteTitles: string) => {
  start.waitUntilAppIsNotBusy().assumeNotePage().expectBreadcrumb(noteTitles)
})

Then('the note title should be {string}', (title: string) => {
  start.assumeNotePage().expectNoteTitleDisplayed(title)
})

Then('the note content should include {string}', (fragment: string) => {
  start.assumeNotePage().expectContentContaining(fragment)
})

Then(
  'the note content should contain a soft line break between {string} and {string}',
  (before: string, after: string) => {
    start.assumeNotePage().expectSoftLineBreakBetween(before, after)
  }
)
