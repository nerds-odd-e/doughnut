import type {
  Notebook,
  NotebookCatalogNotebookItem,
} from '@generated/doughnut-backend-api'
import Builder from './Builder'
import NotebookBuilder from './NotebookBuilder'

class NotebookCatalogNotebookItemBuilder extends Builder<NotebookCatalogNotebookItem> {
  private notebookBuilder = new NotebookBuilder()
  private notebookOverride: Notebook | undefined

  forNotebook(notebook: Notebook) {
    this.notebookOverride = notebook
    return this
  }

  title(value: string) {
    this.notebookBuilder.notebuilder.title(value)
    return this
  }

  do(): NotebookCatalogNotebookItem {
    const notebook = this.notebookOverride ?? this.notebookBuilder.do()
    return { type: 'notebook', notebook }
  }
}

export default NotebookCatalogNotebookItemBuilder
