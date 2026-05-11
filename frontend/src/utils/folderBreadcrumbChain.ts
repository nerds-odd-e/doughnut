import type { Folder } from "@generated/doughnut-backend-api"

/** Builds root-to-leaf folder segments for breadcrumbs using the flat folder index. */
export function folderBreadcrumbChainFromFlatIndex(
  leafFolder: Folder,
  rows: Folder[]
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
  return ids.map((id) => (id === leafFolder.id ? leafFolder : byId.get(id)!))
}
