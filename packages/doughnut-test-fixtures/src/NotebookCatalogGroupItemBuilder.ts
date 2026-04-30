import type {
  Notebook,
  NotebookCatalogGroupItem,
  NotebookClientView,
} from '@generated/doughnut-backend-api'
import Builder from './Builder'
import NotebookBuilder from './NotebookBuilder'
import generateId from './generateId'

class NotebookCatalogGroupItemBuilder extends Builder<NotebookCatalogGroupItem> {
  private groupId = generateId()
  private groupName = 'Notebook group'
  private groupCreatedAt = new Date().toISOString()
  private memberNotebooks: NotebookClientView[] = []

  name(value: string) {
    this.groupName = value
    return this
  }

  id(value: number) {
    this.groupId = value
    return this
  }

  createdAt(value: string) {
    this.groupCreatedAt = value
    return this
  }

  names(...memberNames: string[]) {
    for (const n of memberNames) {
      const nb = new NotebookBuilder()
      nb.notebuilder.title(n)
      this.memberNotebooks.push({ notebook: nb.do() })
    }
    return this
  }

  members(entries: NotebookClientView[]) {
    this.memberNotebooks = [...entries]
    return this
  }

  membersFromNotebooks(notebooks: Notebook[]) {
    this.memberNotebooks = notebooks.map((n) => ({ notebook: n }))
    return this
  }

  do(): NotebookCatalogGroupItem {
    return {
      type: 'notebookGroup',
      id: this.groupId,
      name: this.groupName,
      createdAt: this.groupCreatedAt,
      notebooks: this.memberNotebooks,
    }
  }
}

export default NotebookCatalogGroupItemBuilder
