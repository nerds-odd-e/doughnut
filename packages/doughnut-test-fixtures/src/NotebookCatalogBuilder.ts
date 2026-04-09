import type {
  Notebook,
  NotebookCatalogGroupItem,
  NotebookCatalogNotebookItem,
} from '@generated/doughnut-backend-api'
import Builder from './Builder'
import NotebookCatalogGroupItemBuilder from './NotebookCatalogGroupItemBuilder'
import NotebookCatalogNotebookItemBuilder from './NotebookCatalogNotebookItemBuilder'

export type NotebookCatalogEntry =
  | NotebookCatalogNotebookItem
  | NotebookCatalogGroupItem

class NotebookCatalogBuilder extends Builder<NotebookCatalogEntry[]> {
  private items: NotebookCatalogEntry[] = []

  notebook(title?: string) {
    const b = new NotebookCatalogNotebookItemBuilder()
    if (title !== undefined) {
      b.title(title)
    }
    this.items.push(b.do())
    return this
  }

  notebooks(...notebooks: Notebook[]) {
    for (const nb of notebooks) {
      this.items.push(
        new NotebookCatalogNotebookItemBuilder().forNotebook(nb).do()
      )
    }
    return this
  }

  group(name: string, ...memberTitles: string[]) {
    this.items.push(
      new NotebookCatalogGroupItemBuilder()
        .name(name)
        .titles(...memberTitles)
        .do()
    )
    return this
  }

  groupWithMembers(name: string, members: Notebook[]) {
    this.items.push(
      new NotebookCatalogGroupItemBuilder().name(name).members(members).do()
    )
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
