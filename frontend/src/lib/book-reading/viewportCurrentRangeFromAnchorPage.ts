import type { BookAnchorFull } from "@generated/doughnut-backend-api"
import {
  ANCHOR_FORMAT_PDF_MINERU_OUTLINE_V1,
  parseMineruOutlineV1StartAnchor,
} from "./mineruOutlineV1PageIndex"

/**
 * **Rule:** last `BookAnchorFull` in preorder (caller supplies start anchors in outline order) such that:
 * - `page_idx < anchorPageZeroBased`, or
 * - `page_idx === anchorPageZeroBased` and (`viewportTopYDown === null` or `yStart ≤ viewportTopYDown`),
 * where `yStart` is the top edge of the optional mineru `bbox` (`y0`), or `0` if there is no bbox.
 *
 * Anchors without a parseable `pdf.mineru_outline_v1` value are skipped.
 *
 * `viewportTopYDown` uses the same coordinate system as mineru bbox (origin top-left of the page, y down),
 * units as `pdfPage.getViewport({ scale: 1 })`. The PDF viewer passes a **single reference y** on the page
 * (vertical center of the visible slice). When `null`, same-page ties behave like “all starts at the top”
 * (last preorder row on that page wins), matching page-only matching.
 */
export function viewportCurrentAnchorIdFromAnchorPage(
  orderedPreorderStartAnchors: readonly BookAnchorFull[],
  anchorPageZeroBased: number,
  viewportTopYDown: number | null = null
): number | null {
  if (!Number.isInteger(anchorPageZeroBased) || anchorPageZeroBased < 0) {
    return null
  }
  let best: number | null = null
  for (const anchor of orderedPreorderStartAnchors) {
    if (anchor.anchorFormat !== ANCHOR_FORMAT_PDF_MINERU_OUTLINE_V1) {
      continue
    }
    const parsed = parseMineruOutlineV1StartAnchor(anchor.value)
    if (parsed === null) continue
    const { pageIndex: pageIdx, bbox } = parsed
    const yStart = bbox !== null ? bbox[1] : 0
    if (pageIdx < anchorPageZeroBased) {
      best = anchor.id
    } else if (pageIdx === anchorPageZeroBased) {
      if (viewportTopYDown === null || yStart <= viewportTopYDown) {
        best = anchor.id
      }
    }
  }
  return best
}
