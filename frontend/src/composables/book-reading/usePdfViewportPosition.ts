import type { ViewportYRange } from "@/lib/book-reading/pdfViewerViewportTopYDown"
import type { PdfLocatorFull } from "@generated/donut-backend-api"
import { computed, ref } from "vue"

export type PdfViewportPayload = {
  anchorPageIndexZeroBased: number
  viewport: ViewportYRange | null
  pagesCount: number
  readingPosition?: { pageIndexZeroBased: number; normalizedTop: number } | null
}

/** What the PDF viewer's latest viewport shows: page counter, reading position. */
export function usePdfViewportPosition() {
  const payload = ref<PdfViewportPayload | null>(null)

  const currentPage = computed(() => {
    const p = payload.value
    return p && p.pagesCount > 0 ? p.anchorPageIndexZeroBased + 1 : null
  })

  const pagesTotal = computed(() => {
    const p = payload.value
    return p && p.pagesCount > 0 ? p.pagesCount : null
  })

  function readingPositionLocator(): PdfLocatorFull | null {
    const p = payload.value
    if (!p) return null
    let reading: { pageIndexZeroBased: number; normalizedTop: number } | null =
      null
    if (p.readingPosition !== undefined) {
      reading = p.readingPosition
    } else if (p.viewport !== null) {
      reading = {
        pageIndexZeroBased: p.anchorPageIndexZeroBased,
        normalizedTop: p.viewport.top,
      }
    }
    if (reading === null) return null
    const y = Math.max(0, Math.min(1000, Math.round(reading.normalizedTop)))
    return {
      type: "PdfLocator_Full",
      pageIndex: reading.pageIndexZeroBased,
      bbox: [0, y, 0, y],
    }
  }

  return {
    payload,
    currentPage,
    pagesTotal,
    readingPositionLocator,
  }
}
