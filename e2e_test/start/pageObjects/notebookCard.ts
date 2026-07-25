import { waitUntilAppIsNotBusy } from '../pageBase'
import {
  clickNotebookCardTitleToOpenNotebookPage,
  findNotebookCardButton,
  notebookList,
} from './NotebookList'
import notebookPage from './notebookPage'

export const notebookCard = (notebook: string) => ({
  ...notebookList(),
  openMoveToGroupDialog() {
    findNotebookCardButton(notebook, 'Move to group…').click()
  },
  updateSubscription() {
    findNotebookCardButton(notebook, 'Edit subscription').click()
  },
  unsubscribe() {
    findNotebookCardButton(notebook, 'Unsubscribe').click()
    cy.findByRole('button', { name: 'OK' }).click()
    waitUntilAppIsNotBusy()
  },
  openNotebookPage() {
    clickNotebookCardTitleToOpenNotebookPage(notebook)
    return notebookPage()
  },
})
