import { parseOptionalBbox } from "./pdfOutlineV1Anchor"
import type { ViewportYRange } from "./pdfViewerViewportTopYDown"

/**
 * Normalized distance for the close-nodes transition (tuned with scroll padding
 * and typical page height in mind).
 */
const SCROLL_PADDING_NORMALIZED = 50

/** First bbox per block may omit `bbox` (page-only); stricter than generated OpenAPI `PageBboxFull`. */
export type BookBlockFirstBboxRow = {
  readonly id: number
  readonly firstBbox?: {
    readonly pageIndex: number
    readonly bbox?: ReadonlyArray<number>
  }
}

/**
 * **Rule (in priority order):**
 * 1. Current = the **earliest** block on `visiblePageIndexZeroBased` whose first-bbox region is visible
 *    (`y0 < viewport.bottom AND y1 > viewport.top`).
 * 2. If that block's top (`y0`) has **not** passed the viewport midpoint (`y0 > viewport.mid`) →
 *    use the block **before** it (or `bestFromEarlierPages` if none on the page).
 * 3. If that block is **partially scrolled above** the viewport top (`y0 < viewport.top`) and the
 *    **next** block's top is within `SCROLL_PADDING_NORMALIZED` of the viewport top → use the next
 *    block (close-nodes transition).
 * 4. **Gap case** (no visible block on the page) → last block on the page with `y0 ≤ viewport.mid`
 *    (most recent section heading scrolled past); falls back to `bestFromEarlierPages`.
 * 5. `viewport === null` → last block on the page (scroll position unknown).
 *
 * Blocks on earlier pages contribute `bestFromEarlierPages` only when the current page has **no**
 * participating rows (`onCurrentPage` is empty).
 *
 * `y0` / `y1` come from `bbox[1]` / `bbox[3]`; rows with a page index but no bbox (or omitted bbox)
 * are treated as a point at `y = 0` (never visible by the bbox check, but reachable as the "previous"
 * fallback). A bbox array that is **present** but fails validation skips the whole row.
 */
export function currentBlockIdFromVisiblePage(
  orderedPreorderRows: readonly BookBlockFirstBboxRow[],
  visiblePageIndexZeroBased: number,
  viewport: ViewportYRange | null = null,
  pdfPageCount: number | null = null
): number | null {
  if (
    !Number.isInteger(visiblePageIndexZeroBased) ||
    visiblePageIndexZeroBased < 0
  ) {
    return null
  }
  if (pdfPageCount != null) {
    if (!Number.isInteger(pdfPageCount) || pdfPageCount <= 0) {
      return null
    }
    if (visiblePageIndexZeroBased >= pdfPageCount) {
      return null
    }
  }
  let bestFromEarlierPages: number | null = null
  const onCurrentPage: { id: number; y0: number; y1: number }[] = []
  for (const row of orderedPreorderRows) {
    const fb = row.firstBbox
    if (fb === undefined) continue
    const pageIdx = fb.pageIndex
    if (
      typeof pageIdx !== "number" ||
      !Number.isInteger(pageIdx) ||
      pageIdx < 0
    ) {
      continue
    }
    if (pdfPageCount != null && pageIdx >= pdfPageCount) {
      continue
    }
    let y0: number
    let y1: number
    if (fb.bbox === undefined) {
      y0 = 0
      y1 = 0
    } else {
      const bbox = parseOptionalBbox(fb.bbox)
      if (bbox === null) continue
      y0 = bbox[1]
      y1 = bbox[3]
    }
    if (pageIdx < visiblePageIndexZeroBased) {
      bestFromEarlierPages = row.id
    } else if (pageIdx === visiblePageIndexZeroBased) {
      onCurrentPage.push({ id: row.id, y0, y1 })
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
    if (next && next.y0 >= top && next.y0 - top <= SCROLL_PADDING_NORMALIZED) {
      return next.id
    }
  }

  return first.id
}
