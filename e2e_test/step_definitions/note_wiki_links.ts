/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import { Then, When } from '@badeball/cypress-cucumber-preprocessor'
import type NotePath from '../support/NotePath'
import start from '../start'

Then(
  'I should see note {notepath} has content {string}',
  (notePath: NotePath, expectedContent: string) => {
    start
      .navigateToNotebooksPage()
      .navigateToPath(notePath)
      .findNoteContent(expectedContent)
  }
)

Then(
  'note {string} should have content {string}',
  (noteTitle: string, expectedContent: string) => {
    start.jumpToNotePage(noteTitle).findNoteContent(expectedContent)
  }
)

Then('the note content should contain a line break', () => {
  start.assumeNotePage().expectNoteContentContainLineBreak()
})

Then('I should see wiki link {string} as a dead link', (linkText: string) => {
  start.assumeNotePage().expectDeadWikiLink(linkText)
})

Then(
  'the link {string} should link to the note with the same title',
  (linkText: string) => {
    start
      .assumeNotePage()
      .wikiLinkInNoteContent(linkText)
      .expectNoteShowHref()
      .expectHrefPointsToNote(linkText)
  }
)

Then(
  'the link {string} should open the note titled {string}',
  (linkText: string, noteTitle: string) => {
    start
      .assumeNotePage()
      .wikiLinkInNoteContent(linkText)
      .expectNoteShowHref()
      .followAndAssumeNote(noteTitle)
  }
)

Then(
  'I should be able to create a new note by following the dead link {string}',
  (linkTitle: string) => {
    start.assumeNotePage().followDeadLink(linkTitle).createNote()
    start.testability().rememberUiCreatedNote(linkTitle)
    start.assumeNotePage(linkTitle).expectNoteTitleDisplayed(linkTitle)
  }
)

When(
  'I link dead link {string} to existing note {string}',
  (deadLinkText: string, existingNoteTitle: string) => {
    start
      .assumeNotePage()
      .followDeadLink(deadLinkText)
      .pointAtExistingNote(existingNoteTitle, deadLinkText)
  }
)
