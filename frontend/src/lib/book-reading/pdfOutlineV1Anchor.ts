/**
 * MinerU / book-reading geometry: bboxes are `[x0,y0,x1,y1]` in 0–1000 normalized page space
 * (top-left origin, y downward), not PDF user space. `wireItemsToNavigationTargets` maps API
 * `allBboxes` (`PageBboxFull`) into scroll/highlight targets for pdf.js.
 */
const NORMALIZED_MAX = 1000
const SCROLL_TOP_PADDING_PDF = 40

export type PdfOutlineV1Bbox = readonly [number, number, number, number]

export type PdfOutlineV1NavigationTarget = {
  pageIndex: number
  bbox: PdfOutlineV1Bbox | null
}

/** pdf.js scrollPageIntoView XYZ dest array shape. */
export type PdfJsXyzDestArray = readonly [
  null,
  { readonly name: "XYZ" },
  number,
  number,
  null,
]

export function parseOptionalBbox(raw: unknown): PdfOutlineV1Bbox | null {
  if (!Array.isArray(raw) || raw.length !== 4) return null
  const a = raw[0]
  const b = raw[1]
  const c = raw[2]
  const d = raw[3]
  if (
    typeof a !== "number" ||
    typeof b !== "number" ||
    typeof c !== "number" ||
    typeof d !== "number"
  ) {
    return null
  }
  if (
    !Number.isFinite(a) ||
    !Number.isFinite(b) ||
    !Number.isFinite(c) ||
    !Number.isFinite(d)
  ) {
    return null
  }
  if (a >= c || b >= d) return null
  return [a, b, c, d]
}

/**
 * Convert `allBboxes` wire items (`PageBboxFull` from `BookBlockFull.allBboxes`) to navigation targets.
 * The server pre-validates these; we still guard against malformed items defensively.
 * Missing `bbox` yields a page-only target (`bbox: null`).
 */
export function wireItemsToNavigationTargets(
  items:
    | ReadonlyArray<{ pageIndex: number; bbox?: ReadonlyArray<number> }>
    | undefined
): PdfOutlineV1NavigationTarget[] {
  if (!items?.length) return []
  const out: PdfOutlineV1NavigationTarget[] = []
  for (const it of items) {
    if (
      typeof it.pageIndex !== "number" ||
      !Number.isInteger(it.pageIndex) ||
      it.pageIndex < 0
    ) {
      continue
    }
    const rawBbox = it.bbox as unknown
    if (rawBbox === undefined || rawBbox === null) {
      out.push({ pageIndex: it.pageIndex, bbox: null })
      continue
    }
    const bbox = parseOptionalBbox(it.bbox as unknown[])
    if (bbox === null) continue
    out.push({ pageIndex: it.pageIndex, bbox })
  }
  return out
}

/**
 * Map v1 bbox to pdf.js scrollPageIntoView XYZ so the viewport top sits a little above the
 * bbox top. Zoom null keeps the current scale.
 */
export function outlineV1BboxToPdfJsXyzDestArray(
  pageWidthPdf: number,
  pageHeightPdf: number,
  bbox: PdfOutlineV1Bbox
): PdfJsXyzDestArray {
  const [x0, y0, x1] = bbox
  const x = ((x0 + x1) / 2 / NORMALIZED_MAX) * pageWidthPdf
  const yTopPdf = Math.max(
    0,
    (y0 / NORMALIZED_MAX) * pageHeightPdf - SCROLL_TOP_PADDING_PDF
  )
  const y = pageHeightPdf - yTopPdf
  return [null, { name: "XYZ" }, x, y, null]
}

/** Convert normalized bbox to pixel rectangle in the given viewport dimensions. */
export function outlineV1BboxToPixelRect(
  bbox: PdfOutlineV1Bbox,
  viewportWidth: number,
  viewportHeight: number
): { left: number; top: number; width: number; height: number } {
  const [x0, y0, x1, y1] = bbox
  return {
    left: (x0 / NORMALIZED_MAX) * viewportWidth,
    top: (y0 / NORMALIZED_MAX) * viewportHeight,
    width: ((x1 - x0) / NORMALIZED_MAX) * viewportWidth,
    height: ((y1 - y0) / NORMALIZED_MAX) * viewportHeight,
  }
}

/** Convert normalized Y (0–1000) to viewport Y, clamped to page bounds. */
export function normalizedYToViewportY(
  normalizedY: number,
  pageHeight: number
): number {
  const clamped = Math.max(0, Math.min(normalizedY, NORMALIZED_MAX))
  return (clamped / NORMALIZED_MAX) * pageHeight
}

/** Screen Y → normalized Y (0–1000), clamped to the page band. */
export function screenYToNormalizedY(
  screenY: number,
  page: { top: number; height: number }
): number {
  return (
    (Math.max(0, Math.min(screenY - page.top, page.height)) / page.height) *
    NORMALIZED_MAX
  )
}
