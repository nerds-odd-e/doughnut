/**
 * Returns folder ids on the active path whose row tops lie above the scrollport top
 * (viewport Y), in the same order as `pathFolderIds`.
 */
export function folderIdsWithRowAboveScrollportTop(
  scrollportTop: number,
  pathFolderIds: number[],
  getRowTop: (folderId: number) => number | null | undefined
): number[] {
  const out: number[] = []
  for (const id of pathFolderIds) {
    const top = getRowTop(id)
    if (top == null) continue
    if (top < scrollportTop) out.push(id)
  }
  return out
}
