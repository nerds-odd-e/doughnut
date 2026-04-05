import type { BookAnchorFull } from "@generated/doughnut-backend-api"
import {
  ANCHOR_FORMAT_PDF_MINERU_OUTLINE_V1,
  extractPageIndexZeroBased,
} from "./mineruOutlineV1PageIndex"

/**
 * **Rule:** last `BookAnchorFull` in preorder (caller supplies start anchors in outline order) whose
 * mineru `page_idx` ≤ `anchorPageZeroBased`. Anchors without a parseable `pdf.mineru_outline_v1`
 * value are skipped.
 */
export function viewportCurrentAnchorIdFromAnchorPage(
  orderedPreorderStartAnchors: readonly BookAnchorFull[],
  anchorPageZeroBased: number
): number | null {
  if (!Number.isInteger(anchorPageZeroBased) || anchorPageZeroBased < 0) {
    return null
  }
  let best: number | null = null
  for (const anchor of orderedPreorderStartAnchors) {
    if (anchor.anchorFormat !== ANCHOR_FORMAT_PDF_MINERU_OUTLINE_V1) {
      continue
    }
    const pageIdx = extractPageIndexZeroBased(anchor.value)
    if (pageIdx === null) continue
    if (pageIdx <= anchorPageZeroBased) {
      best = anchor.id
    }
  }
  return best
}
