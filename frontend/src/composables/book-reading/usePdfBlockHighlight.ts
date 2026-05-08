import { ref, type Ref } from "vue"
import { attachBookBlockSelectionBboxHighlight } from "@/lib/book-reading/bookBlockSelectionBboxHighlight"
import {
  normalizedBboxToPixelRect,
  type BookNavigationTarget,
  type NormalizedPageBbox,
} from "@/lib/book-reading/pdfOutlineV1Anchor"
import type { PDFViewer } from "pdfjs-dist/web/pdf_viewer.mjs"

export type HoldCallout = {
  contentBlockId: number
  clientX: number
  clientY: number
  derivedTitle: string | undefined
}

export function usePdfBlockHighlight(opts: {
  getPdfViewer: () => PDFViewer | null
  onCreateBlock: (payload: {
    contentBlockId: number
    derivedTitle: string | undefined
  }) => void
}) {
  const holdCallout = ref<HoldCallout | null>(null)
  let bookBlockSelectionBboxHighlightCancels: (() => void)[] = []
  let currentHighlightBboxes: ReadonlyArray<BookNavigationTarget> = []

  function clearBlockHighlight() {
    for (const c of bookBlockSelectionBboxHighlightCancels) {
      c()
    }
    bookBlockSelectionBboxHighlightCancels = []
    currentHighlightBboxes = []
    holdCallout.value = null
  }

  function appendBboxHighlight(
    pageNumber: number,
    bbox: NormalizedPageBbox,
    contentBlockId?: number,
    derivedTitle?: string
  ) {
    const pdfViewer = opts.getPdfViewer()
    if (!pdfViewer) return
    const pageView = pdfViewer.getPageView(pageNumber - 1)
    if (!pageView?.div) return
    const rect = normalizedBboxToPixelRect(
      bbox,
      pageView.viewport.width,
      pageView.viewport.height
    )
    bookBlockSelectionBboxHighlightCancels.push(
      attachBookBlockSelectionBboxHighlight(pageView.div, {
        ...rect,
        contentBlockId,
        derivedTitle,
      })
    )
  }

  function showSelectionBboxHighlights(
    highlightBboxes: ReadonlyArray<BookNavigationTarget>
  ) {
    clearBlockHighlight()
    currentHighlightBboxes = highlightBboxes
    const pdfViewer = opts.getPdfViewer()
    if (!pdfViewer) return
    for (const e of highlightBboxes) {
      if (e.bbox === null) continue
      if (
        !Number.isInteger(e.pageIndex) ||
        e.pageIndex < 0 ||
        e.pageIndex >= pdfViewer.pagesCount
      ) {
        continue
      }
      appendBboxHighlight(
        e.pageIndex + 1,
        e.bbox,
        e.contentBlockId,
        e.derivedTitle
      )
    }
  }

  function contentBlockAtClientPoint(
    clientX: number,
    clientY: number
  ): { contentBlockId: number; derivedTitle: string | undefined } | null {
    const pdfViewer = opts.getPdfViewer()
    if (!pdfViewer) return null
    for (const e of currentHighlightBboxes) {
      if (e.contentBlockId === undefined || e.bbox === null) continue
      if (
        !Number.isInteger(e.pageIndex) ||
        e.pageIndex < 0 ||
        e.pageIndex >= pdfViewer.pagesCount
      )
        continue
      const pageView = pdfViewer.getPageView(e.pageIndex)
      if (!pageView?.div) continue
      const pageRect = pageView.div.getBoundingClientRect()
      const left = pageRect.left + (e.bbox[0] / 1000) * pageRect.width
      const top = pageRect.top + (e.bbox[1] / 1000) * pageRect.height
      const right = pageRect.left + (e.bbox[2] / 1000) * pageRect.width
      const bottom = pageRect.top + (e.bbox[3] / 1000) * pageRect.height
      if (
        clientX >= left &&
        clientX <= right &&
        clientY >= top &&
        clientY <= bottom
      ) {
        return {
          contentBlockId: e.contentBlockId,
          derivedTitle: e.derivedTitle,
        }
      }
    }
    return null
  }

  function onContainerClick(e: MouseEvent) {
    const hit = contentBlockAtClientPoint(e.clientX, e.clientY)
    if (hit) {
      holdCallout.value = {
        contentBlockId: hit.contentBlockId,
        clientX: e.clientX,
        clientY: e.clientY,
        derivedTitle: hit.derivedTitle,
      }
    }
  }

  function onConfirmNewBlock() {
    const callout = holdCallout.value
    holdCallout.value = null
    if (callout) {
      opts.onCreateBlock({
        contentBlockId: callout.contentBlockId,
        derivedTitle: callout.derivedTitle,
      })
    }
  }

  function highlightBlockSelection(
    highlightBboxes: ReadonlyArray<BookNavigationTarget>
  ) {
    showSelectionBboxHighlights(highlightBboxes)
  }

  return {
    holdCallout: holdCallout as Ref<HoldCallout | null>,
    onContainerClick,
    onConfirmNewBlock,
    clearBlockHighlight,
    showSelectionBboxHighlights,
    highlightBlockSelection,
  }
}
