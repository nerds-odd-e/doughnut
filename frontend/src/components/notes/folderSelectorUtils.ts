import type { Folder, FolderTrailSegment } from "@generated/donut-backend-api"

/** A folder with the parent link needed to label its path. */
export type FolderPathRow = FolderTrailSegment & { parentFolderId?: number }

export function folderRowsById(
  rows: readonly FolderPathRow[]
): Map<number, FolderPathRow> {
  return new Map(rows.map((r) => [r.id, r]))
}

/** Sibling folders for the quick-pick dropdown, with parent links for path labels. */
export function siblingFoldersForQuickPick(
  neighbours: readonly Folder[],
  parentFolderId: number | null
): Folder[] {
  return neighbours
    .filter((f) => f.id !== parentFolderId)
    .map((f) => ({
      ...f,
      parentFolderId: parentFolderId ?? undefined,
    }))
}

/** Path from notebook root to folder, segments joined with ` / `. */
export function folderPathLabel(
  folderId: number,
  byId: Map<number, FolderPathRow>
): string {
  const segments: string[] = []
  const visited = new Set<number>()
  let id: number | undefined = folderId
  while (id != null) {
    if (visited.has(id)) break
    visited.add(id)
    const row = byId.get(id)
    if (!row) break
    segments.push(row.name)
    id = row.parentFolderId
  }
  return segments.reverse().join(" / ")
}

/**
 * Copy a root-to-leaf folder trail with inferred parentFolderId on each entry
 * (outermost folder has no parent).
 */
export function folderChainWithParentIds(
  chain: readonly FolderTrailSegment[]
): FolderPathRow[] {
  return chain.map((f, i) => ({
    id: f.id,
    name: f.name,
    parentFolderId: i === 0 ? undefined : chain[i - 1]!.id,
  }))
}

/**
 * From a root-to-leaf ancestor chain and a folder id in that chain, return
 * ancestors of that folder (outermost first) and its parent folder id.
 * `parentFolderId` is null when the folder is at notebook root or not in chain.
 */
export function ancestorsFromChain(
  folderId: number,
  chain: readonly FolderTrailSegment[]
): { ancestorFolders: FolderPathRow[]; parentFolderId: number | null } {
  const idx = chain.findIndex((f) => f.id === folderId)
  if (idx === -1) {
    // Folder not in chain – return chain as ancestors (best-effort)
    return {
      ancestorFolders: folderChainWithParentIds(chain),
      parentFolderId: chain.length > 0 ? chain[chain.length - 1]!.id : null,
    }
  }
  const ancestors = chain.slice(0, idx)
  return {
    ancestorFolders: folderChainWithParentIds(ancestors),
    parentFolderId: idx > 0 ? chain[idx - 1]!.id : null,
  }
}

export function collectSubtreeFolderIds(
  rootId: number,
  rows: Folder[]
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
