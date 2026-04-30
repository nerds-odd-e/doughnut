import type {
  Notebook,
  NotebookCatalogNotebookItem,
} from '@generated/doughnut-backend-api'
import Builder from './Builder'
import NotebookBuilder from './NotebookBuilder'

class NotebookCatalogNotebookItemBuilder extends Builder<NotebookCatalogNotebookItem> {
  private notebookBuilder = new NotebookBuilder()
  private notebookOverride: Notebook | undefined
  private hasAttachedBookFlag: boolean | undefined

  forNotebook(notebook: Notebook) {
    this.notebookOverride = notebook
    return this
  }

  name(value: string) {
    this.notebookBuilder.notebuilder.title(value)
    return this
  }

  hasAttachedBook(value: boolean) {
    this.hasAttachedBookFlag = value
    return this
  }

  do(): NotebookCatalogNotebookItem {
    const notebook = this.notebookOverride ?? this.notebookBuilder.do()
    const item: NotebookCatalogNotebookItem = { type: 'notebook', notebook }
    if (this.hasAttachedBookFlag !== undefined) {
      item.hasAttachedBook = this.hasAttachedBookFlag
    }
    return item
  }
}

export default NotebookCatalogNotebookItemBuilder
