import type {
  CircleForUserView,
  Notebook,
  NotebookCatalogNotebookItem,
} from '@generated/doughnut-backend-api'
import NotebooksBuilder from './BazaarNotebooksBuilder'
import Builder from './Builder'
import generateId from './generateId'

class CircleNoteBuilder extends Builder<CircleForUserView> {
  notebooksBuilder: NotebooksBuilder = new NotebooksBuilder()

  notebooks(notebook: Notebook) {
    this.notebooksBuilder.notebooks(notebook)
    return this
  }

  do(): CircleForUserView {
    const notebooks = this.notebooksBuilder
      .do()
      .map((bazaarNotebook) => bazaarNotebook.notebook)
    const catalogItems: NotebookCatalogNotebookItem[] = notebooks.map(
      (notebook) => ({
        type: 'notebook' as const,
        notebook,
      }),
    )
    return {
      id: generateId(),
      name: '',
      invitationCode: '',
      notebooks: {
        notebooks: notebooks.map((notebook) => ({ notebook })),
        catalogItems,
      },
      members: [],
    }
  }
}

export default CircleNoteBuilder
