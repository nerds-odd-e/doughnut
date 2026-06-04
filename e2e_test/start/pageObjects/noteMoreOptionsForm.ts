import { pageIsNotLoading } from '../pageBase'
import { assumeAssimilationPage, keepForRecallButton } from './assimilationPage'
import { questionListPage } from './questionListPage'

const titles = {
  assimilation: 'Assimilation settings',
  delete: 'Delete note',
  questions: 'Questions for the note',
  overflowMenu: 'more options',
} as const

const noteShowToolbar = () => cy.get('[data-note-toolbar]', { timeout: 15000 })

const visibleMoreOptionsButton = (title: string) =>
  cy.get(`button[title="${title}"]:visible`, { timeout: 15000 }).first()

const assimilationSettingsButton = () =>
  visibleMoreOptionsButton(titles.assimilation)

const openOverflowMenuIfNeeded = () => {
  cy.get(`button[title="${titles.assimilation}"]`, { timeout: 15000 }).then(
    ($buttons) => {
      if ($buttons.filter(':visible').length > 0) {
        return
      }

      noteShowToolbar()
        .find(`summary[title="${titles.overflowMenu}"]`)
        .should('be.visible')
        .click()
    }
  )
}

/**
 * Ensures Export / Questions / Assimilation / Delete are reachable — either on the
 * toolbar (wide note column) or inside the overflow menu (narrow).
 */
export const makeSureNoteMoreOptionsFormIsOpen = () => {
  noteShowToolbar().should('exist')
  openOverflowMenuIfNeeded()
  assimilationSettingsButton().should('be.visible')

  return noteMoreOptionsPage()
}

const deleteNoteWithConfirmation = (confirmButtonName: string) => {
  visibleMoreOptionsButton(titles.delete).click()
  cy.findByRole('button', { name: confirmButtonName }).click()
  pageIsNotLoading()
}

const noteMoreOptionsPage = () => {
  return {
    deleteNote() {
      deleteNoteWithConfirmation('OK')
    },
    deleteNoteAndLeaveReferencesAsDeadLinks() {
      deleteNoteWithConfirmation('Leave all references as dead link')
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
      visibleMoreOptionsButton(titles.questions).click()
      return questionListPage()
    },
    openAssimilationSettings() {
      cy.document().then((doc) => {
        if (!doc.querySelector('[data-test="keep-for-recall"]')) {
          assimilationSettingsButton().scrollIntoView().click()
        }
      })
      keepForRecallButton({ timeout: 15000 }).should('be.visible')
      pageIsNotLoading()
      return assumeAssimilationPage().waitForAssimilationReady()
    },
    assimilateNote() {
      this.openAssimilationSettings().clickKeepForRecall()
      pageIsNotLoading()
    },
  }
}
