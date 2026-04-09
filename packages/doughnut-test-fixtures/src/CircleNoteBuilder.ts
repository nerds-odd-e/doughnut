import type {
  CircleForUserView,
  Notebook,
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
    return {
      id: generateId(),
      name: '',
      invitationCode: '',
      notebooks: {
        notebooks,
        catalogItems: notebooks.map((notebook) => ({
          type: 'notebook' as const,
          notebook,
        })),
      },
      members: [],
    }
  }
}

export default CircleNoteBuilder
