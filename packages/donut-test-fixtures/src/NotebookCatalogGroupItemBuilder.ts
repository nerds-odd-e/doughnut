import type {
  Notebook,
  NotebookCatalogGroupItem,
  NotebookRealm,
} from '@generated/doughnut-backend-api'
import Builder from './Builder'
import NotebookBuilder from './NotebookBuilder'
import generateId from './generateId'

class NotebookCatalogGroupItemBuilder extends Builder<NotebookCatalogGroupItem> {
  private groupId = generateId()
  private groupName = 'Notebook group'
  private groupCreatedAt = new Date().toISOString()
  private memberNotebooks: NotebookRealm[] = []

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
      this.memberNotebooks.push({ notebook: nb.do(), readonly: false })
    }
    return this
  }

  members(entries: NotebookRealm[]) {
    this.memberNotebooks = [...entries]
    return this
  }

  membersFromNotebooks(notebooks: Notebook[]) {
    this.memberNotebooks = notebooks.map((n) => ({
      notebook: n,
      readonly: false,
    }))
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
