import type { Folder, NoteTopology } from "@generated/doughnut-backend-api"
import type { SidebarPeerSortSpec } from "@/composables/useNoteSidebarPeerSort"

export type SidebarStructuralRow =
  | { kind: "note"; noteTopology: NoteTopology }
  | { kind: "folder"; folder: Folder }

function parseTime(iso: string | undefined): number {
  if (iso == null || iso === "") return Number.NaN
  const t = Date.parse(iso)
  return Number.isNaN(t) ? Number.NaN : t
}

function peerTitle(row: SidebarStructuralRow): string {
  return row.kind === "folder"
    ? row.folder.name.toLocaleLowerCase()
    : row.noteTopology.title.toLocaleLowerCase()
}

function peerCreated(row: SidebarStructuralRow): number {
  const iso =
    row.kind === "folder" ? row.folder.createdAt : row.noteTopology.createdAt
  return parseTime(iso)
}

function peerUpdated(row: SidebarStructuralRow): number {
  const iso =
    row.kind === "folder" ? row.folder.updatedAt : row.noteTopology.updatedAt
  return parseTime(iso)
}

function tieBreak(a: SidebarStructuralRow, b: SidebarStructuralRow): number {
  const idA = a.kind === "folder" ? a.folder.id : a.noteTopology.id
  const idB = b.kind === "folder" ? b.folder.id : b.noteTopology.id
  return idA - idB
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

function compare(
  a: SidebarStructuralRow,
  b: SidebarStructuralRow,
  spec: SidebarPeerSortSpec
): number {
  let cmp = 0
  if (spec.field === "title") {
    cmp = peerTitle(a).localeCompare(peerTitle(b))
    if (cmp !== 0) {
      return spec.direction === "asc" ? cmp : -cmp
    }
    return tieBreak(a, b)
  }
  if (spec.field === "created") {
    const d = compareDates(peerCreated(a), peerCreated(b), spec.direction)
    if (d !== 0) return d
    return tieBreak(a, b)
  }
  const d = compareDates(peerUpdated(a), peerUpdated(b), spec.direction)
  if (d !== 0) return d
  return tieBreak(a, b)
}

export function sortSidebarStructuralRows(
  rows: SidebarStructuralRow[],
  spec: SidebarPeerSortSpec
): SidebarStructuralRow[] {
  type FolderRow = Extract<SidebarStructuralRow, { kind: "folder" }>
  type NoteRow = Extract<SidebarStructuralRow, { kind: "note" }>

  const folderRows = rows.filter((r): r is FolderRow => r.kind === "folder")
  const noteRows = rows.filter((r): r is NoteRow => r.kind === "note")

  folderRows.sort((a, b) => compare(a, b, spec))
  noteRows.sort((a, b) => compare(a, b, spec))

  return [...folderRows, ...noteRows]
}

export function buildUnsortedStructuralRows(
  noteTopologies: NoteTopology[],
  folders: Folder[] | undefined
): SidebarStructuralRow[] {
  type FolderRow = Extract<SidebarStructuralRow, { kind: "folder" }>
  type NoteRow = Extract<SidebarStructuralRow, { kind: "note" }>

  const folderRows: FolderRow[] = []
  for (const folder of folders ?? []) {
    if (folder.id !== undefined) {
      folderRows.push({ kind: "folder", folder })
    }
  }

  const noteRows: NoteRow[] = noteTopologies.map((noteTopology) => ({
    kind: "note" as const,
    noteTopology,
  }))

  return [...folderRows, ...noteRows]
}
