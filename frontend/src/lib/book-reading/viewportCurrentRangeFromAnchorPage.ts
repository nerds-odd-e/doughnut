import type { BookAnchorFull } from "@generated/doughnut-backend-api"
import {
  ANCHOR_FORMAT_PDF_MINERU_OUTLINE_V1,
  parseMineruOutlineV1StartAnchor,
} from "./mineruOutlineV1PageIndex"

/**
 * Small grace added to `viewportTopYDown` so a section registers as current as soon as its heading
 * is near the top of the viewport — accounting for the scroll top padding applied during outline
 * navigation (`MINERU_SCROLL_TOP_PADDING_PDF ≈ 40 PDF pts ≈ 50 MinerU units on a typical page`).
 */
const VIEWPORT_CURRENT_GRACE = 60

/**
 * **Rule:** Among anchors on `anchorPageZeroBased`, take the **last** in preorder with
 * `yStart <= viewportTopYDown + VIEWPORT_CURRENT_GRACE` (or all rows when `viewportTopYDown === null`,
 * then last wins). If **none** qualify (reading band is above every heading top on the page), use the
 * **first** anchor on that page so a newly scrolled-to page still highlights its opening section.
 *
 * Anchors on earlier pages contribute a fallback id only when the current PDF page has **no** outline
 * rows (empty `onCurrentPage`).
 *
 * `yStart` is the top edge of the optional mineru `bbox` (`y0`), or `0` if there is no bbox.
 * Skips anchors without a parseable `pdf.mineru_outline_v1` value. With `pdfPageCount`, skips
 * anchors whose `page_idx` is out of range; `anchorPageZeroBased` must be in range or the result is `null`.
 *
 * `viewportTopYDown` is the **top edge** of the visible page slice, normalized 0–1000 (origin top-left, y down).
 */
export function viewportCurrentAnchorIdFromAnchorPage(
  orderedPreorderStartAnchors: readonly BookAnchorFull[],
  anchorPageZeroBased: number,
  viewportTopYDown: number | null = null,
  pdfPageCount: number | null = null
): number | null {
  if (!Number.isInteger(anchorPageZeroBased) || anchorPageZeroBased < 0) {
    return null
  }
  if (pdfPageCount != null) {
    if (!Number.isInteger(pdfPageCount) || pdfPageCount <= 0) {
      return null
    }
    if (anchorPageZeroBased >= pdfPageCount) {
      return null
    }
  }
  let bestFromEarlierPages: number | null = null
  const onCurrentPage: { id: number; yStart: number }[] = []
  for (const anchor of orderedPreorderStartAnchors) {
    if (anchor.anchorFormat !== ANCHOR_FORMAT_PDF_MINERU_OUTLINE_V1) {
      continue
    }
    const parsed = parseMineruOutlineV1StartAnchor(anchor.value)
    if (parsed === null) continue
    const { pageIndex: pageIdx, bbox } = parsed
    if (pdfPageCount != null && pageIdx >= pdfPageCount) {
      continue
    }
    const yStart = bbox !== null ? bbox[1] : 0
    if (pageIdx < anchorPageZeroBased) {
      bestFromEarlierPages = anchor.id
    } else if (pageIdx === anchorPageZeroBased) {
      onCurrentPage.push({ id: anchor.id, yStart })
    }
  }
  if (onCurrentPage.length === 0) {
    return bestFromEarlierPages
  }
  const limit =
    viewportTopYDown === null
      ? Number.POSITIVE_INFINITY
      : viewportTopYDown + VIEWPORT_CURRENT_GRACE
  let bestOnPage: number | null = null
  for (const row of onCurrentPage) {
    if (row.yStart <= limit) {
      bestOnPage = row.id
    }
  }
  if (bestOnPage !== null) {
    return bestOnPage
  }
  return onCurrentPage[0]!.id
}
