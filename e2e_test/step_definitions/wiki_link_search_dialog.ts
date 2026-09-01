/// <reference types="cypress" />
/// <reference types="@testing-library/cypress" />
/// <reference types="../support" />
// @ts-check

import { When } from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'

When('I insert a wiki link to {string}', (targetNoteTitle: string) => {
  start.assumeNotePage().insertWikiLinkToNote(targetNoteTitle)
})

When(
  'I insert a wiki link to {string} in folder {string}',
  (targetNoteTitle: string, folderName: string) => {
    start
      .assumeNotePage()
      .insertWikiLinkToNoteInFolder(targetNoteTitle, folderName)
  }
)

When(
  'I point dead wiki link {string} at existing note {string} in folder {string}',
  (deadWikiLinkText: string, existingNoteTitle: string, folderName: string) => {
    start
      .assumeNotePage()
      .followDeadWikiLink(deadWikiLinkText)
      .pointAtExistingNoteInFolder(
        existingNoteTitle,
        folderName,
        deadWikiLinkText
      )
  }
)

When(
  'I point the wiki link at existing note {string} in folder {string}',
  (destinationTitle: string, folderName: string) => {
    start
      .assumeNotePage()
      .pointOpenUnresolvedWikiLinkAtDestinationInFolder(
        destinationTitle,
        folderName
      )
  }
)
