import { pageIsNotLoading } from '../pageBase'
import { assumeAssimilationPage } from './assimilationPage'
import { toolbarButton } from './toolbarButton'
import { questionListPage } from './questionListPage'

export const makeSureNoteMoreOptionsFormIsOpen = () => {
  cy.get('summary[title="more options"]').click()
  cy.get('details[data-auto-collapse-dropdown]')
    .filter(':has(summary[title="more options"])')
    .should('have.prop', 'open', true)
  cy.findByRole('button', { name: 'Assimilation settings' }).should(
    'be.visible'
  )

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
      cy.findByRole('button', { name: 'Assimilation settings' })
        .scrollIntoView()
        .click()
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
