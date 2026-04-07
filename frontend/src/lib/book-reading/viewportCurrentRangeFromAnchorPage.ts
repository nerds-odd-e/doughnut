import type { BookAnchorFull } from "@generated/doughnut-backend-api"
import {
  ANCHOR_FORMAT_PDF_MINERU_OUTLINE_V1,
  parseMineruOutlineV1StartAnchor,
} from "./mineruOutlineV1PageIndex"
import type { ViewportYRange } from "./pdfViewerViewportTopYDown"

/**
 * Approximate MinerU-unit equivalent of `MINERU_SCROLL_TOP_PADDING_PDF` (40 PDF pts on a typical
 * page of ~800–850 pt). Used by the close-nodes transition: if the next section heading enters the
 * viewport within this distance of the top edge, it becomes current.
 */
const SCROLL_PADDING_MINERU = 50

/**
 * **Rule (in priority order):**
 * 1. Current = the **earliest** anchor on `anchorPageZeroBased` whose bbox is visible
 *    (`y0 < viewport.bottom AND y1 > viewport.top`).
 * 2. If that anchor's top (`y0`) has **not** passed the viewport midpoint (`y0 > viewport.mid`) →
 *    use the anchor **before** it (or `bestFromEarlierPages` if none on the page).
 * 3. If that anchor is **partially scrolled above** the viewport top (`y0 < viewport.top`) and the
 *    **next** anchor's top is within `SCROLL_PADDING_MINERU` of the viewport top → use the next
 *    anchor (close-nodes transition).
 * 4. **Gap case** (no visible anchor on the page) → last anchor on the page with `y0 ≤ viewport.mid`
 *    (most recent section heading scrolled past); falls back to `bestFromEarlierPages`.
 * 5. `viewport === null` → last anchor on the page (scroll position unknown).
 *
 * Anchors on earlier pages contribute `bestFromEarlierPages` only when the current page has **no**
 * outline rows (`onCurrentPage` is empty).
 *
 * `y0` / `y1` come from `bbox[1]` / `bbox[3]`; anchors without a bbox are treated as a point at
 * `y = 0` (never visible by the bbox check, but reachable as the "previous" fallback).
 */
export function viewportCurrentAnchorIdFromAnchorPage(
  orderedPreorderStartAnchors: readonly BookAnchorFull[],
  anchorPageZeroBased: number,
  viewport: ViewportYRange | null = null,
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
  const onCurrentPage: { id: number; y0: number; y1: number }[] = []
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
    const y0 = bbox !== null ? bbox[1] : 0
    const y1 = bbox !== null ? bbox[3] : y0
    if (pageIdx < anchorPageZeroBased) {
      bestFromEarlierPages = anchor.id
    } else if (pageIdx === anchorPageZeroBased) {
      onCurrentPage.push({ id: anchor.id, y0, y1 })
    }
  }
  if (onCurrentPage.length === 0) {
    return bestFromEarlierPages
  }
  if (viewport === null) {
    return onCurrentPage[onCurrentPage.length - 1]!.id
  }
  const { top, mid, bottom } = viewport

  const firstVisibleIdx = onCurrentPage.findIndex(
    (a) => a.y0 < bottom && a.y1 > top
  )

  if (firstVisibleIdx === -1) {
    let gapBest: number | null = null
    for (const a of onCurrentPage) {
      if (a.y0 <= mid) gapBest = a.id
    }
    return gapBest ?? bestFromEarlierPages
  }

  const first = onCurrentPage[firstVisibleIdx]!

  if (first.y0 > mid) {
    if (firstVisibleIdx > 0) return onCurrentPage[firstVisibleIdx - 1]!.id
    return bestFromEarlierPages
  }

  if (first.y0 < top) {
    const next = onCurrentPage[firstVisibleIdx + 1]
    if (next && next.y0 >= top && next.y0 - top <= SCROLL_PADDING_MINERU) {
      return next.id
    }
  }

  return first.id
}
