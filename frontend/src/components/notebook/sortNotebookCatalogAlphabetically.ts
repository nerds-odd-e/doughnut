import type { NotebookCatalogEntry } from "@/components/notebook/patchNotebookInCatalogItems"
import { sortNotebookCatalogByPeerSpec } from "@/components/notebook/sortNotebookCatalogByPeerSpec"

export function sortNotebookCatalogAlphabetically(
  items: NotebookCatalogEntry[]
): NotebookCatalogEntry[] {
  return sortNotebookCatalogByPeerSpec(items, {
    field: "title",
    direction: "asc",
  })
}
