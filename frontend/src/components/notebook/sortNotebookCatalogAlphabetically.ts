import type { Notebook } from "@generated/doughnut-backend-api"
import type { NotebookCatalogEntry } from "@/components/notebook/patchNotebookInCatalogItems"

function compareTitles(a: string, b: string): number {
  return a.localeCompare(b, undefined, { sensitivity: "base" })
}

function compareNotebooks(a: Notebook, b: Notebook): number {
  const t = compareTitles(a.title ?? "", b.title ?? "")
  if (t !== 0) {
    return t
  }
  return a.id - b.id
}

function topLevelName(item: NotebookCatalogEntry): string {
  if (item.type === "notebook" || item.type === "subscribedNotebook") {
    return item.notebook.title ?? ""
  }
  return item.name
}

function topLevelTieId(item: NotebookCatalogEntry): number {
  if (item.type === "notebookGroup") {
    return item.id
  }
  return item.notebook.id
}

export function sortNotebookCatalogAlphabetically(
  items: NotebookCatalogEntry[]
): NotebookCatalogEntry[] {
  const sortedTop = [...items].sort((a, b) => {
    const n = compareTitles(topLevelName(a), topLevelName(b))
    if (n !== 0) {
      return n
    }
    return topLevelTieId(a) - topLevelTieId(b)
  })
  return sortedTop.map((item) => {
    if (item.type !== "notebookGroup") {
      return item
    }
    return {
      ...item,
      notebooks: [...item.notebooks].sort(compareNotebooks),
    }
  })
}
