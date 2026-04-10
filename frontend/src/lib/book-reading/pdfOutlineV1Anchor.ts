/**
 * PDF outline v1 wire format (`pdf.mineru_outline_v1`): `BookAnchor.value` is JSON with `page_idx`
 * and optional `bbox`. Bbox coordinates come from the MinerU book-layout pipeline (`content_list`)
 * as `[x0,y0,x1,y1]` in 0–1000 normalized page space, top-left origin, y downward — not PDF user space.
 *
 * Consumers should use the exported functions; constants are internal.
 */
import type { BookAnchorFull } from "@generated/doughnut-backend-api"

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

/** Parse anchor value JSON. Never throws: malformed input yields `null`. */
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

/** Parse `BookAnchor.value` as PDF MinerU outline v1 JSON. Returns `null` for bad value. */
export function parsePdfOutlineV1Anchor(
  anchor: BookAnchorFull
): PdfOutlineV1NavigationTarget | null {
  return parsePdfOutlineV1StartAnchor(anchor.value)
}

/**
 * Convert `allBboxes` wire items (from `BookBlockFull.allBboxes`) to navigation targets.
 * The server pre-validates these; we still guard against malformed items defensively.
 */
export function wireItemsToNavigationTargets(
  items:
    | ReadonlyArray<{ pageIndex: number; bbox: ReadonlyArray<number> }>
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
    const bbox = parseOptionalBbox(it.bbox as unknown[])
    if (bbox === null) continue
    out.push({ pageIndex: it.pageIndex, bbox })
  }
  return out
}

export function extractPageIndexZeroBased(value: string): number | null {
  return parsePdfOutlineV1StartAnchor(value)?.pageIndex ?? null
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
