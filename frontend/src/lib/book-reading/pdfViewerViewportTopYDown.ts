type PageViewLike = {
  div: HTMLDivElement | undefined
  viewport?: { width: number; height: number }
}

/**
 * Viewport Y-range projected onto the anchor page, in MinerU-style coordinates
 * (0–1000, origin top-left, y increasing downward).
 */
export type ViewportYRange = {
  top: number
  mid: number
  bottom: number
}

/** Minimal pdf.js `PDFViewer` surface used here (avoids duplicate pdfjs-dist type trees in TS). */
export type PdfJsViewerForViewport = {
  pagesCount: number
  currentPageNumber: number
  pdfDocument: unknown
  getPageView: (index: number) => unknown
}

/** Content area of a rendered PDF page div, excluding CSS borders. */
function pageContentArea(
  pageView: PageViewLike
): { top: number; height: number } | null {
  const div = pageView.div
  if (!div) return null
  const rect = div.getBoundingClientRect()
  const top = rect.top + div.clientTop
  const height =
    pageView.viewport !== undefined && pageView.viewport.height > 1
      ? pageView.viewport.height
      : div.clientHeight
  if (height <= 1) return null
  return { top, height }
}

/** Screen Y → MinerU normalized Y (0–1000), clamped to the page. */
function mineruY(
  screenY: number,
  page: { top: number; height: number }
): number {
  return (
    (Math.max(0, Math.min(screenY - page.top, page.height)) / page.height) *
    1000
  )
}

/**
 * Page that contains the vertical midpoint of the scroll container (reading focus). Falls back to
 * the page with the largest visible height inside the container when the midpoint lies in a gap.
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
 * Projects the scroll container's visible Y-range onto the center page as a `ViewportYRange`
 * (0–1000 MinerU, top-down). Returns `null` viewport when the page has no meaningful height.
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
  const area = pageContentArea(pdfViewer.getPageView(pageIndex) as PageViewLike)
  if (!area) {
    return { anchorPageIndexZeroBased: pageIndex, viewport: null }
  }

  const containerRect = scrollContainer.getBoundingClientRect()
  return {
    anchorPageIndexZeroBased: pageIndex,
    viewport: {
      top: mineruY(containerRect.top, area),
      mid: mineruY((containerRect.top + containerRect.bottom) / 2, area),
      bottom: mineruY(containerRect.bottom, area),
    },
  }
}

/**
 * Page index + MinerU Y (0–1000) for the **top edge** of the scroll container.
 * The page index and normalizedTop always refer to the same physical page.
 */
export function pdfViewerReadingPositionTopEdge(
  scrollContainer: HTMLElement,
  pdfViewer: PdfJsViewerForViewport
): { pageIndexZeroBased: number; normalizedTop: number } | null {
  const pages = pdfViewer.pagesCount
  if (pages <= 0 || !pdfViewer.pdfDocument) return null

  const topY =
    scrollContainer.getBoundingClientRect().top + scrollContainer.clientTop

  let firstArea: { index: number; top: number; height: number } | null = null
  let lastArea: { index: number; top: number; height: number } | null = null

  for (let i = 0; i < pages; i++) {
    const area = pageContentArea(pdfViewer.getPageView(i) as PageViewLike)
    if (!area) continue
    if (!firstArea) firstArea = { index: i, ...area }
    lastArea = { index: i, ...area }

    if (topY >= area.top && topY < area.top + area.height) {
      return { pageIndexZeroBased: i, normalizedTop: mineruY(topY, area) }
    }
  }

  if (!firstArea || !lastArea) return null

  if (topY < firstArea.top) {
    return { pageIndexZeroBased: firstArea.index, normalizedTop: 0 }
  }
  return { pageIndexZeroBased: lastArea.index, normalizedTop: 1000 }
}
