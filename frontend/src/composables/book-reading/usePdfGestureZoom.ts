import {
  clampScrollAxis,
  scrollAfterUniformContentScale,
  wheelDeltaYToScaleFactor,
} from "@/lib/book-reading/pdfBookViewerZoomAroundPoint"
import type { PdfViewerScrollSuppressionApi } from "@/composables/bookReaderViewerRef"
import type { PDFViewer } from "pdfjs-dist/web/pdf_viewer.mjs"
import type { Ref } from "vue"

const ZOOM_STEP = 1.25

function touchPairDistance(a: Touch, b: Touch) {
  return Math.hypot(a.clientX - b.clientX, a.clientY - b.clientY)
}

function clampOriginInSpan(offset: number, span: number) {
  if (span <= 0) return 0
  return Math.min(span, Math.max(0, offset))
}

export function usePdfGestureZoom(opts: {
  containerRef: Ref<HTMLDivElement | null>
  getPdfViewer: () => PDFViewer | null
  getScrollSuppression: () => PdfViewerScrollSuppressionApi
  onUserAdjusted: () => void
  afterScaleChange: () => void
  onClearCallout: () => void
}) {
  let onWheelForZoom: ((e: WheelEvent) => void) | null = null
  let onTouchStartForPinch: ((e: TouchEvent) => void) | null = null
  let onTouchMoveForPinch: ((e: TouchEvent) => void) | null = null
  let onTouchEndForPinch: ((e: TouchEvent) => void) | null = null
  let lastPinchDistance = 0

  function applyGestureScaleFactor(
    factor: number,
    clientX: number,
    clientY: number
  ) {
    const container = opts.containerRef.value
    const pdfViewer = opts.getPdfViewer()
    if (!container || !pdfViewer) return
    if (!(factor > 0) || !Number.isFinite(factor)) return

    opts.onUserAdjusted()
    const oldScale = pdfViewer.currentScale
    if (oldScale <= 0) return

    pdfViewer.currentScale = oldScale * factor
    const newScale = pdfViewer.currentScale
    const actualFactor = newScale / oldScale
    if (Math.abs(actualFactor - 1) < 1e-9) {
      opts.afterScaleChange()
      return
    }

    const rect = container.getBoundingClientRect()
    const ox = clampOriginInSpan(clientX - rect.left, rect.width)
    const oy = clampOriginInSpan(clientY - rect.top, rect.height)

    const next = scrollAfterUniformContentScale({
      scrollLeft: container.scrollLeft,
      scrollTop: container.scrollTop,
      originXInContainer: ox,
      originYInContainer: oy,
      scaleFactor: actualFactor,
    })

    requestAnimationFrame(() => {
      const c = opts.containerRef.value
      if (!c || !opts.getPdfViewer()) return
      c.scrollLeft = clampScrollAxis(
        next.scrollLeft,
        c.scrollWidth,
        c.clientWidth
      )
      c.scrollTop = clampScrollAxis(
        next.scrollTop,
        c.scrollHeight,
        c.clientHeight
      )
      opts.afterScaleChange()
    })
  }

  function detachGestureListeners(container: HTMLElement) {
    lastPinchDistance = 0
    if (onWheelForZoom) {
      container.removeEventListener("wheel", onWheelForZoom)
      onWheelForZoom = null
    }
    if (onTouchStartForPinch) {
      container.removeEventListener("touchstart", onTouchStartForPinch)
      onTouchStartForPinch = null
    }
    if (onTouchMoveForPinch) {
      container.removeEventListener("touchmove", onTouchMoveForPinch)
      onTouchMoveForPinch = null
    }
    if (onTouchEndForPinch) {
      container.removeEventListener("touchend", onTouchEndForPinch)
      container.removeEventListener("touchcancel", onTouchEndForPinch)
      onTouchEndForPinch = null
    }
  }

  function attachGestureListeners(container: HTMLElement) {
    onWheelForZoom = (e: WheelEvent) => {
      if (opts.getScrollSuppression().checkEvent()) {
        e.preventDefault()
        return
      }
      if (e.ctrlKey || e.metaKey) {
        if (!opts.getPdfViewer()) return
        e.preventDefault()
        const factor = wheelDeltaYToScaleFactor(e.deltaY)
        applyGestureScaleFactor(factor, e.clientX, e.clientY)
      } else {
        opts.onClearCallout()
      }
    }
    container.addEventListener("wheel", onWheelForZoom, { passive: false })

    onTouchStartForPinch = (e: TouchEvent) => {
      const a = e.touches[0]
      const b = e.touches[1]
      if (a && b) {
        lastPinchDistance = touchPairDistance(a, b)
      }
    }
    onTouchMoveForPinch = (e: TouchEvent) => {
      if (opts.getScrollSuppression().checkEvent()) {
        e.preventDefault()
        return
      }
      const a = e.touches[0]
      const b = e.touches[1]
      if (!b) {
        opts.onClearCallout()
        return
      }
      if (!a || !opts.getPdfViewer()) return
      e.preventDefault()
      const d = touchPairDistance(a, b)
      if (lastPinchDistance <= 0) {
        lastPinchDistance = d
        return
      }
      if (d <= 0) return
      const pinchFactor = d / lastPinchDistance
      lastPinchDistance = d
      const cx = (a.clientX + b.clientX) / 2
      const cy = (a.clientY + b.clientY) / 2
      applyGestureScaleFactor(pinchFactor, cx, cy)
    }
    onTouchEndForPinch = (e: TouchEvent) => {
      const a = e.touches[0]
      const b = e.touches[1]
      if (!a || !b) {
        lastPinchDistance = 0
      } else {
        lastPinchDistance = touchPairDistance(a, b)
      }
    }
    container.addEventListener("touchstart", onTouchStartForPinch, {
      passive: true,
    })
    container.addEventListener("touchmove", onTouchMoveForPinch, {
      passive: false,
    })
    container.addEventListener("touchend", onTouchEndForPinch, {
      passive: true,
    })
    container.addEventListener("touchcancel", onTouchEndForPinch, {
      passive: true,
    })
  }

  function zoomIn() {
    const pdfViewer = opts.getPdfViewer()
    if (!pdfViewer) return
    opts.onUserAdjusted()
    pdfViewer.currentScale *= ZOOM_STEP
    opts.afterScaleChange()
  }

  function zoomOut() {
    const pdfViewer = opts.getPdfViewer()
    if (!pdfViewer) return
    opts.onUserAdjusted()
    pdfViewer.currentScale /= ZOOM_STEP
    opts.afterScaleChange()
  }

  return { attachGestureListeners, detachGestureListeners, zoomIn, zoomOut }
}
