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
 *
 * **parseMineruOutlineV1StartAnchor:** For any string input, returns a navigation target
 * or `null`; it **never throws** (invalid JSON, wrong shape, missing `page_idx`, etc. → `null`).
 */
export const ANCHOR_FORMAT_PDF_MINERU_OUTLINE_V1 = "pdf.mineru_outline_v1"

export type MineruOutlineV1Bbox = readonly [number, number, number, number]

export type MineruOutlineV1NavigationTarget = {
  pageIndex: number
  bbox: MineruOutlineV1Bbox | null
}

/** pdf.js scrollPageIntoView: above bbox top by margin (horizontal center), zoom null. */
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

/**
 * Returns a navigation target or `null`. Never throws: malformed or non-conforming
 * `value` yields `null` so callers can no-op (e.g. outline click without breaking the viewer).
 */
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

/** MinerU y gap above bbox top so scroll target leaves the section title in view (pdf.js top bias). */
const MINERU_SCROLL_TARGET_Y_MARGIN = 100

/**
 * Map v1 bbox (top-left, y down) to pdf.js scrollPageIntoView XYZ at horizontal center and a point
 * **above** the box top in PDF user space (y up), so the visible band includes the heading. Zoom
 * is null so the viewer keeps its current scale (e.g. page-width after init).
 */
export function mineruOutlineV1BboxToXyzDestArray(
  pageHeightPdf: number,
  bbox: MineruOutlineV1Bbox
): PdfJsXyzDestArray {
  const [x0, y0, x1] = bbox
  const x = (x0 + x1) / 2
  const yTopMineru = Math.max(0, y0 - MINERU_SCROLL_TARGET_Y_MARGIN)
  const y = pageHeightPdf - yTopMineru
  return [null, { name: "XYZ" }, x, y, null]
}
