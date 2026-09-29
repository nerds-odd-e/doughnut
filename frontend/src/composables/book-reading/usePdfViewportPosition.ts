import { pdfLocatorsFromBlock } from "@/lib/book-reading/asPdfLocator"
import { currentBlockIdFromVisiblePage } from "@/lib/book-reading/currentBlockIdFromVisiblePage"
import type { ViewportYRange } from "@/lib/book-reading/pdfViewerViewportTopYDown"
import type {
  BookBlockFull,
  PdfLocatorFull,
} from "@generated/donut-backend-api"
import { computed, ref, toValue, type MaybeRefOrGetter } from "vue"

export type PdfViewportPayload = {
  anchorPageIndexZeroBased: number
  viewport: ViewportYRange | null
  pagesCount: number
  readingPosition?: { pageIndexZeroBased: number; normalizedTop: number } | null
}

/** What the PDF viewer's latest viewport shows: page counter, reading position, current-block candidate. */
export function usePdfViewportPosition(
  bookBlocks: MaybeRefOrGetter<readonly BookBlockFull[]>
) {
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

  /** Maps the anchor page and viewport Y-range to the block being read. */
  function currentBlockCandidate(p: PdfViewportPayload): number | null {
    return currentBlockIdFromVisiblePage(
      toValue(bookBlocks).map((r) => {
        const first = pdfLocatorsFromBlock(r)[0]
        return {
          id: r.id,
          firstBbox: first
            ? { pageIndex: first.pageIndex, bbox: first.bbox }
            : undefined,
        }
      }),
      p.anchorPageIndexZeroBased,
      p.viewport,
      p.pagesCount
    )
  }

  return {
    payload,
    currentPage,
    pagesTotal,
    readingPositionLocator,
    currentBlockCandidate,
  }
}
