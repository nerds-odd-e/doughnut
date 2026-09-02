import { waitUntilAppIsNotBusy } from '../pageBase'
import {
  assumeAssimilationPage,
  assimilateButton,
  assimilateButtonSelector,
  isNoteLevelAssimilationControl,
} from './assimilationPage'
import { clickToolbarOverflowAction, noteToolbar } from './noteToolbarOverflow'
import { questionListPage } from './questionListPage'

const titles = {
  audio: 'Audio tools',
  assimilation: 'Assimilation settings',
  delete: 'Delete note (d)',
  questions: 'Questions for the note',
} as const

const clickMoreOption = clickToolbarOverflowAction

const deleteNoteWithConfirmation = (confirmButtonName: string | RegExp) => {
  clickMoreOption(titles.delete)
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
    deleteNote() {
      deleteNoteWithConfirmation('OK')
    },
    /** Plain delete for a relationship note (reduce vs delete options dialog). */
    deleteRelationshipNote() {
      deleteNoteWithConfirmation(/^Delete "/)
    },
    deleteNoteAndLeaveReferencesAsDeadWikiLinks() {
      deleteNoteWithConfirmation('Leave all references as dead wiki links')
    },
    deleteNoteAndRemoveFromReferenceProperties() {
      deleteNoteWithConfirmation(
        'Remove from properties of references (undo will not recover the removed property)'
      )
    },
    deleteNoteAndReduceToSourceProperty() {
      deleteNoteWithConfirmation('Reduce to a property of the source')
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
    openAssimilationSettings() {
      cy.document().then((doc) => {
        const hasNoteLevelAssimilateButton = [
          ...doc.querySelectorAll(assimilateButtonSelector),
        ].some(isNoteLevelAssimilationControl)
        if (!hasNoteLevelAssimilateButton) {
          clickMoreOption(titles.assimilation)
        }
      })
      assimilateButton({ timeout: 15000 }).should('be.visible')
      waitUntilAppIsNotBusy()
      return assumeAssimilationPage()
    },
    assimilateNote() {
      this.openAssimilationSettings().clickAssimilate()
      waitUntilAppIsNotBusy()
    },
  }
}
