import {
  normalizedBboxToPdfJsXyzDestArray,
  normalizedYToViewportY,
  type BookNavigationTarget,
} from "@/lib/book-reading/pdfOutlineV1Anchor"
import { locatorAsPdfNavigationTarget } from "@/composables/bookReaderViewerRef"
import type { ContentLocatorFull } from "@generated/doughnut-backend-api"
import type { PDFViewer } from "pdfjs-dist/web/pdf_viewer.mjs"

type PendingNavigation = {
  target: BookNavigationTarget
  highlightBboxes: ReadonlyArray<BookNavigationTarget>
}

export function usePdfNavigation(opts: {
  getPdfViewer: () => PDFViewer | null
  afterNavigate: () => void
  showHighlights: (targets: ReadonlyArray<BookNavigationTarget>) => void
}) {
  let pendingNavigation: PendingNavigation | null = null

  async function applyNavigationTarget(
    target: BookNavigationTarget,
    highlightBboxes: ReadonlyArray<BookNavigationTarget> = []
  ) {
    const pdfViewer = opts.getPdfViewer()
    if (!pdfViewer?.pdfDocument) return
    const { pageIndex, bbox } = target
    if (
      !Number.isInteger(pageIndex) ||
      pageIndex < 0 ||
      pageIndex >= pdfViewer.pagesCount
    ) {
      return
    }
    const pageNumber = pageIndex + 1
    if (bbox === null) {
      pdfViewer.scrollPageIntoView({ pageNumber })
    } else {
      const page = await pdfViewer.pdfDocument.getPage(pageNumber)
      const vp = page.getViewport({ scale: 1 })
      const destArray = normalizedBboxToPdfJsXyzDestArray(
        vp.width,
        vp.height,
        bbox
      )
      pdfViewer.scrollPageIntoView({ pageNumber, destArray: [...destArray] })
    }
    opts.showHighlights(highlightBboxes)
    queueMicrotask(() => opts.afterNavigate())
  }

  function flushPendingNavigation() {
    if (pendingNavigation === null || !opts.getPdfViewer()?.pdfDocument) {
      return
    }
    const shot = pendingNavigation
    pendingNavigation = null
    applyNavigationTarget(shot.target, shot.highlightBboxes).catch(() => {
      /* Outline jump failures from pdf.js must not reject pagesinit / viewer setup. */
    })
  }

  async function scrollToBookNavigationTarget(
    target: BookNavigationTarget,
    highlightBboxes: ReadonlyArray<BookNavigationTarget> = []
  ) {
    if (!Number.isInteger(target.pageIndex) || target.pageIndex < 0) {
      return
    }
    if (!opts.getPdfViewer()?.pdfDocument) {
      pendingNavigation = { target, highlightBboxes }
      return
    }
    await applyNavigationTarget(target, highlightBboxes)
  }

  async function scrollToStoredReadingPosition(
    pageIndexZeroBased: number,
    normalizedY: number
  ) {
    const pdfViewer = opts.getPdfViewer()
    if (!pdfViewer?.pdfDocument) return
    if (
      !Number.isInteger(pageIndexZeroBased) ||
      pageIndexZeroBased < 0 ||
      pageIndexZeroBased >= pdfViewer.pagesCount
    ) {
      return
    }
    const pageNumber = pageIndexZeroBased + 1
    const page = await pdfViewer.pdfDocument.getPage(pageNumber)
    const vp = page.getViewport({ scale: 1 })
    const vx = vp.width / 2
    const vy = normalizedYToViewportY(normalizedY, vp.height)
    const [pdfX, pdfY] = vp.convertToPdfPoint(vx, vy)
    pdfViewer.scrollPageIntoView({
      pageNumber,
      destArray: [null, { name: "XYZ" }, pdfX, pdfY, null],
    })
    queueMicrotask(() => opts.afterNavigate())
  }

  async function displayLocator(locator: ContentLocatorFull): Promise<void> {
    const target = locatorAsPdfNavigationTarget(locator)
    if (target === null) {
      return
    }
    await scrollToBookNavigationTarget(target)
  }

  function clearPendingNavigation() {
    pendingNavigation = null
  }

  return {
    scrollToBookNavigationTarget,
    scrollToStoredReadingPosition,
    displayLocator,
    flushPendingNavigation,
    clearPendingNavigation,
  }
}
