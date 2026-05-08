import type { ViewerLocatorRect } from "@/composables/bookReaderViewerRef"
import { locatorAsPdfNavigationTarget } from "@/composables/bookReaderViewerRef"
import type { ContentLocatorFull } from "@generated/doughnut-backend-api"
import type { PDFViewer } from "pdfjs-dist/web/pdf_viewer.mjs"
import type { Ref } from "vue"

const READING_PANEL_ANCHOR_GAP_PX = 8

export function usePdfLocatorGeometry(opts: {
  containerRef: Ref<HTMLDivElement | null>
  getPdfViewer: () => PDFViewer | null
}) {
  function resolveLocatorRect(
    locator: ContentLocatorFull
  ): ViewerLocatorRect | null {
    const container = opts.containerRef.value
    const pdfViewer = opts.getPdfViewer()
    if (!container || !pdfViewer) return null
    const target = locatorAsPdfNavigationTarget(locator)
    if (target === null || target.bbox === null) return null
    const { pageIndex, bbox } = target
    if (
      !Number.isInteger(pageIndex) ||
      pageIndex < 0 ||
      pageIndex >= pdfViewer.pagesCount
    )
      return null
    const pageView = pdfViewer.getPageView(pageIndex)
    if (!pageView?.div) return null
    const pageRect = pageView.div.getBoundingClientRect()
    const top = pageRect.top + (bbox[1] / 1000) * pageRect.height
    const bottom = pageRect.top + (bbox[3] / 1000) * pageRect.height
    const left = pageRect.left + (bbox[0] / 1000) * pageRect.width
    const right = pageRect.left + (bbox[2] / 1000) * pageRect.width
    return {
      top,
      bottom,
      left,
      right,
      width: Math.max(0, right - left),
      height: Math.max(0, bottom - top),
    }
  }

  function isLocatorBottomVisible(
    locator: ContentLocatorFull,
    obstructionPx: number
  ): boolean {
    const container = opts.containerRef.value
    if (!container || !opts.getPdfViewer()) return false
    const rect = resolveLocatorRect(locator)
    if (rect === null) return false
    const containerRect = container.getBoundingClientRect()
    const panelTop = containerRect.bottom - obstructionPx
    return rect.bottom < panelTop && rect.bottom > containerRect.top
  }

  function readingPanelAnchorTopPx(
    locator: ContentLocatorFull,
    obstructionPx: number
  ): number | null {
    if (!isLocatorBottomVisible(locator, obstructionPx)) {
      return null
    }
    const container = opts.containerRef.value
    if (!container || !opts.getPdfViewer()) return null
    const rect = resolveLocatorRect(locator)
    if (rect === null) return null
    const containerRect = container.getBoundingClientRect()
    return rect.bottom - containerRect.top + READING_PANEL_ANCHOR_GAP_PX
  }

  function getPageRect(pageIndex: number): { height: number } | null {
    const pdfViewer = opts.getPdfViewer()
    if (!pdfViewer) return null
    if (
      !Number.isInteger(pageIndex) ||
      pageIndex < 0 ||
      pageIndex >= pdfViewer.pagesCount
    )
      return null
    const pageView = pdfViewer.getPageView(pageIndex)
    if (!pageView?.div) return null
    return { height: pageView.div.getBoundingClientRect().height }
  }

  function getScrollViewportHeightPx(): number | null {
    const container = opts.containerRef.value
    if (!container) return null
    return container.getBoundingClientRect().height
  }

  function scrollPageNormalizedYToReadingClearance(
    pageIndex: number,
    normalizedY: number,
    obstructionPx: number
  ): void {
    const container = opts.containerRef.value
    const pdfViewer = opts.getPdfViewer()
    if (!container || !pdfViewer) return
    if (
      !Number.isInteger(pageIndex) ||
      pageIndex < 0 ||
      pageIndex >= pdfViewer.pagesCount
    )
      return
    const pageView = pdfViewer.getPageView(pageIndex)
    if (!(pageView as { div?: HTMLDivElement } | null)?.div) return
    const pageDiv = (pageView as { div: HTMLDivElement }).div
    const pageRect = pageDiv.getBoundingClientRect()
    const containerRect = container.getBoundingClientRect()
    const pointClientY = pageRect.top + (normalizedY / 1000) * pageRect.height
    const targetClient = containerRect.bottom - obstructionPx
    container.scrollTop += pointClientY - targetClient
  }

  function afterNextViewUpdate(fn: () => void): void {
    const pdfViewer = opts.getPdfViewer()
    if (!pdfViewer) {
      queueMicrotask(fn)
      return
    }
    pdfViewer.eventBus.on("updateviewarea", fn, { once: true })
  }

  return {
    resolveLocatorRect,
    isLocatorBottomVisible,
    readingPanelAnchorTopPx,
    getPageRect,
    getScrollViewportHeightPx,
    scrollPageNormalizedYToReadingClearance,
    afterNextViewUpdate,
  }
}
