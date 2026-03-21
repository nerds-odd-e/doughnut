import type { BazaarNotebook, Notebook } from '@generated/doughnut-backend-api'
import Builder from './Builder'
import generateId from './generateId'

class NotebooksBuilder extends Builder<BazaarNotebook[]> {
  data: BazaarNotebook[] = []

  notebooks(notebook: Notebook) {
    this.data = [
      ...this.data,
      {
        id: generateId(),
        notebook,
      },
    ]
    return this
  }

  do(): BazaarNotebook[] {
    return this.data
  }
}

export default NotebooksBuilder
