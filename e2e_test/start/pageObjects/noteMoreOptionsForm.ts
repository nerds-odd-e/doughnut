import { waitUntilAppIsNotBusy } from '../pageBase'
import {
  assumeAssimilationPage,
  assimilationModesSelector,
  noteLevelAssimilationModesPanel,
  noteLevelControlElements,
} from './assimilationPage'
import { clickToolbarOverflowAction, noteToolbar } from './noteToolbarOverflow'
import { questionListPage } from './questionListPage'

const titles = {
  audio: 'Audio tools',
  assimilation: 'Assimilate',
  trash: 'Trash note (d)',
  questions: 'Questions for the note',
} as const

const clickMoreOption = clickToolbarOverflowAction

const trashNoteWithConfirmation = (confirmButtonName: string | RegExp) => {
  clickMoreOption(titles.trash)
  cy.findByRole('button', { name: confirmButtonName }).click()
  waitUntilAppIsNotBusy()
}

/**
 * Note toolbar more-options actions — on the bar when inline/pinned, otherwise
 * inside the overflow menu.
 */
export const noteMoreOptions = () => {
  noteToolbar().should('exist')

  return {
    trashNote() {
      trashNoteWithConfirmation('OK')
    },
    /** Remove a relationship note without reducing it to a source property. */
    deleteRelationshipNote() {
      trashNoteWithConfirmation(/^Trash "/)
    },
    trashNoteAndLeaveReferencesAsDeadWikiLinks() {
      trashNoteWithConfirmation('Leave all references as dead wiki links')
    },
    trashNoteAndRemoveFromReferenceProperties() {
      trashNoteWithConfirmation(
        'Remove from properties of references (undo will not recover the removed property)'
      )
    },
    deleteNoteAndReduceToSourceProperty() {
      trashNoteWithConfirmation(/^Reduce to a property of the source/)
    },
    openQuestionList() {
      clickMoreOption(titles.questions)
      return questionListPage()
    },
    openAudioTools() {
      clickMoreOption(titles.audio)
      cy.findByRole('button', { name: 'Record Audio' }).should('be.visible')
      waitUntilAppIsNotBusy()
    },
    openAssimilationPanel() {
      // `assimilationModesSelector` also matches a note property's own
      // AssimilationModes row (RichFrontmatterPropertyPanel) when its
      // property panel is open — scope both the "already open" probe and
      // the visibility wait to the note-level panel only.
      cy.document().then((doc) => {
        const hasAssimilationPanel =
          noteLevelControlElements(doc, assimilationModesSelector).length > 0
        if (!hasAssimilationPanel) {
          clickMoreOption(titles.assimilation)
        }
      })
      noteLevelAssimilationModesPanel({ timeout: 15000 }).should('be.visible')
      waitUntilAppIsNotBusy()
      return assumeAssimilationPage()
    },
    assimilateNote() {
      this.openAssimilationPanel().clickAssimilate()
      waitUntilAppIsNotBusy()
    },
  }
}
