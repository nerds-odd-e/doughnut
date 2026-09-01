/// <reference types="cypress" />
/// <reference types="@testing-library/cypress" />
/// <reference types="../support" />
// @ts-check

import { Then, When } from '@badeball/cypress-cucumber-preprocessor'
import { PRODUCTION_CANONICAL_DONUT_ORIGIN } from '@/utils/noteIdUrl'
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
      .expectHrefPointsToNote(wikiLinkText)
  }
)

function followWikiLinkToNote(wikiLinkText: string, noteTitle: string) {
  start
    .assumeNotePage()
    .wikiLinkInNoteContent(wikiLinkText)
    .followAndAssumeNote(noteTitle)
}

Then(
  'the wiki link {string} should open the note titled {string}',
  followWikiLinkToNote
)

Then(
  'the wiki link {string} should open property {string} of note {string}',
  (wikiLinkText: string, propertyKey: string, noteTitle: string) => {
    start
      .assumeNotePage()
      .wikiLinkInNoteContent(wikiLinkText)
      .followToNoteProperty(noteTitle, propertyKey)
  }
)

When('I follow the dead wiki link {string}', (wikiLinkText: string) => {
  start.assumeNotePage().followDeadWikiLink(wikiLinkText)
})

Then(
  'I should see that several notes match and I can choose one for a longer Portable path',
  () => {
    start.assumeNotePage().expectAmbiguousWikiLinkAsksForLongerPath()
  }
)

Then('I should not be offered to create a note from the wiki link', () => {
  start.assumeNotePage().expectWikiLinkCreateNoteNotOffered()
})

Then('I should still see the note titled {string}', (noteTitle: string) => {
  start.assumeNotePage().expectNoteTitleDisplayed(noteTitle)
})

Then(
  'following the wiki link {string} should open the note titled {string}',
  followWikiLinkToNote
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

When(
  'I point the wiki link at existing note {string}',
  (destinationTitle: string) => {
    start
      .assumeNotePage()
      .pointOpenUnresolvedWikiLinkAtDestination(destinationTitle)
  }
)

Then('I should not see the References section', () => {
  cy.findByRole('heading', { name: 'References' }).should('not.exist')
})

Then('I should see the References section', () => {
  cy.findByRole('heading', { name: 'References' }).should('be.visible')
})

Then('I should see {string} in the References section', (noteTitle: string) => {
  cy.findByRole('heading', { name: 'References' })
    .parent()
    .should('contain.text', noteTitle)
})

When(
  'I update note {string} content using markdown to become a note URL to {string} with display {string}',
  (sourceTitle: string, targetTitle: string, display: string) => {
    start
      .testability()
      .getInjectedNoteIdByTitle(targetTitle)
      .then((noteId: number) => {
        start
          .jumpToNotePage(sourceTitle)
          .updateContentAsMarkdown(`[${display}](/n${noteId})`)
      })
  }
)

When(
  'I update note {string} content using markdown to become a full Donut note URL to {string} with display {string}',
  (sourceTitle: string, targetTitle: string, display: string) => {
    start
      .testability()
      .getInjectedNoteIdByTitle(targetTitle)
      .then((noteId: number) => {
        start
          .jumpToNotePage(sourceTitle)
          .updateContentAsMarkdown(
            `[${display}](${PRODUCTION_CANONICAL_DONUT_ORIGIN}/n${noteId})`
          )
      })
  }
)

Then(
  'the note content markdown source should contain a note URL to {string} with display {string}',
  (targetTitle: string, display: string) => {
    start
      .testability()
      .getInjectedNoteIdByTitle(targetTitle)
      .then((noteId: number) => {
        start
          .assumeNotePage()
          .expectMarkdownContentSourceContains(`[${display}](/n${noteId})`)
      })
  }
)

Then(
  'the note content markdown source should contain a full Donut note URL to {string} with display {string}',
  (targetTitle: string, display: string) => {
    start
      .testability()
      .getInjectedNoteIdByTitle(targetTitle)
      .then((noteId: number) => {
        start
          .assumeNotePage()
          .expectMarkdownContentSourceContains(
            `[${display}](${PRODUCTION_CANONICAL_DONUT_ORIGIN}/n${noteId})`
          )
      })
  }
)
