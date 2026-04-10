import type { NotebookCatalogGroupItem } from "@generated/doughnut-backend-api"

export function narrowGroupNotebooksForCatalogFilter(
  group: NotebookCatalogGroupItem,
  q: string
): NotebookCatalogGroupItem["notebooks"] {
  if (!q) {
    return group.notebooks
  }
  if (group.name.toLowerCase().includes(q)) {
    return group.notebooks
  }
  return group.notebooks.filter((nb) =>
    (nb.title ?? "").toLowerCase().includes(q)
  )
}
