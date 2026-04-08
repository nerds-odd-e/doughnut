/**
 * PDF outline v1 wire format (`pdf.mineru_outline_v1`): `BookAnchor.value` is JSON with `page_idx`
 * and optional `bbox`. Bbox coordinates come from the book-outline pipeline (MinerU content_list)
 * as `[x0,y0,x1,y1]` in 0–1000 normalized page space, top-left origin, y downward — not PDF user space.
 *
 * `parsePdfOutlineV1StartAnchor` never throws on bad input.
 */
export const PDF_OUTLINE_V1_ANCHOR_FORMAT = "pdf.mineru_outline_v1"

/** Max coordinate on the normalized page axis (bbox and viewport projection use the same scale). */
export const PDF_OUTLINE_V1_NORMALIZED_MAX = 1000

/** PDF user-space points subtracted above bbox top when scrolling to a heading (pdf.js XYZ). */
export const PDF_OUTLINE_V1_SCROLL_TOP_PADDING_PDF = 40

export type PdfOutlineV1Bbox = readonly [number, number, number, number]

export type PdfOutlineV1NavigationTarget = {
  pageIndex: number
  bbox: PdfOutlineV1Bbox | null
}

/** pdf.js scrollPageIntoView: near bbox top with small top padding (horizontal center of bbox), zoom null. */
export type PdfJsXyzDestArray = readonly [
  null,
  { readonly name: "XYZ" },
  number,
  number,
  null,
]

function parseOptionalBbox(raw: unknown): PdfOutlineV1Bbox | null {
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

export function parsePdfOutlineV1StartAnchor(
  value: string
): PdfOutlineV1NavigationTarget | null {
  try {
    const parsed: unknown = JSON.parse(value)
    if (!parsed || typeof parsed !== "object") return null
    const rec = parsed as Record<string, unknown>
    const pageIdx = rec.page_idx
    if (
      typeof pageIdx !== "number" ||
      !Number.isInteger(pageIdx) ||
      pageIdx < 0
    ) {
      return null
    }
    const bbox = rec.bbox === undefined ? null : parseOptionalBbox(rec.bbox)
    return { pageIndex: pageIdx, bbox }
  } catch {
    return null
  }
}

export function extractPageIndexZeroBased(value: string): number | null {
  return parsePdfOutlineV1StartAnchor(value)?.pageIndex ?? null
}

/**
 * Map v1 bbox (normalized page space, top-left origin, y down) to pdf.js scrollPageIntoView XYZ
 * so the viewport top sits a little above the bbox top. Zoom null keeps the current scale.
 */
export function outlineV1BboxToPdfJsXyzDestArray(
  pageWidthPdf: number,
  pageHeightPdf: number,
  bbox: PdfOutlineV1Bbox
): PdfJsXyzDestArray {
  const [x0, y0, x1] = bbox
  const x = ((x0 + x1) / 2 / PDF_OUTLINE_V1_NORMALIZED_MAX) * pageWidthPdf
  const yTopPdf = Math.max(
    0,
    (y0 / PDF_OUTLINE_V1_NORMALIZED_MAX) * pageHeightPdf -
      PDF_OUTLINE_V1_SCROLL_TOP_PADDING_PDF
  )
  const y = pageHeightPdf - yTopPdf
  return [null, { name: "XYZ" }, x, y, null]
}

/** Screen Y → normalized outline-v1 Y (0–PDF_OUTLINE_V1_NORMALIZED_MAX), clamped to the page band. */
export function screenYToOutlineV1NormalizedY(
  screenY: number,
  page: { top: number; height: number }
): number {
  return (
    (Math.max(0, Math.min(screenY - page.top, page.height)) / page.height) *
    PDF_OUTLINE_V1_NORMALIZED_MAX
  )
}
