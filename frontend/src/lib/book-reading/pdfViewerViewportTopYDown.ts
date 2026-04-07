type PageViewLike = {
  div: HTMLDivElement | undefined
}

/**
 * Viewport Y-range projected onto the anchor page, in MinerU-style coordinates
 * (0–1000, origin top-left, y increasing downward).
 */
export type ViewportYRange = {
  /** Y at the top edge of the visible container (page may extend above this). */
  top: number
  /** Y at the vertical midpoint of the scroll container. */
  mid: number
  /** Y at the bottom edge of the visible container (page may extend below this). */
  bottom: number
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
 * Picks the pdf.js page via `pageIndexForScrollContainerCenter`, then projects the scroll
 * container's visible Y-range onto that page as a `ViewportYRange` (0–1000 MinerU, top-down):
 * - `top`: where the container top falls on the page (0 when page starts at or above container top)
 * - `mid`: the container midpoint projected onto the page
 * - `bottom`: where the container bottom falls on the page (1000 when page extends below container)
 *
 * Returns `null` for the viewport when the page is not meaningfully visible (height ≤ 1 px).
 */
export function pdfViewerViewportTopYDown(
  scrollContainer: HTMLElement,
  pdfViewer: PdfJsViewerForViewport
): { anchorPageIndexZeroBased: number; viewport: ViewportYRange | null } {
  const pages = pdfViewer.pagesCount

  if (pages === 0 || !pdfViewer.pdfDocument) {
    return { anchorPageIndexZeroBased: 0, viewport: null }
  }
  const pageIndex = pageIndexForScrollContainerCenter(
    scrollContainer,
    pdfViewer
  )
  const pageView = pdfViewer.getPageView(pageIndex) as PageViewLike | undefined

  if (!pageView?.div) {
    return { anchorPageIndexZeroBased: pageIndex, viewport: null }
  }

  const containerRect = scrollContainer.getBoundingClientRect()
  const pageRect = pageView.div.getBoundingClientRect()
  const visibleTop = Math.max(containerRect.top, pageRect.top)
  const visibleBottom = Math.min(containerRect.bottom, pageRect.bottom)
  if (visibleBottom <= visibleTop + 1) {
    return { anchorPageIndexZeroBased: pageIndex, viewport: null }
  }

  const pageRenderedHeight = pageRect.bottom - pageRect.top
  if (pageRenderedHeight <= 0) {
    return { anchorPageIndexZeroBased: pageIndex, viewport: null }
  }

  const toMineruY = (cssY: number) =>
    (Math.max(0, Math.min(cssY - pageRect.top, pageRenderedHeight)) /
      pageRenderedHeight) *
    1000

  return {
    anchorPageIndexZeroBased: pageIndex,
    viewport: {
      top: toMineruY(containerRect.top),
      mid: toMineruY((containerRect.top + containerRect.bottom) / 2),
      bottom: toMineruY(containerRect.bottom),
    },
  }
}
