export const ANCHOR_FORMAT_PDF_MINERU_OUTLINE_V1 = "pdf.mineru_outline_v1"

/** Interim (until bad-anchor phase): invalid JSON, missing or invalid page_idx → null (caller no-ops). */
export function extractPageIndexZeroBased(value: string): number | null {
  try {
    const parsed: unknown = JSON.parse(value)
    if (!parsed || typeof parsed !== "object") return null
    const pageIdx = (parsed as Record<string, unknown>).page_idx
    if (
      typeof pageIdx !== "number" ||
      !Number.isInteger(pageIdx) ||
      pageIdx < 0
    ) {
      return null
    }
    return pageIdx
  } catch {
    return null
  }
}
