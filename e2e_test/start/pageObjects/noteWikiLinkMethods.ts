import { clickPopupConfirmOk } from '../../support/daisyModalHelpers'
import testability from '../testability'
import noteCreationForm from './forms/noteCreationForm'
import { findNoteContentRegion } from './notePageContentRegion'
import { assumeNoteTargetSearchDialog } from './noteTargetSearchDialog'

/** Matches `noteShowHref()` (`/n{id}`), `/n/:id`, or legacy `/d/n/:id` note links. */
const noteShowHref = /^\/d\/n\/\d+$|^\/n\/\d+$|^\/n\d+$/
const noteShowPathInUrl = /\/d\/n\/\d+|\/n\/\d+|\/n\d+/

const createNewNoteOffer = 'Create a new note named'
const pointAtExistingNoteOffer = 'Point at an existing note'

type AssumeNotePage = typeof import('./notePage').assumeNotePage
type RichContentSwitcher = { switchToRichContent: () => unknown }

function findWikiLinkInNoteContent(linkClass: string, wikiLinkText: string) {
  return findNoteContentRegion().find(`a.${linkClass}`).contains(wikiLinkText)
}

function clickWikiLinkInNoteContent(
  page: RichContentSwitcher,
  linkClass: string,
  wikiLinkText: string
) {
  page.switchToRichContent()
  findWikiLinkInNoteContent(linkClass, wikiLinkText).click()
}

function wikiLinkInNoteContentFluent(
  wikiLinkText: string,
  assumeNotePage: AssumeNotePage
) {
  const locator = () =>
    findWikiLinkInNoteContent('donut-wiki-link', wikiLinkText)
  return {
    expectNoteShowHref() {
      locator().should('have.attr', 'href').and('match', noteShowHref)
      return this
    },
    expectHrefPointsToNote(noteTitle: string) {
      testability()
        .getInjectedNoteIdByTitle(noteTitle)
        .then((noteId) => {
          locator()
            .should('have.attr', 'href')
            .and('match', new RegExp(`/n${noteId}$|/n/${noteId}$`))
        })
      return this
    },
    followAndAssumeNote(noteTitle: string) {
      locator().click()
      cy.url({ timeout: 15000 }).should('match', noteShowPathInUrl)
      return assumeNotePage(noteTitle)
    },
  }
}

export const noteWikiLinkMethods = (assumeNotePage: AssumeNotePage) => ({
  expectDeadWikiLink(wikiLinkText: string) {
    findWikiLinkInNoteContent('dead-wiki-link', wikiLinkText)
    return this
  },
  followPendingWikiLink(this: RichContentSwitcher, wikiLinkText: string) {
    clickWikiLinkInNoteContent(this, 'pending-wiki-link', wikiLinkText)
    return this
  },
  expectCreateOrPointAtNoteNotOffered() {
    cy.get('body').should(($body) => {
      const text = $body.text()
      expect(
        text,
        'pending wiki link should not offer creating a note'
      ).not.to.include(createNewNoteOffer)
      expect(
        text,
        'pending wiki link should not offer pointing at an existing note'
      ).not.to.include(pointAtExistingNoteOffer)
    })
    return this
  },
  expectCreateOrPointAtNoteOffered() {
    cy.get('dialog')
      .filter(':visible')
      .should(($dialog) => {
        const text = $dialog.text()
        expect(text, 'dead wiki link should offer creating a note').to.include(
          createNewNoteOffer
        )
        expect(
          text,
          'dead wiki link should offer pointing at an existing note'
        ).to.include(pointAtExistingNoteOffer)
      })
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
    clickWikiLinkInNoteContent(this, 'dead-wiki-link', wikiLinkText)
    const chooseCreateNewNote = () => {
      cy.findByRole('button', { name: /Create a new note/ }).click()
    }
    return {
      chooseCreateNewNote,
      createNote: () => {
        chooseCreateNewNote()
        noteCreationForm.submit()
      },
      pointAtExistingNote: (existingNoteTitle: string, displayText: string) => {
        cy.findByRole('button', { name: pointAtExistingNoteOffer }).click()
        assumeNoteTargetSearchDialog()
          .findTarget(existingNoteTitle)
          .pointWikiLinkAtTarget(existingNoteTitle, displayText)
      },
    }
  },
  wikiLinkInNoteContent(wikiLinkText: string) {
    return wikiLinkInNoteContentFluent(wikiLinkText, assumeNotePage)
  },
})
