/// <reference types="cypress" />
/// <reference types="@testing-library/cypress" />
/// <reference types="../support" />
// @ts-check

import { Then, When } from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'

When(
  'I insert a wiki link to {string} via the wiki link or relationship toolbar',
  (targetNoteTitle: string) => {
    start.assumeNotePage().insertWikiLinkToNote(targetNoteTitle)
  }
)

When(
  'I move the current note to notebook {string} root via the wiki link or relationship toolbar',
  (notebookName: string) => {
    start
      .assumeNotePage()
      .startSearchingAndAddRelationship()
      .findTarget(notebookName)
      .moveToNotebookRoot(notebookName)
  }
)

When(
  'I move the current note under folder {string} in notebook {string} via the wiki link or relationship toolbar',
  (folderName: string, notebookName: string) => {
    start
      .assumeNotePage()
      .startSearchingAndAddRelationship()
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
  'I should be able to create a new note by following the dead wiki link {string}',
  (wikiLinkTitle: string) => {
    start.assumeNotePage().followDeadWikiLink(wikiLinkTitle).createNote()
    start.testability().rememberUiCreatedNote(wikiLinkTitle)
    start.assumeNotePage(wikiLinkTitle).expectNoteTitleDisplayed(wikiLinkTitle)
  }
)

When(
  'I point dead wiki link {string} at existing note {string}',
  (deadWikiLinkText: string, existingNoteTitle: string) => {
    start
      .assumeNotePage()
      .followDeadWikiLink(deadWikiLinkText)
      .pointAtExistingNote(existingNoteTitle, deadWikiLinkText)
  }
)
