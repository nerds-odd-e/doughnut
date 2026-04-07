type PageViewLike = {
  div: HTMLDivElement | undefined
}

/** Minimal pdf.js `PDFViewer` surface used here (avoids duplicate pdfjs-dist type trees in TS). */
export type PdfJsViewerForViewport = {
  pagesCount: number
  currentPageNumber: number
  pdfDocument: unknown
  getPageView: (index: number) => unknown
}

/**
 * Page that contains the vertical midpoint of the scroll container (reading focus). Falls back to
 * the page with the largest visible height inside the container when the midpoint lies in a gap.
 *
 * Does not use `PDFViewer.currentPageNumber`: in a short viewport pdf.js can keep that on an
 * earlier page while it remains "fully visible", even when the user has scrolled a later page into
 * the reading band.
 */
export function pageIndexForScrollContainerCenter(
  scrollContainer: HTMLElement,
  pdfViewer: PdfJsViewerForViewport
): number {
  const pages = pdfViewer.pagesCount
  if (pages <= 0) {
    return 0
  }
  const containerRect = scrollContainer.getBoundingClientRect()
  const centerY = (containerRect.top + containerRect.bottom) / 2

  let bestOverlap = -Infinity
  let bestOverlapIndex = Math.max(
    0,
    Math.min(pages - 1, pdfViewer.currentPageNumber - 1)
  )

  for (let i = 0; i < pages; i++) {
    const pageView = pdfViewer.getPageView(i) as PageViewLike | undefined
    const div = pageView?.div
    if (!div) continue
    const pr = div.getBoundingClientRect()
    if (centerY >= pr.top && centerY <= pr.bottom) {
      return i
    }
    const overlap =
      Math.min(containerRect.bottom, pr.bottom) -
      Math.max(containerRect.top, pr.top)
    if (overlap > bestOverlap) {
      bestOverlap = overlap
      bestOverlapIndex = i
    }
  }
  return bestOverlapIndex
}

/**
 * Picks the pdf.js page via `pageIndexForScrollContainerCenter`, then computes a reading-reference Y
 * at **2/3 down the visible slice** (MinerU-style, 0–1000, top-down). The 2/3 point means a heading
 * becomes "viewport-current" once it is in the upper two-thirds of the visible area — stable across
 * viewport heights (midpoint or top-edge approaches are too sensitive to container size).
 *
 * Coordinates are derived directly from CSS layout (`getBoundingClientRect`), avoiding
 * PDF-internal coordinate roundtrips (`getPagePoint` / `getViewport`) that can drift with
 * CSS transforms or device pixel ratio.
 */
export function pdfViewerViewportTopYDown(
  scrollContainer: HTMLElement,
  pdfViewer: PdfJsViewerForViewport
): { anchorPageIndexZeroBased: number; viewportTopYDown: number | null } {
  const pages = pdfViewer.pagesCount

  if (pages === 0 || !pdfViewer.pdfDocument) {
    return { anchorPageIndexZeroBased: 0, viewportTopYDown: null }
  }
  const pageIndex = pageIndexForScrollContainerCenter(
    scrollContainer,
    pdfViewer
  )
  const pageView = pdfViewer.getPageView(pageIndex) as PageViewLike | undefined

  if (!pageView?.div) {
    return { anchorPageIndexZeroBased: pageIndex, viewportTopYDown: null }
  }

  const containerRect = scrollContainer.getBoundingClientRect()
  const pageRect = pageView.div.getBoundingClientRect()
  const visibleTop = Math.max(containerRect.top, pageRect.top)
  const visibleBottom = Math.min(containerRect.bottom, pageRect.bottom)
  if (visibleBottom <= visibleTop + 1) {
    return { anchorPageIndexZeroBased: pageIndex, viewportTopYDown: null }
  }

  const pageRenderedHeight = pageRect.bottom - pageRect.top
  if (pageRenderedHeight <= 0) {
    return { anchorPageIndexZeroBased: pageIndex, viewportTopYDown: null }
  }

  const readingLineY = visibleTop + (2 / 3) * (visibleBottom - visibleTop)
  const localY = Math.max(
    0,
    Math.min(readingLineY - pageRect.top, pageRenderedHeight)
  )

  return {
    anchorPageIndexZeroBased: pageIndex,
    viewportTopYDown: (localY / pageRenderedHeight) * 1000,
  }
}
