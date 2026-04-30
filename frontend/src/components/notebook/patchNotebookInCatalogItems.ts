import type {
  Notebook,
  NotebookCatalogGroupItem,
  NotebookCatalogNotebookItem,
  NotebookCatalogSubscribedNotebookItem,
  NotebookClientView,
} from "@generated/doughnut-backend-api"

export type NotebookCatalogEntry =
  | NotebookCatalogGroupItem
  | NotebookCatalogNotebookItem
  | NotebookCatalogSubscribedNotebookItem

function patchGroupedMember(
  nb: NotebookClientView,
  updated: Notebook
): NotebookClientView {
  if (nb.notebook.id !== updated.id) return nb
  return { ...nb, notebook: { ...nb.notebook, ...updated } }
}

export function patchNotebookInCatalogItems(
  items: NotebookCatalogEntry[],
  updated: Notebook
): NotebookCatalogEntry[] {
  return items.map((item) => {
    if (item.type === "notebook" || item.type === "subscribedNotebook") {
      if (item.notebook.id !== updated.id) return item
      return {
        ...item,
        notebook: { ...item.notebook, ...updated },
        hasAttachedBook: item.hasAttachedBook,
      }
    }
    return {
      ...item,
      notebooks: item.notebooks.map((nb) => patchGroupedMember(nb, updated)),
    }
  })
}
