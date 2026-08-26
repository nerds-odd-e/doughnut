import type {
  Notebook,
  NotebookCatalogSubscribedNotebookItem,
} from '@generated/doughnut-backend-api'
import Builder from './Builder'
import NotebookBuilder from './NotebookBuilder'
import generateId from './generateId'

class NotebookCatalogSubscribedNotebookItemBuilder extends Builder<NotebookCatalogSubscribedNotebookItem> {
  private notebookBuilder = new NotebookBuilder()
  private notebookOverride: Notebook | undefined
  private subId = generateId()

  forNotebook(notebook: Notebook) {
    this.notebookOverride = notebook
    return this
  }

  subscriptionId(value: number) {
    this.subId = value
    return this
  }

  do(): NotebookCatalogSubscribedNotebookItem {
    const notebook = this.notebookOverride ?? this.notebookBuilder.do()
    return { type: 'subscribedNotebook', notebook, subscriptionId: this.subId }
  }
}

export default NotebookCatalogSubscribedNotebookItemBuilder
