import type { NotebookClientView } from "@generated/doughnut-backend-api"
import type { NotebookCatalogEntry } from "@/components/notebook/patchNotebookInCatalogItems"
import type { SidebarPeerSortSpec } from "@/composables/useNoteSidebarPeerSort"

function parseTime(iso: string | undefined): number {
  if (iso == null || iso === "") return Number.NaN
  const t = Date.parse(iso)
  return Number.isNaN(t) ? Number.NaN : t
}

function compareDates(
  ta: number,
  tb: number,
  direction: "asc" | "desc"
): number {
  const na = Number.isNaN(ta)
  const nb = Number.isNaN(tb)
  if (na && nb) return 0
  if (na) return 1
  if (nb) return -1
  const diff = ta - tb
  if (diff === 0) return 0
  return direction === "asc" ? diff : -diff
}

function topLevelTitle(item: NotebookCatalogEntry): string {
  if (item.type === "notebook" || item.type === "subscribedNotebook") {
    return item.notebook.name ?? ""
  }
  return item.name
}

function topLevelTieId(item: NotebookCatalogEntry): number {
  if (item.type === "notebookGroup") {
    return item.id
  }
  return item.notebook.id
}

function catalogCreatedTime(item: NotebookCatalogEntry): number {
  if (item.type === "notebookGroup") {
    return parseTime(item.createdAt)
  }
  return parseTime(item.notebook.createdAt)
}

function catalogUpdatedTime(item: NotebookCatalogEntry): number {
  if (item.type === "notebookGroup") {
    let best = parseTime(item.createdAt)
    for (const nb of item.notebooks) {
      const u = parseTime(nb.notebook.updatedAt)
      if (!Number.isNaN(u) && (Number.isNaN(best) || u > best)) {
        best = u
      }
    }
    return best
  }
  return parseTime(item.notebook.updatedAt)
}

function compareCatalogEntries(
  a: NotebookCatalogEntry,
  b: NotebookCatalogEntry,
  spec: SidebarPeerSortSpec
): number {
  if (spec.field === "title") {
    const cmp = topLevelTitle(a).localeCompare(topLevelTitle(b), undefined, {
      sensitivity: "base",
    })
    if (cmp !== 0) {
      return spec.direction === "asc" ? cmp : -cmp
    }
    return topLevelTieId(a) - topLevelTieId(b)
  }
  if (spec.field === "created") {
    const d = compareDates(
      catalogCreatedTime(a),
      catalogCreatedTime(b),
      spec.direction
    )
    if (d !== 0) return d
    return topLevelTieId(a) - topLevelTieId(b)
  }
  const d = compareDates(
    catalogUpdatedTime(a),
    catalogUpdatedTime(b),
    spec.direction
  )
  if (d !== 0) return d
  return topLevelTieId(a) - topLevelTieId(b)
}

function compareGroupMembers(
  a: NotebookClientView,
  b: NotebookClientView,
  spec: SidebarPeerSortSpec
): number {
  if (spec.field === "title") {
    const ta = a.notebook.name ?? ""
    const tb = b.notebook.name ?? ""
    const cmp = ta.localeCompare(tb, undefined, { sensitivity: "base" })
    if (cmp !== 0) {
      return spec.direction === "asc" ? cmp : -cmp
    }
    return a.notebook.id - b.notebook.id
  }
  if (spec.field === "created") {
    const d = compareDates(
      parseTime(a.notebook.createdAt),
      parseTime(b.notebook.createdAt),
      spec.direction
    )
    if (d !== 0) return d
    return a.notebook.id - b.notebook.id
  }
  const d = compareDates(
    parseTime(a.notebook.updatedAt),
    parseTime(b.notebook.updatedAt),
    spec.direction
  )
  if (d !== 0) return d
  return a.notebook.id - b.notebook.id
}

export function sortNotebookCatalogByPeerSpec(
  items: NotebookCatalogEntry[],
  spec: SidebarPeerSortSpec
): NotebookCatalogEntry[] {
  const sortedTop = [...items].sort((a, b) => compareCatalogEntries(a, b, spec))
  return sortedTop.map((item) => {
    if (item.type !== "notebookGroup") {
      return item
    }
    return {
      ...item,
      notebooks: [...item.notebooks].sort((a, b) =>
        compareGroupMembers(a, b, spec)
      ),
    }
  })
}
