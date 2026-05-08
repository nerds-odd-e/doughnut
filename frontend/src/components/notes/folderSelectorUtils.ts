import type { NotebookFolderIndexRow } from "@generated/doughnut-backend-api"

export function folderRowsById(
  rows: NotebookFolderIndexRow[]
): Map<number, NotebookFolderIndexRow> {
  return new Map(rows.map((r) => [r.id, r]))
}

/** Path from notebook root to folder, segments joined with ` / `. */
export function folderPathLabel(
  folderId: number,
  byId: Map<number, NotebookFolderIndexRow>
): string {
  const segments: string[] = []
  let id: number | undefined = folderId
  while (id != null) {
    const row = byId.get(id)
    if (!row) break
    segments.push(row.name)
    id = row.parentFolderId
  }
  return segments.reverse().join(" / ")
}

/** Outermost ancestor first: chain from root down to parent of `contextFolderId`. */
export function ancestorFolderIdsOutermostFirst(
  contextFolderId: number,
  byId: Map<number, NotebookFolderIndexRow>
): number[] {
  const ctx = byId.get(contextFolderId)
  if (!ctx) return []
  let p = ctx.parentFolderId
  const innerToOuter: number[] = []
  while (p != null) {
    innerToOuter.push(p)
    const parent = byId.get(p)
    p = parent?.parentFolderId
  }
  innerToOuter.reverse()
  return innerToOuter
}

export function siblingFolderIds(
  contextFolderId: number,
  rows: NotebookFolderIndexRow[],
  byId: Map<number, NotebookFolderIndexRow>
): number[] {
  const ctx = byId.get(contextFolderId)
  if (!ctx) return []
  const parentKey = ctx.parentFolderId
  return rows
    .filter(
      (r) =>
        r.id !== contextFolderId &&
        (r.parentFolderId ?? undefined) === (parentKey ?? undefined)
    )
    .map((r) => r.id)
}

export function collectSubtreeFolderIds(
  rootId: number,
  rows: NotebookFolderIndexRow[]
): Set<number> {
  const excluded = new Set<number>()
  function dfs(id: number) {
    excluded.add(id)
    for (const r of rows) {
      if (r.parentFolderId === id) dfs(r.id)
    }
  }
  dfs(rootId)
  return excluded
}

export function dissolveParentQuotedLabel(
  movingFolderId: number,
  byId: Map<number, NotebookFolderIndexRow>
): string {
  const moving = byId.get(movingFolderId)
  if (!moving || moving.parentFolderId == null) return "notebook root"
  const path = folderPathLabel(moving.parentFolderId, byId)
  return `"${path}"`
}
