import type { BookAnchorFull } from "@generated/doughnut-backend-api"
import {
  ANCHOR_FORMAT_PDF_MINERU_OUTLINE_V1,
  extractPageIndexZeroBased,
} from "./mineruOutlineV1PageIndex"

/**
 * Preorder outline row with an optional mineru start anchor (same shape as `flatOutline` nodes).
 *
 * **Rule:** last node in preorder whose mineru `page_idx` ≤ `anchorPageZeroBased`. Nodes without a
 * parseable `pdf.mineru_outline_v1` start anchor are skipped.
 */
export type OutlineAnchorNode = {
  id: number
  startAnchor?: BookAnchorFull
}

export function viewportCurrentRangeIdFromAnchorPage(
  orderedPreorderNodes: readonly OutlineAnchorNode[],
  anchorPageZeroBased: number
): number | null {
  if (!Number.isInteger(anchorPageZeroBased) || anchorPageZeroBased < 0) {
    return null
  }
  let best: number | null = null
  for (const node of orderedPreorderNodes) {
    const anchor = node.startAnchor
    if (
      !anchor ||
      anchor.anchorFormat !== ANCHOR_FORMAT_PDF_MINERU_OUTLINE_V1 ||
      anchor.value == null
    ) {
      continue
    }
    const pageIdx = extractPageIndexZeroBased(anchor.value)
    if (pageIdx === null) continue
    if (pageIdx <= anchorPageZeroBased) {
      best = node.id
    }
  }
  return best
}
