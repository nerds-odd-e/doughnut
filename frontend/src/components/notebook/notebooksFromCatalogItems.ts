import type { Notebook } from "@generated/doughnut-backend-api"
import type { NotebookCatalogEntry } from "@/components/notebook/patchNotebookInCatalogItems"

export function notebooksFromCatalogItems(
  items: NotebookCatalogEntry[]
): Notebook[] {
  const out: Notebook[] = []
  const seen = new Set<number>()
  for (const item of items) {
    if (item.type === "notebook" || item.type === "subscribedNotebook") {
      if (!seen.has(item.notebook.id)) {
        seen.add(item.notebook.id)
        out.push(item.notebook)
      }
      continue
    }
    for (const member of item.notebooks) {
      if (!seen.has(member.notebook.id)) {
        seen.add(member.notebook.id)
        out.push(member.notebook)
      }
    }
  }
  return out
}
