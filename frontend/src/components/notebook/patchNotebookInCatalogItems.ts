import type {
  Notebook,
  NotebookCatalogGroupItem,
  NotebookCatalogNotebookItem,
} from "@generated/doughnut-backend-api"

export type NotebookCatalogEntry =
  | NotebookCatalogGroupItem
  | NotebookCatalogNotebookItem

export function patchNotebookInCatalogItems(
  items: NotebookCatalogEntry[],
  updated: Notebook
): NotebookCatalogEntry[] {
  return items.map((item) => {
    if (item.type === "notebook") {
      if (item.notebook.id === updated.id) {
        return { ...item, notebook: updated }
      }
      return item
    }
    return {
      ...item,
      notebooks: item.notebooks.map((nb) =>
        nb.id === updated.id ? updated : nb
      ),
    }
  })
}
