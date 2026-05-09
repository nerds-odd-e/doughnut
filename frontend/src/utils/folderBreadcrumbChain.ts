import type {
  Folder,
  NotebookFolderIndexRow,
} from "@generated/doughnut-backend-api"

const PLACEHOLDER_ISO = "1970-01-01T00:00:00.000Z"

function rowToFolder(row: NotebookFolderIndexRow): Folder {
  return {
    id: row.id,
    name: row.name,
    createdAt: PLACEHOLDER_ISO,
    updatedAt: PLACEHOLDER_ISO,
  }
}

/** Builds root-to-leaf folder segments for breadcrumbs using the flat folder index. */
export function folderBreadcrumbChainFromFlatIndex(
  leafFolder: Folder,
  rows: NotebookFolderIndexRow[]
): Folder[] {
  const byId = new Map(rows.map((r) => [r.id, r]))
  const idsRev: number[] = []
  let cur: number | undefined = leafFolder.id
  const guard = new Set<number>()
  while (cur != null && !guard.has(cur)) {
    guard.add(cur)
    idsRev.push(cur)
    const row = byId.get(cur)
    cur = row?.parentFolderId
  }
  const ids = idsRev.reverse()
  return ids.map((id) =>
    id === leafFolder.id ? leafFolder : rowToFolder(byId.get(id)!)
  )
}
