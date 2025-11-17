import noteCreationForm from './noteForms/noteCreationForm'
import { bazaarOrCircle } from './BazaarOrCircle'
import { findNotebookCardButton } from './NotebookList'
import { navigateToMyCircles } from './myCirclesPage'

export const assumeCirclePage = () => ({
  creatingNotebook(notebookTopic: string) {
    cy.findByText('Add New Notebook In This Circle').click()
    return noteCreationForm.createNoteWithTitle(notebookTopic)
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
})

export const navigateToCircle = (circleName: string) => {
  navigateToMyCircles()
  cy.findByText(circleName, { selector: 'a' }).click()
  return assumeCirclePage()
}
