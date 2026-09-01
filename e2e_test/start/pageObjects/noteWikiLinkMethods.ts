import { noteShowHref } from '@/routes/noteShowLocation'
import { clickPopupConfirmOk } from '../../support/daisyModalHelpers'
import { waitUntilAppIsNotBusy } from '../pageBase'
import testability from '../testability'
import noteCreationForm from './forms/noteCreationForm'
import { findNoteContentRegion } from './notePageContentRegion'
import { assumeNoteTargetSearchDialog } from './noteTargetSearchDialog'

/** Classifier for inbound/legacy note-show paths in `cy.url()`, not a compile of the named table. */
const noteShowPathInUrl = /\/d\/n\/\d+|\/n\/\d+|\/n\d+/

const pointAtExistingNoteOffer = 'Point at an existing note'

type AssumeNotePage = typeof import('./notePage').assumeNotePage
type RichContentSwitcher = { switchToRichContent: () => unknown }

function findWikiLinkInNoteContent(linkClass: string, wikiLinkText: string) {
  return findNoteContentRegion().find(`a.${linkClass}`).contains(wikiLinkText)
}

function pointUnresolvedWikiLinkAtDestinationInFolder(
  destinationTitle: string,
  folderName: string,
  displayText: string
) {
  cy.findByRole('button', { name: pointAtExistingNoteOffer }).click()
  assumeNoteTargetSearchDialog()
    .findTarget(destinationTitle)
    .pointWikiLinkAtTargetInFolder(destinationTitle, folderName, displayText)
}

function wikiLinkInNoteContentFluent(
  wikiLinkText: string,
  assumeNotePage: AssumeNotePage
) {
  const locator = () =>
    findWikiLinkInNoteContent('donut-wiki-link', wikiLinkText)
  return {
    expectHrefPointsToNote(noteTitle: string) {
      testability()
        .getInjectedNoteIdByTitle(noteTitle)
        .then((noteId: number) => {
          locator()
            .should('have.attr', 'href')
            .and('equal', noteShowHref(noteId))
        })
      return this
    },
    followAndAssumeNote(noteTitle: string) {
      locator().click()
      cy.url({ timeout: 15000 }).should('match', noteShowPathInUrl)
      return assumeNotePage(noteTitle)
    },
    followToNoteProperty(noteTitle: string, propertyKey: string) {
      locator().click()
      waitUntilAppIsNotBusy()
      return assumeNotePage(noteTitle).expectAtNoteProperty(
        noteTitle,
        propertyKey
      )
    },
  }
}

export const noteWikiLinkMethods = (assumeNotePage: AssumeNotePage) => ({
  expectDeadWikiLink(wikiLinkText: string) {
    findWikiLinkInNoteContent('dead-wiki-link', wikiLinkText)
    return this
  },
  expectAmbiguousWikiLinkAsksForLongerPath() {
    cy.get('dialog')
      .filter(':visible')
      .should(($dialog) => {
        const text = $dialog.text()
        expect(text, 'ambiguous wiki link guidance').to.match(
          /several notes match/i
        )
        expect(text, 'asks for a longer Portable path').to.match(
          /longer Portable path/i
        )
      })
    cy.findByRole('button', { name: pointAtExistingNoteOffer }).should(
      'be.visible'
    )
    return this
  },
  expectWikiLinkCreateNoteNotOffered() {
    cy.findByRole('button', { name: /Create a new note/ }).should('not.exist')
    return this
  },
  expectCannotCreateNoteFromPath() {
    cy.get('dialog')
      .filter(':visible')
      .contains(
        'Cannot create a note from a path. You can point at an existing note instead.'
      )
      .should('be.visible')
    clickPopupConfirmOk()
    cy.findByTestId('note-new-form').should('not.exist')
    cy.findByRole('button', { name: pointAtExistingNoteOffer }).should(
      'be.visible'
    )
    return this
  },
  followDeadWikiLink(this: RichContentSwitcher, wikiLinkText: string) {
    this.switchToRichContent()
    findWikiLinkInNoteContent('dead-wiki-link', wikiLinkText).click()
    const chooseCreateNewNote = () => {
      cy.findByRole('button', { name: /Create a new note/ }).click()
    }
    return {
      chooseCreateNewNote,
      createNote: () => {
        chooseCreateNewNote()
        noteCreationForm.submit()
      },
      pointAtExistingNoteInFolder: (
        destinationTitle: string,
        folderName: string,
        displayText: string
      ) => {
        pointUnresolvedWikiLinkAtDestinationInFolder(
          destinationTitle,
          folderName,
          displayText
        )
      },
    }
  },
  pointOpenUnresolvedWikiLinkAtDestinationInFolder(
    destinationTitle: string,
    folderName: string
  ) {
    pointUnresolvedWikiLinkAtDestinationInFolder(
      destinationTitle,
      folderName,
      destinationTitle
    )
    return this
  },
  wikiLinkInNoteContent(wikiLinkText: string) {
    return wikiLinkInNoteContentFluent(wikiLinkText, assumeNotePage)
  },
})
