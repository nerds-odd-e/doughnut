import { commonSenseSplit } from '../../support/string_util'
import { waitUntilAppIsNotBusy } from '../pageBase'
import testability from '../testability'
import audioToolsPage from './audioToolsPage'
import { sidebarChildNotePageMethods } from './sidebarChildNotePageMethods'
import { noteMoreOptions } from './noteMoreOptionsForm'
import { toolbarButton } from './toolbarButton'
import { noteContentEditingMethods } from './noteContentEditingMethods'
import { noteConversationAndQuestionMethods } from './noteConversationAndQuestionMethods'
import {
  findNoteContentRegion,
  noteContentRegion,
} from './notePageContentRegion'
import { notePropertyLocationMethods } from './notePropertyLocationMethods'
import { noteRelationshipMethods } from './noteRelationshipMethods'
import { noteRichPropertyAssimilationMethods } from './noteRichPropertyAssimilationMethods'
import { noteRichPropertyMethods } from './noteRichPropertyMethods'
import { noteWikiLinkMethods } from './noteWikiLinkMethods'

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
    expectOkfIncompatibleTitleWarning() {
      cy.get('#main-note-content .path-name-editor .text-warning').should(
        ($el) => {
          const actual = $el.text().trim()
          expect(
            actual,
            `Expected a warning that the portable tree may be OKF-incompatible, but found: "${actual}"`
          ).to.match(/OKF-incompatible/i)
        }
      )
      return this
    },
    expectHeaderImage() {
      cy.get('#note-image').should(($img) => {
        expect(
          $img.length,
          'Expected note header image (#note-image) to be present'
        ).to.be.greaterThan(0)
      })
      return this
    },
    /** Asserts the rendered note content body contains this substring (plain or rich) */
    expectContentContaining(fragment: string) {
      this.findNoteContent(fragment)
      return this
    },
    moreOptions: noteMoreOptions,
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
    expectSoftLineBreakBetween(before: string, after: string) {
      const escapedBefore = before.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
      const escapedAfter = after.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
      const softBreak = new RegExp(
        `${escapedBefore}\\s*<br[^>]*>\\s*${escapedAfter}`,
        'i'
      )
      findNoteContentRegion().within(() => {
        cy.get('.ql-editor').should(($el) => {
          const html = $el.html()
          expect(
            html,
            `Expected soft line break between "${before}" and "${after}", but found: ${html}`
          ).to.match(softBreak)
        })
      })
      return this
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
      noteMoreOptions().openAudioTools()
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
    ...noteWikiLinkMethods(assumeNotePage),
    ...noteContentEditingMethods(),
    ...noteRichPropertyMethods(),
    ...notePropertyLocationMethods(),
    ...noteRichPropertyAssimilationMethods(),
    ...noteRelationshipMethods(),
    ...sidebarChildNotePageMethods(),
    ...noteConversationAndQuestionMethods(),
  }
}
