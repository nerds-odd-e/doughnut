import type { Folder } from "@generated/doughnut-backend-api"

export function folderRowsById(rows: Folder[]): Map<number, Folder> {
  return new Map(rows.map((r) => [r.id, r]))
}

/** Path from notebook root to folder, segments joined with ` / `. */
export function folderPathLabel(
  folderId: number,
  byId: Map<number, Folder>
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

/**
 * Copy a root-to-leaf Folder array with inferred parentFolderId on each entry
 * (outermost folder has no parent).
 */
export function folderChainWithParentIds(chain: readonly Folder[]): Folder[] {
  return chain.map((f, i) => ({
    id: f.id,
    name: f.name,
    createdAt: f.createdAt,
    updatedAt: f.updatedAt,
    parentFolderId: i === 0 ? undefined : chain[i - 1]!.id,
  }))
}

/**
 * From a root-to-leaf ancestor chain and the moving folder id, return the
 * subset that are ancestors of the moving folder (outermost first), and the
 * parent folder id. Returns `undefined` for parentFolderId when moving folder
 * is at notebook root or not found in the chain.
 */
export function ancestorsFromChain(
  movingFolderId: number,
  chain: readonly Folder[]
): { ancestorFolders: Folder[]; parentFolderId: number | null } {
  const idx = chain.findIndex((f) => f.id === movingFolderId)
  if (idx === -1) {
    // Moving folder not in chain – return chain as ancestors (best-effort)
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
