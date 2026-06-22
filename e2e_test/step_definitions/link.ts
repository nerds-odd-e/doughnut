/// <reference types="cypress" />
/// <reference types="@testing-library/cypress" />
/// <reference types="../support" />
// @ts-check

import { When } from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'

When(
  'I insert a wiki link to {string} via the link toolbar',
  (targetNoteTitle: string) => {
    start.assumeNotePage().insertWikiLinkToNote(targetNoteTitle)
  }
)

When(
  'I move the current note to notebook {string} root via the link toolbar',
  (notebookName: string) => {
    start
      .assumeNotePage()
      .startSearchingAndAddRelationship()
      .findTarget(notebookName)
      .moveToNotebookRoot(notebookName)
  }
)

When(
  'I move the current note under folder {string} in notebook {string} via the link toolbar',
  (folderName: string, notebookName: string) => {
    start
      .assumeNotePage()
      .startSearchingAndAddRelationship()
      .findTarget(folderName)
      .moveUnder(folderName, notebookName)
  }
)
