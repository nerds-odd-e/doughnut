import { parseOptionalBbox } from "./pdfOutlineV1Anchor"
import type { ViewportYRange } from "./pdfViewerViewportTopYDown"

/**
 * Normalized distance for the close-nodes transition (tuned with scroll padding
 * and typical page height in mind).
 */
const SCROLL_PADDING_NORMALIZED = 50

/** First bbox per block may omit `bbox` (page-only); matches PDF anchor locator wire shape. */
export type BookBlockFirstBboxRow = {
  readonly id: number
  readonly firstBbox?: {
    readonly pageIndex: number
    readonly bbox?: ReadonlyArray<number>
  }
}

/**
 * Maps the anchor page + viewport Y-range (from `pdfViewerViewportTopYDown`) to the current block ID.
 * Called by `BookReadingContent.onViewportAnchorPage` on every scroll/resize.
 *
 * Key invariant: if the first visible block's `y0` is above the viewport midpoint, the previous
 * block is returned. Scrolling page N to the container top is not sufficient to make a block at
 * `y0 > 0` the current block in a short viewport — the scroll must reach at least `y0` pixels
 * into the page so `viewport.mid >= y0`.
 *
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
