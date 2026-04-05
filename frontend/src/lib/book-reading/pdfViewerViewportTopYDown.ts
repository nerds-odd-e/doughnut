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
  const pageNumber = pdfViewer.currentPageNumber
  const pageIndex = pageNumber - 1
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
  if (!Number.isFinite(mineruY)) {
    return { anchorPageIndexZeroBased: pageIndex, viewportTopYDown: null }
  }

  return {
    anchorPageIndexZeroBased: pageIndex,
    viewportTopYDown: Math.max(0, mineruY),
  }
}
