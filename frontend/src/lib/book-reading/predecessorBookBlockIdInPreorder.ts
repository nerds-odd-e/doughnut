/** Preorder neighbor before `canceledBlockId`, or null if none (e.g. first block or missing id). */
export function predecessorBookBlockIdInPreorder(
  blocks: { id: number }[],
  canceledBlockId: number
): number | null {
  const idx = blocks.findIndex((b) => b.id === canceledBlockId)
  if (idx <= 0) return null
  return blocks[idx - 1]!.id
}
