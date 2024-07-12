import { bazaarOrCircle } from './NotebookList'
import noteCreationForm from './noteForms/noteCreationForm'
import start from '../../start'

export const navigateToCircle = (circleName: string) => {
  start.systemSidebar()
  cy.findByText(circleName, { selector: '.modal-body a' }).click()

  return {
    creatingNotebook(notebookTopic: string) {
      cy.findByText('Add New Notebook In This Circle').click()
      return noteCreationForm.createNote(notebookTopic, undefined)
    },
    haveMembers(count: number) {
      cy.get('body').find('.circle-member').should('have.length', count)
    },
    ...bazaarOrCircle(),
  }
}
