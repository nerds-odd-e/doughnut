/**
 * pdf.mineru_outline_v1 — anchor.value is JSON from the CLI outline path.
 *
 * **page_idx:** 0-based page index (same as pdf.js pageNumber − 1).
 *
 * **bbox (optional):** MinerU-style `[x0, y0, x1, y1]` — origin top-left, y increasing
 * downward, in the same length units as `pdfPage.getViewport({ scale: 1 })` (PDF user
 * space width/height). Rotation other than 0 is not handled for bbox.
 *
 * Invalid or malformed bbox is treated as absent (page-top navigation only).
 */
export const ANCHOR_FORMAT_PDF_MINERU_OUTLINE_V1 = "pdf.mineru_outline_v1"

export type MineruOutlineV1Bbox = readonly [number, number, number, number]

export type MineruOutlineV1NavigationTarget = {
  pageIndex: number
  bbox: MineruOutlineV1Bbox | null
}

/** pdf.js scrollPageIntoView: center of bbox, zoom null keeps current scale (e.g. page-width). */
export type PdfJsXyzDestArray = readonly [
  null,
  { readonly name: "XYZ" },
  number,
  number,
  null,
]

function parseOptionalBbox(raw: unknown): MineruOutlineV1Bbox | null {
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

/** Invalid JSON, missing page_idx, or invalid page_idx → null (caller no-ops). */
export function parseMineruOutlineV1StartAnchor(
  value: string
): MineruOutlineV1NavigationTarget | null {
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
  return parseMineruOutlineV1StartAnchor(value)?.pageIndex ?? null
}

/**
 * Map v1 bbox (top-left, y down) to pdf.js scrollPageIntoView XYZ at the box center in PDF user
 * space (y up). Zoom is null so the viewer keeps its current scale (e.g. page-width after init).
 * (FitR fits the rectangle by changing scale, which can shrink the page to the viewport and leave
 * no scroll delta for same-page jumps; XYZ targets the region without rescaling.)
 */
export function mineruOutlineV1BboxToXyzDestArray(
  pageHeightPdf: number,
  bbox: MineruOutlineV1Bbox
): PdfJsXyzDestArray {
  const [x0, y0, x1, y1] = bbox
  const x = (x0 + x1) / 2
  const y = pageHeightPdf - (y0 + y1) / 2
  return [null, { name: "XYZ" }, x, y, null]
}
