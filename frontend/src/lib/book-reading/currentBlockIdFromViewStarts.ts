export type ViewBlockStarts<Block extends { id: number }> = {
  /** Distance in px from the top of the view down to the block's start (negative when above); null when unknown. */
  startTopPx: (block: Block) => number | null
  /** How far below the top a start may lie and still count as landed at the top (the view's end). */
  landingLimitPx: number
}

/**
 * A start this far below the top of the view still counts as at the top: a heading's top
 * margin can collapse through its wrapper, leaving the start about 20 px below the top when
 * the view shows the section from its beginning.
 */
const AT_TOP_TOLERANCE_PX = 24

/**
 * The current block is the last block in reading order (`blocks` in depth-first preorder)
 * whose start is at the top of the view or above it. The selected block wins when its start is
 * at that same place, or lies between it and the landing limit because the view cannot bring
 * it any higher.
 */
export function currentBlockIdFromViewStarts<Block extends { id: number }>(
  blocks: readonly Block[],
  view: ViewBlockStarts<Block>,
  selectedBlockId: number | null
): number | null {
  let current: { id: number; top: number } | null = null
  let selectedTop: number | null = null
  for (const block of blocks) {
    const top = view.startTopPx(block)
    if (top === null) {
      continue
    }
    if (block.id === selectedBlockId) {
      selectedTop = top
    }
    if (top <= AT_TOP_TOLERANCE_PX) {
      current = { id: block.id, top }
    }
  }
  if (
    selectedTop !== null &&
    selectedTop >= (current?.top ?? Number.NEGATIVE_INFINITY) &&
    selectedTop <= Math.max(AT_TOP_TOLERANCE_PX, view.landingLimitPx)
  ) {
    return selectedBlockId
  }
  return current?.id ?? null
}
