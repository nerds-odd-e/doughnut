/** Preorder neighbor after `blockId`, or null if none (e.g. last block or missing id). */
export function nextBookBlockAfter<T extends { id: number }>(
  blocks: readonly T[],
  blockId: number
): T | null {
  const idx = blocks.findIndex((b) => b.id === blockId)
  if (idx < 0 || idx >= blocks.length - 1) return null
  return blocks[idx + 1]!
}
