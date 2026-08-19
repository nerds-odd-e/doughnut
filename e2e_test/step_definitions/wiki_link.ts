/// <reference types="cypress" />
/// <reference types="@testing-library/cypress" />
/// <reference types="../support" />
// @ts-check

import { Then, When } from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'

When('I insert a wiki link to {string}', (targetNoteTitle: string) => {
  start.assumeNotePage().insertWikiLinkToNote(targetNoteTitle)
})

When(
  'I move the current note to notebook {string} root',
  (notebookName: string) => {
    start
      .assumeNotePage()
      .openWikiLinkOrRelationship()
      .findTarget(notebookName)
      .moveToNotebookRoot(notebookName)
  }
)

When(
  'I move the current note under folder {string} in notebook {string}',
  (folderName: string, notebookName: string) => {
    start
      .assumeNotePage()
      .openWikiLinkOrRelationship()
      .findTarget(folderName)
      .moveUnder(folderName, notebookName)
  }
)

Then(
  'I should see wiki link {string} as a dead wiki link',
  (wikiLinkText: string) => {
    start.assumeNotePage().expectDeadWikiLink(wikiLinkText)
  }
)

Then(
  'the wiki link {string} should link to the note with the same title',
  (wikiLinkText: string) => {
    start
      .assumeNotePage()
      .wikiLinkInNoteContent(wikiLinkText)
      .expectNoteShowHref()
      .expectHrefPointsToNote(wikiLinkText)
  }
)

Then(
  'the wiki link {string} should open the note titled {string}',
  (wikiLinkText: string, noteTitle: string) => {
    start
      .assumeNotePage()
      .wikiLinkInNoteContent(wikiLinkText)
      .expectNoteShowHref()
      .followAndAssumeNote(noteTitle)
  }
)

Then(
  'following the wiki link {string} should open the note titled {string}',
  (wikiLinkText: string, noteTitle: string) => {
    start
      .assumeNotePage()
      .wikiLinkInNoteContent(wikiLinkText)
      .followAndAssumeNote(noteTitle)
  }
)

function createNoteFromDeadWikiLink(displayText: string, newNoteTitle: string) {
  start.assumeNotePage().followDeadWikiLink(displayText).createNote()
  start.testability().rememberUiCreatedNote(newNoteTitle)
  start.assumeNotePage(newNoteTitle).expectNoteTitleDisplayed(newNoteTitle)
}

When(
  'I create a new note by following the dead wiki link {string}',
  (wikiLinkTitle: string) => {
    createNoteFromDeadWikiLink(wikiLinkTitle, wikiLinkTitle)
  }
)

When(
  'I create a new note titled {string} by following the dead wiki link displayed as {string}',
  (newNoteTitle: string, displayText: string) => {
    createNoteFromDeadWikiLink(displayText, newNoteTitle)
  }
)

When(
  'I try to create a new note by following the dead wiki link displayed as {string}',
  (displayText: string) => {
    start.assumeNotePage().followDeadWikiLink(displayText).chooseCreateNewNote()
  }
)

Then('I should see a warning that a note cannot be created from a path', () => {
  start.assumeNotePage().expectCannotCreateNoteFromPath()
})

When(
  'I point dead wiki link {string} at existing note {string}',
  (deadWikiLinkText: string, existingNoteTitle: string) => {
    start
      .assumeNotePage()
      .followDeadWikiLink(deadWikiLinkText)
      .pointAtExistingNote(existingNoteTitle, deadWikiLinkText)
  }
)
