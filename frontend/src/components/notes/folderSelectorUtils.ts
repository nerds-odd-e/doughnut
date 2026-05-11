import type {
  Folder,
  NotebookFolderIndexRow,
} from "@generated/doughnut-backend-api"

/** Timestamps when only index-row data is available (e.g. search pick). */
const FOLDER_PLACEHOLDER_TIMESTAMP = "1970-01-01T00:00:00.000Z"

/** Build a `Folder` from an index row when the listing API did not return a full row. */
export function folderFromIndexRow(row: NotebookFolderIndexRow): Folder {
  return {
    id: row.id,
    name: row.name,
    createdAt: FOLDER_PLACEHOLDER_TIMESTAMP,
    updatedAt: FOLDER_PLACEHOLDER_TIMESTAMP,
  }
}

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

/**
 * Convert a root-to-leaf Folder array (as stored in NoteRealm.ancestorFolders)
 * into NotebookFolderIndexRow entries with inferred parentFolderId.
 */
export function folderChainToIndexRows(
  chain: readonly Folder[]
): NotebookFolderIndexRow[] {
  return chain.map((f, i) => ({
    id: f.id,
    name: f.name,
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
): { ancestorRows: NotebookFolderIndexRow[]; parentFolderId: number | null } {
  const idx = chain.findIndex((f) => f.id === movingFolderId)
  if (idx === -1) {
    // Moving folder not in chain – return chain as ancestors (best-effort)
    return {
      ancestorRows: folderChainToIndexRows(chain),
      parentFolderId: chain.length > 0 ? chain[chain.length - 1]!.id : null,
    }
  }
  const ancestors = chain.slice(0, idx)
  return {
    ancestorRows: folderChainToIndexRows(ancestors),
    parentFolderId: idx > 0 ? chain[idx - 1]!.id : null,
  }
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

/**
 * Quoted path label of the parent of the moving folder, for the dissolve
 * confirmation text. Derived purely from the ancestor chain.
 */
export function dissolveParentLabelFromChain(
  movingFolderId: number,
  chain: readonly Folder[]
): string {
  const idx = chain.findIndex((f) => f.id === movingFolderId)
  if (idx <= 0) return "notebook root"
  const parentChain = chain.slice(0, idx)
  return `"${parentChain.map((f) => f.name).join(" / ")}"`
}
