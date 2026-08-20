import { waitUntilAppIsNotBusy } from '../pageBase'
import {
  clickNotebookCardTitleToOpenNotebookPage,
  findNotebookCardButton,
  NOTEBOOK_CATALOG_EXPORT_ACTION,
  notebookList,
} from './NotebookList'
import { downloadedNotebookZip } from './notebookExportZip'
import notebookPage from './notebookPage'

export const notebookCard = (notebook: string) => ({
  ...notebookList(),
  openMoveToGroupDialog() {
    findNotebookCardButton(notebook, 'Move to group…').click()
  },
  exportNotebook() {
    downloadedNotebookZip(notebook).clearBeforeExport()
    findNotebookCardButton(notebook, NOTEBOOK_CATALOG_EXPORT_ACTION).click()
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
