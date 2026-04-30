import type {
  Notebook,
  NotebookCatalogGroupItem,
  NotebookCatalogNotebookItem,
  NotebookCatalogSubscribedNotebookItem,
} from '@generated/doughnut-backend-api'
import Builder from './Builder'
import NotebookCatalogGroupItemBuilder from './NotebookCatalogGroupItemBuilder'
import NotebookCatalogNotebookItemBuilder from './NotebookCatalogNotebookItemBuilder'
import NotebookCatalogSubscribedNotebookItemBuilder from './NotebookCatalogSubscribedNotebookItemBuilder'

export type NotebookCatalogEntry =
  | NotebookCatalogNotebookItem
  | NotebookCatalogGroupItem
  | NotebookCatalogSubscribedNotebookItem

class NotebookCatalogBuilder extends Builder<NotebookCatalogEntry[]> {
  private items: NotebookCatalogEntry[] = []

  notebook(name?: string) {
    const b = new NotebookCatalogNotebookItemBuilder()
    if (name !== undefined) {
      b.name(name)
    }
    this.items.push(b.do())
    return this
  }

  notebooks(...inputs: Array<Notebook & { hasAttachedBook?: boolean }>) {
    for (const raw of inputs) {
      const { hasAttachedBook, ...nb } = raw
      const b = new NotebookCatalogNotebookItemBuilder().forNotebook(
        nb as Notebook
      )
      if (hasAttachedBook !== undefined) {
        b.hasAttachedBook(hasAttachedBook)
      }
      this.items.push(b.do())
    }
    return this
  }

  group(name: string, ...memberNames: string[]) {
    this.items.push(
      new NotebookCatalogGroupItemBuilder()
        .name(name)
        .names(...memberNames)
        .do()
    )
    return this
  }

  groupWithMembers(name: string, members: Notebook[]) {
    this.items.push(
      new NotebookCatalogGroupItemBuilder()
        .name(name)
        .membersFromNotebooks(members)
        .do()
    )
    return this
  }

  subscribedNotebook(notebook?: Notebook) {
    const b = new NotebookCatalogSubscribedNotebookItemBuilder()
    if (notebook !== undefined) {
      b.forNotebook(notebook)
    }
    this.items.push(b.do())
    return this
  }

  entry(item: NotebookCatalogEntry) {
    this.items.push(item)
    return this
  }

  do(): NotebookCatalogEntry[] {
    return this.items
  }
}

export default NotebookCatalogBuilder
