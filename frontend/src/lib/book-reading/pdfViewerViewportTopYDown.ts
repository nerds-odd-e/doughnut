import type { PDFViewer } from "pdfjs-dist/web/pdf_viewer.mjs"

type PageViewLike = {
  div: HTMLDivElement | undefined
  pdfPage: {
    getViewport: (opts: { scale: number; rotation: number }) => {
      height: number
    }
  } | null
  rotation: number
  pdfPageRotate: number
  width: number
  height: number
  getPagePoint: (x: number, y: number) => number[]
}

/**
 * Page that contains the vertical midpoint of the scroll container (reading focus). Falls back to
 * the page with the largest visible height inside the container when the midpoint lies in a gap.
 *
 * Does not use `PDFViewer.currentPageNumber`: in a short viewport pdf.js can keep that on an
 * earlier page while it remains “fully visible”, even when the user has scrolled a later page into
 * the reading band.
 */
export function pageIndexForScrollContainerCenter(
  scrollContainer: HTMLElement,
  pdfViewer: PDFViewer
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
 * Vertical **center** of the scroll container’s visible band intersected with the current pdf.js
 * page, expressed as mineru-style y from the page top (down), same units as
 * `pdfPage.getViewport({ scale: 1 })`. Using the midpoint matches “where I’m reading” better than
 * the top edge alone (which stays at ~0 when a lower bbox is centered on screen).
 */
export function pdfViewerViewportTopYDown(
  scrollContainer: HTMLElement,
  pdfViewer: PDFViewer
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

  if (!pageView?.pdfPage || !pageView.div) {
    return { anchorPageIndexZeroBased: pageIndex, viewportTopYDown: null }
  }

  const containerRect = scrollContainer.getBoundingClientRect()
  const pageRect = pageView.div.getBoundingClientRect()
  const visibleTop = Math.max(containerRect.top, pageRect.top)
  const visibleBottom = Math.min(containerRect.bottom, pageRect.bottom)
  if (visibleBottom <= visibleTop + 1) {
    return { anchorPageIndexZeroBased: pageIndex, viewportTopYDown: null }
  }

  const visibleMidY = (visibleTop + visibleBottom) / 2
  let localY = visibleMidY - pageRect.top
  localY = Math.max(0, Math.min(localY, pageView.height - 1e-6))

  const localX = pageView.width / 2
  const pdfPoint = pageView.getPagePoint(localX, localY)
  const pdfY = pdfPoint[1]
  if (typeof pdfY !== "number" || !Number.isFinite(pdfY)) {
    return { anchorPageIndexZeroBased: pageIndex, viewportTopYDown: null }
  }

  const totalRotation = (pageView.rotation + pageView.pdfPageRotate) % 360
  const vp1 = pageView.pdfPage.getViewport({
    scale: 1,
    rotation: totalRotation,
  })
  const pageHeightPdf = vp1.height
  const mineruY = pageHeightPdf - pdfY
  if (!Number.isFinite(mineruY) || pageHeightPdf <= 0) {
    return { anchorPageIndexZeroBased: pageIndex, viewportTopYDown: null }
  }

  return {
    anchorPageIndexZeroBased: pageIndex,
    viewportTopYDown: Math.max(0, (mineruY / pageHeightPdf) * 1000),
  }
}
