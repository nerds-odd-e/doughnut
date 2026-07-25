import { commonSenseSplit } from '../../support/string_util'
import { waitUntilAppIsNotBusy } from '../pageBase'
import testability from '../testability'
import audioToolsPage from './audioToolsPage'
import noteCreationForm from './forms/noteCreationForm'
import { assumeNoteTargetSearchDialog } from './noteTargetSearchDialog'
import { sidebarChildNotePageMethods } from './sidebarChildNotePageMethods'
import { makeSureNoteMoreOptionsFormIsOpen } from './noteMoreOptionsForm'
import { toolbarButton } from './toolbarButton'
import { noteContentEditingMethods } from './noteContentEditingMethods'
import { noteConversationAndQuestionMethods } from './noteConversationAndQuestionMethods'
import {
  findNoteContentRegion,
  noteContentRegion,
} from './notePageContentRegion'
import { noteRelationshipMethods } from './noteRelationshipMethods'
import { noteRichPropertyMethods } from './noteRichPropertyMethods'

/** Matches `noteShowHref()` (`/n{id}`), `/n/:id`, or legacy `/d/n/:id` note links. */
const noteShowHref = /^\/d\/n\/\d+$|^\/n\/\d+$|^\/n\d+$/
const noteShowPathInUrl = /\/d\/n\/\d+|\/n\/\d+|\/n\d+/

const mainNoteHeadingTitleSelector =
  '#main-note-content h2.path-name-heading [role=title]'

type TitleRenameReferenceChoice = 'KEEP_VISIBLE_TEXT' | 'UPDATE_VISIBLE_TEXT'

const titleRenameReferenceSaveTestId: Record<
  TitleRenameReferenceChoice,
  string
> = {
  KEEP_VISIBLE_TEXT: 'referenced-title-save-keep-visible-text',
  UPDATE_VISIBLE_TEXT: 'referenced-title-save-update-visible-text',
}

function wikiLinkInNoteContentFluent(linkText: string) {
  const locator = () =>
    findNoteContentRegion().find('a.doughnut-link').contains(linkText)
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

export const assumeNotePage = (
  noteTopology?: string,
  options?: { timeout?: number }
) => {
  const findNoteTitle = (title: string) =>
    cy
      .get(mainNoteHeadingTitleSelector, {
        ...(options?.timeout !== undefined ? { timeout: options.timeout } : {}),
      })
      .should(($el) => {
        expect($el.text().trim()).to.eq(title)
      })

  if (noteTopology) {
    findNoteTitle(noteTopology)
  }
  return {
    expectNoteTitleDisplayed(title: string) {
      findNoteTitle(title)
      return this
    },
    /** Asserts the rendered note content body contains this substring (plain or rich) */
    expectContentContaining(fragment: string) {
      this.findNoteContent(fragment)
      return this
    },
    moreOptions: () => {
      return makeSureNoteMoreOptionsFormIsOpen()
    },
    expandChildren: () => {
      cy.findByRole('button', { name: 'expand children' }).click()
    },
    expectBreadcrumb: (items: string) => {
      cy.get('.daisy-breadcrumbs').within(() =>
        commonSenseSplit(items, ', ').forEach((noteTopology: string) =>
          cy.findByText(noteTopology)
        )
      )
    },
    findNoteContent: (expected: string, timeout?: number) => {
      const normalized = expected.replace(/\\n/g, '\n')
      const lines = normalized.split('\n').filter((line) => line.length > 0)
      cy.findByRole(noteContentRegion.role, {
        name: noteContentRegion.name,
        ...(timeout !== undefined ? { timeout } : {}),
      }).should(($el) => {
        const text = $el.text()
        for (const line of lines) {
          expect(
            text,
            `expected note content to include ${JSON.stringify(line)}`
          ).to.contain(line)
        }
      })
    },
    expectNoteContentContainLineBreak: () => {
      findNoteContentRegion().within(() => {
        cy.get('.ql-editor, [contenteditable]').should(($el) => {
          const html = $el.html()
          const match = html.match(/Hello\s*<br[^>]*>/i)
          expect(match).to.not.be.null
          expect(html).to.match(/Hello\s*<br[^>]*>\s*World/i)
        })
      })
    },
    toolbarButton: (btnTextOrTitle: string) => {
      return toolbarButton(btnTextOrTitle)
    },
    undo(undoType: string) {
      this.toolbarButton(`undo ${undoType}`).click()
      cy.findByRole('button', { name: 'OK' }).click()
    },
    saveReferencedNoteTitle: (
      newTitle: string,
      choice: TitleRenameReferenceChoice
    ) => {
      cy.get('#main-note-content').find('[role=title]').first().click()
      cy.clearFocusedText().type(newTitle)
      cy.findByTestId('referenced-title-save-panel')
        .find(`[data-testid="${titleRenameReferenceSaveTestId[choice]}"]`)
        .click()
      cy.findByTestId('referenced-title-save-panel').should('not.exist')
      cy.get('#main-note-content')
        .find('[role=title]')
        .first()
        .should('contain', newTitle)
      waitUntilAppIsNotBusy()
      testability().renameInjectedNoteTitleForNoteOnPage(newTitle)
    },
    audioTools() {
      this.toolbarButton('Audio tools').click()
      return audioToolsPage()
    },
    navigateToReference: (referenceTopic: string) => {
      cy.get('#main-note-content')
        .find('[role=card]')
        .contains(referenceTopic)
        .closest('[role=card]')
        .find('a')
        .first()
        .click({ force: true })
      findNoteContentRegion().should('be.visible')
      waitUntilAppIsNotBusy()
      return assumeNotePage()
    },
    expectDeadWikiLink(linkText: string) {
      findNoteContentRegion().find('a.dead-link').contains(linkText)
      return this
    },
    followDeadLink(linkTitle: string) {
      this.switchToRichContent()
      findNoteContentRegion().find('a.dead-link').contains(linkTitle).click()
      return {
        createNote: () => {
          cy.findByRole('button', { name: /Create a new note/ }).click()
          noteCreationForm.submit()
        },
        linkToExistingNote: (
          existingNoteTitle: string,
          displayText: string
        ) => {
          cy.findByRole('button', { name: 'Link to an existing note' }).click()
          assumeNoteTargetSearchDialog()
            .findTarget(existingNoteTitle)
            .insertDeadLinkToTarget(existingNoteTitle, displayText)
        },
      }
    },
    wikiLinkInNoteContent(linkText: string) {
      return wikiLinkInNoteContentFluent(linkText)
    },
    ...noteContentEditingMethods(),
    ...noteRichPropertyMethods(),
    ...noteRelationshipMethods(),
    ...sidebarChildNotePageMethods(),
    ...noteConversationAndQuestionMethods(),
  }
}
