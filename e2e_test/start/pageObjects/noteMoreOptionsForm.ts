import { pageIsNotLoading } from '../pageBase'
import { assumeAssimilationPage } from './assimilationPage'
import { toolbarButton } from './toolbarButton'
import { questionListPage } from './questionListPage'

const moreOptionsDetails = () =>
  cy
    .get('details[data-auto-collapse-dropdown]')
    .filter(':has(summary[title="more options"])')

const isMoreOptionsDropdownOpen = ($details: JQuery<HTMLElement>) =>
  $details.prop('open') === true || $details.hasClass('daisy-dropdown-open')

export const makeSureNoteMoreOptionsFormIsOpen = () => {
  moreOptionsDetails().then(($details) => {
    if (!isMoreOptionsDropdownOpen($details)) {
      cy.wrap($details).find('summary[title="more options"]').click()
    }
  })
  moreOptionsDetails().should(($details) => {
    expect(isMoreOptionsDropdownOpen($details)).to.eq(true)
  })
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
