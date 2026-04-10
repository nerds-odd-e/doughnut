import noteCreationForm from './noteForms/noteCreationForm'
import { bazaarOrCircle } from './BazaarOrCircle'
import { findNotebookCardButton } from './NotebookList'
import { pageIsNotLoading } from '../pageBase'
import { navigateToMyCircles } from './myCirclesPage'
import notebookPage from './notebookPage'

const completeMoveNotebookToNewGroupDialog = (newGroupName: string) => {
  cy.findByRole('dialog', { name: 'Move to group' }).within(() => {
    cy.get('#notebook-catalog-move-to-group-target').select('new')
    cy.findByLabelText('New group name').type(newGroupName)
    cy.findByRole('button', { name: 'Move' }).click()
  })
  pageIsNotLoading()
}

export const assumeCirclePage = () => ({
  creatingNotebook(notebookTopic: string) {
    cy.findByText('Add New Notebook In This Circle').click()
    return noteCreationForm.createNoteWithTitle(notebookTopic)
  },
  haveMembers(count: number) {
    cy.get('body').find('.circle-member').should('have.length', count)
  },
  expectCatalogLayoutControls() {
    cy.findByRole('button', { name: 'Grid view' }).should('be.visible')
    cy.findByRole('button', { name: 'List view' }).should('be.visible')
    return this as any
  },
  creatingNotebookGroupFromCatalogMove(
    notebookTitle: string,
    groupName: string
  ) {
    findNotebookCardButton(notebookTitle, 'Move to group…').click()
    completeMoveNotebookToNewGroupDialog(groupName)
    return this as any
  },
  moveNotebook(notebookTitle: string) {
    findNotebookCardButton(notebookTitle, 'Edit notebook settings').click()
    notebookPage().moveNotebookToCircle()
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
