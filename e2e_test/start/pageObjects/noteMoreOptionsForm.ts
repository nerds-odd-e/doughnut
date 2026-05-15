import { pageIsNotLoading } from '../pageBase'
import { assumeAssimilationPage } from './assimilationPage'
import { toolbarButton } from './toolbarButton'
import { questionListPage } from './questionListPage'

export const makeSureNoteMoreOptionsFormIsOpen = () => {
  cy.findByRole('button', { name: 'more options' }).then(($button) => {
    if (!$button.hasClass('daisy-btn-active')) {
      cy.wrap($button).click()
    }
  })

  return noteMoreOptionsPage()
}

const noteMoreOptionsPage = () => {
  return {
    toolbarButton,
    deleteNote() {
      toolbarButton('Delete note').click()
      cy.findByRole('button', { name: 'OK' }).click()
      pageIsNotLoading()
    },
    deleteNoteAndLeaveReferencesAsDeadLinks() {
      toolbarButton('Delete note').click()
      cy.findByRole('button', {
        name: 'Leave all references as dead link',
      }).click()
      pageIsNotLoading()
    },
    deleteNoteAndRemoveFromReferenceProperties() {
      toolbarButton('Delete note').click()
      cy.findByRole('button', {
        name: 'Remove from properties of references (undo will not recover the removed property)',
      }).click()
      pageIsNotLoading()
    },
    openQuestionList() {
      toolbarButton('Questions for the note').click()
      return questionListPage()
    },
    openAssimilationPage() {
      toolbarButton('Assimilation settings').click()
      cy.url().should('include', '/assimilate/')
      pageIsNotLoading()
      return assumeAssimilationPage().waitForAssimilationReady()
    },
    assimilateNote() {
      this.openAssimilationPage().clickKeepForRecall()
      pageIsNotLoading()
    },
  }
}
