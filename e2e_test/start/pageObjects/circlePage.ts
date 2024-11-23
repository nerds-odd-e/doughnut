import noteCreationForm from './noteForms/noteCreationForm'
import start from '../../start'
import { bazaarOrCircle } from './BazaarOrCircle'
import { findNotebookCardButton } from './NotebookList'

export const navigateToCircle = (circleName: string) => {
  start.systemSidebar()
  cy.findByText('My Circles').click()
  cy.findByText(circleName, { selector: 'a' }).click()

  return {
    creatingNotebook(notebookTopic: string) {
      cy.findByText('Add New Notebook In This Circle').click()
      return noteCreationForm.createNote(notebookTopic)
    },
    haveMembers(count: number) {
      cy.get('body').find('.circle-member').should('have.length', count)
    },
    moveNotebook(notebookTitle: string) {
      findNotebookCardButton(notebookTitle, 'Move to ...').click()
      return {
        toCircle(circleName: string) {
          cy.findByText(circleName).click()
          cy.findByText('OK').click()
        },
      }
    },
    ...bazaarOrCircle(),
  }
}
