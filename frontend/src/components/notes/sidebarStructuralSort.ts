import type {
  Folder,
  FolderListing,
  NoteTopology,
  NoteRealm,
  NotebookAttachmentListItem,
} from "@generated/donut-backend-api"
import { locationAfterNoteRemoval } from "@/routes/containingLocation"
import { loadFolderListing } from "@/utils/notebookFolderListingRequest"
import { usePeerSort } from "@/composables/usePeerSort"
import type { PeerSortSpec } from "@/composables/usePeerSort"

export type SidebarStructuralRow =
  | { kind: "note"; noteTopology: NoteTopology }
  | { kind: "folder"; folder: Folder }
  | { kind: "attachment"; attachment: NotebookAttachmentListItem }

function parseTime(iso: string | undefined): number {
  if (iso == null || iso === "") return Number.NaN
  const t = Date.parse(iso)
  return Number.isNaN(t) ? Number.NaN : t
}

function peerTitle(row: SidebarStructuralRow): string {
  if (row.kind === "folder") return row.folder.name.toLocaleLowerCase()
  if (row.kind === "note") return row.noteTopology.title.toLocaleLowerCase()
  return row.attachment.filename.toLocaleLowerCase()
}

function peerDates(row: SidebarStructuralRow): {
  createdAt?: string
  updatedAt?: string
} {
  if (row.kind === "folder") return row.folder
  if (row.kind === "note") return row.noteTopology
  return {}
}

function peerId(row: SidebarStructuralRow): number {
  if (row.kind === "folder") return row.folder.id
  if (row.kind === "note") return row.noteTopology.id
  return row.attachment.id
}

function tieBreak(a: SidebarStructuralRow, b: SidebarStructuralRow): number {
  return peerId(a) - peerId(b)
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
  spec: PeerSortSpec
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
    const d = compareDates(
      parseTime(peerDates(a).createdAt),
      parseTime(peerDates(b).createdAt),
      spec.direction
    )
    if (d !== 0) return d
    return tieBreak(a, b)
  }
  const d = compareDates(
    parseTime(peerDates(a).updatedAt),
    parseTime(peerDates(b).updatedAt),
    spec.direction
  )
  if (d !== 0) return d
  return tieBreak(a, b)
}

export function sortSidebarStructuralRows(
  rows: SidebarStructuralRow[],
  spec: PeerSortSpec
): SidebarStructuralRow[] {
  const folderRows = rows.filter((r) => r.kind === "folder")
  const leafRows = rows.filter((r) => r.kind !== "folder")

  folderRows.sort((a, b) => compare(a, b, spec))
  leafRows.sort((a, b) => compare(a, b, spec))

  return [...folderRows, ...leafRows]
}

export function buildUnsortedStructuralRows({
  noteTopologies,
  folders,
  attachments,
}: FolderListing): SidebarStructuralRow[] {
  type FolderRow = Extract<SidebarStructuralRow, { kind: "folder" }>
  type NoteRow = Extract<SidebarStructuralRow, { kind: "note" }>

  const folderRows: FolderRow[] = []
  for (const folder of folders ?? []) {
    if (folder.id !== undefined) {
      folderRows.push({ kind: "folder", folder })
    }
  }

  const noteRows: NoteRow[] = (noteTopologies ?? []).map((noteTopology) => ({
    kind: "note" as const,
    noteTopology,
  }))

  const attachmentRows = (attachments ?? []).map((attachment) => ({
    kind: "attachment" as const,
    attachment,
  }))

  return [...folderRows, ...noteRows, ...attachmentRows]
}

/** The note to show once `removedNoteId` leaves these peers: the note after it in sidebar order, else the one before. */
export function neighborNoteAfterRemoval(
  listing: FolderListing,
  removedNoteId: number,
  spec: PeerSortSpec
): NoteTopology | undefined {
  const notes = sortSidebarStructuralRows(
    buildUnsortedStructuralRows(listing),
    spec
  ).flatMap((row) => (row.kind === "note" ? [row.noteTopology] : []))
  const index = notes.findIndex((note) => note.id === removedNoteId)
  return notes[index + 1] ?? notes[index - 1]
}

/** Read before removal, using the same peer ordering as the visible sidebar. */
export async function locationAfterRemoving(realm: NoteRealm) {
  const listing = await loadFolderListing(
    realm.notebookRealm.notebook.id,
    realm.ancestorFolders?.at(-1)?.id ?? null
  )
  const neighbor = neighborNoteAfterRemoval(
    listing,
    realm.id,
    usePeerSort().peerSortSpec.value
  )
  return locationAfterNoteRemoval(realm, neighbor)
}
