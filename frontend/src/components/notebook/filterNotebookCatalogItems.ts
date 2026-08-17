import type { NotebookCatalogEntry } from "@/components/notebook/patchNotebookInCatalogItems"
import { narrowGroupNotebooksForCatalogFilter } from "@/components/notebook/narrowGroupNotebooksForCatalogFilter"

export function filterNotebookCatalogItems(
  catalogItems: NotebookCatalogEntry[],
  q: string
): NotebookCatalogEntry[] {
  if (!q) {
    return catalogItems
  }
  const rows = catalogItems.filter((item) => {
    if (item.type === "notebook" || item.type === "subscribedNotebook") {
      return (item.notebook.name ?? "").toLowerCase().includes(q)
    }
    if (item.name.toLowerCase().includes(q)) {
      return true
    }
    return item.notebooks.some((nb) =>
      (nb.notebook.name ?? "").toLowerCase().includes(q)
    )
  })
  return rows.map((item) => {
    if (item.type !== "notebookGroup") {
      return item
    }
    return {
      ...item,
      notebooks: narrowGroupNotebooksForCatalogFilter(item, q),
    }
  })
}
