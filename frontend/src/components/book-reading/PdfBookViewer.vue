<template>
  <div
    ref="containerRef"
    data-testid="pdf-book-viewer"
    class="pdf-book-viewer-container"
  >
    <div ref="viewerRef" class="pdfViewer" />
  </div>
</template>

<script setup lang="ts">
import "@/lib/pdfjsWorker"
import {
  attachPdfBookViewerGeometryResampleListeners,
  createCoalescedRequestAnimationFrameEmitter,
} from "@/lib/book-reading/pdfBookViewerGeometryResample"
import { pdfScaleAfterPageWidth } from "@/lib/book-reading/pdfDefaultScale"
import {
  clampScrollAxis,
  scrollAfterUniformContentScale,
  wheelDeltaYToScaleFactor,
} from "@/lib/book-reading/pdfBookViewerZoomAroundPoint"
import {
  pdfViewerReadingPositionTopEdge,
  pdfViewerViewportTopYDown,
  type ViewportYRange,
} from "@/lib/book-reading/pdfViewerViewportTopYDown"
import { attachBookBlockSelectionBboxHighlight } from "@/lib/book-reading/bookBlockSelectionBboxHighlight"
import {
  normalizedYToViewportY,
  outlineV1BboxToPdfJsXyzDestArray,
  outlineV1BboxToPixelRect,
  type PdfOutlineV1Bbox,
  type PdfOutlineV1NavigationTarget,
} from "@/lib/book-reading/pdfOutlineV1Anchor"
import {
  getDocument,
  type PDFDocumentProxy,
} from "pdfjs-dist/legacy/build/pdf.mjs"
import {
  EventBus,
  PDFLinkService,
  PDFViewer,
} from "pdfjs-dist/legacy/web/pdf_viewer.mjs"
import "pdfjs-dist/legacy/web/pdf_viewer.css"
import { nextTick, onBeforeUnmount, ref, watch } from "vue"

const props = defineProps<{
  pdfBytes: ArrayBuffer | Uint8Array | null
}>()

const emit = defineEmits<{
  loadError: [message: string]
  viewportAnchorPage: [
    {
      anchorPageIndexZeroBased: number
      viewport: ViewportYRange | null
      pagesCount: number
      readingPosition: {
        pageIndexZeroBased: number
        normalizedTop: number
      } | null
    },
  ]
  pagesReady: []
}>()

const containerRef = ref<HTMLDivElement | null>(null)
const viewerRef = ref<HTMLDivElement | null>(null)

let pdfViewer: PDFViewer | null = null
let currentLoadingTask: ReturnType<typeof getDocument> | null = null
let onPageChangingForViewport: (() => void) | null = null
let lastEmittedPage: number | null = null
let lastEmittedYQuantized: number | null = null
let onScrollForViewport: (() => void) | null = null
let geometryRafEmitter: ReturnType<
  typeof createCoalescedRequestAnimationFrameEmitter
> | null = null
let viewportOnlyRafEmitter: ReturnType<
  typeof createCoalescedRequestAnimationFrameEmitter
> | null = null
let detachGeometryResampleListeners: (() => void) | null = null
let intrinsicFirstPageWidth = 0
let userAdjustedScale = false

let onWheelForZoom: ((e: WheelEvent) => void) | null = null
let onTouchStartForPinch: ((e: TouchEvent) => void) | null = null
let onTouchMoveForPinch: ((e: TouchEvent) => void) | null = null
let scrollSuppressed = false
let scrollSuppressTimer: ReturnType<typeof setTimeout> | null = null
let onTouchEndForPinch: ((e: TouchEvent) => void) | null = null
let lastPinchDistance = 0

const SCALE_EPSILON = 0.001

function teardownGeometryResample() {
  geometryRafEmitter?.cancel()
  geometryRafEmitter = null
  viewportOnlyRafEmitter?.cancel()
  viewportOnlyRafEmitter = null
  detachGeometryResampleListeners?.()
  detachGeometryResampleListeners = null
}

function detachViewportScrollListener(container: HTMLElement) {
  if (onScrollForViewport) {
    container.removeEventListener("scroll", onScrollForViewport)
    onScrollForViewport = null
  }
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

function touchPairDistance(a: Touch, b: Touch) {
  return Math.hypot(a.clientX - b.clientX, a.clientY - b.clientY)
}

function clampOriginInSpan(offset: number, span: number) {
  if (span <= 0) return 0
  return Math.min(span, Math.max(0, offset))
}

function applyGestureScaleFactor(
  factor: number,
  clientX: number,
  clientY: number
) {
  const container = containerRef.value
  if (!container || !pdfViewer) return
  if (!(factor > 0) || !Number.isFinite(factor)) return

  userAdjustedScale = true
  const oldScale = pdfViewer.currentScale
  if (oldScale <= 0) return

  pdfViewer.currentScale = oldScale * factor
  const newScale = pdfViewer.currentScale
  const actualFactor = newScale / oldScale
  if (Math.abs(actualFactor - 1) < 1e-9) {
    emitViewportDescriptorIfChanged()
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
    const c = containerRef.value
    if (!c || !pdfViewer) return
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
    emitViewportDescriptorIfChanged()
  })
}

function emitViewportDescriptorIfChanged() {
  const container = containerRef.value
  if (!container || !pdfViewer) return
  const sample = pdfViewerViewportTopYDown(container, pdfViewer)
  const midQ =
    sample.viewport === null
      ? null
      : Math.round(sample.viewport.mid * 100) / 100
  if (
    lastEmittedPage === sample.anchorPageIndexZeroBased &&
    lastEmittedYQuantized === midQ
  ) {
    return
  }
  lastEmittedPage = sample.anchorPageIndexZeroBased
  lastEmittedYQuantized = midQ
  const readingPosition = pdfViewerReadingPositionTopEdge(container, pdfViewer)
  emit("viewportAnchorPage", {
    anchorPageIndexZeroBased: sample.anchorPageIndexZeroBased,
    viewport: sample.viewport,
    pagesCount: pdfViewer.pagesCount,
    readingPosition,
  })
}

function applyResponsiveDefaultScale(options?: { force?: boolean }) {
  const force = options?.force === true
  const container = containerRef.value
  if (!container || !pdfViewer || intrinsicFirstPageWidth <= 0) return
  if (!force && userAdjustedScale) return
  if (container.clientWidth <= 0) return

  pdfViewer.currentScaleValue = "page-width"
  const nextScale = pdfScaleAfterPageWidth(
    pdfViewer.currentScale,
    intrinsicFirstPageWidth
  )
  if (Math.abs(pdfViewer.currentScale - nextScale) < SCALE_EPSILON) {
    return
  }
  pdfViewer.currentScale = nextScale
}

type PendingNavigation = {
  target: PdfOutlineV1NavigationTarget
  highlightBboxes: ReadonlyArray<PdfOutlineV1NavigationTarget>
}

let pendingNavigation: PendingNavigation | null = null
let bookBlockSelectionBboxHighlightCancels: (() => void)[] = []

function clearBookBlockSelectionBboxHighlight() {
  for (const c of bookBlockSelectionBboxHighlightCancels) {
    c()
  }
  bookBlockSelectionBboxHighlightCancels = []
}

function appendBookBlockSelectionBboxHighlight(
  pageNumber: number,
  bbox: PdfOutlineV1Bbox
) {
  if (!pdfViewer) return
  const pageView = pdfViewer.getPageView(pageNumber - 1)
  if (!pageView?.div) return
  const rect = outlineV1BboxToPixelRect(
    bbox,
    pageView.viewport.width,
    pageView.viewport.height
  )
  bookBlockSelectionBboxHighlightCancels.push(
    attachBookBlockSelectionBboxHighlight(pageView.div, rect)
  )
}

function showSelectionBboxHighlights(
  highlightBboxes: ReadonlyArray<PdfOutlineV1NavigationTarget>
) {
  clearBookBlockSelectionBboxHighlight()
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
    appendBookBlockSelectionBboxHighlight(e.pageIndex + 1, e.bbox)
  }
}

async function applyPdfOutlineV1Target(
  target: PdfOutlineV1NavigationTarget,
  highlightBboxes: ReadonlyArray<PdfOutlineV1NavigationTarget> = []
) {
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
    const destArray = outlineV1BboxToPdfJsXyzDestArray(
      vp.width,
      vp.height,
      bbox
    )
    pdfViewer.scrollPageIntoView({ pageNumber, destArray: [...destArray] })
  }
  showSelectionBboxHighlights(highlightBboxes)
  queueMicrotask(() => emitViewportDescriptorIfChanged())
}

function flushPendingNavigation() {
  if (pendingNavigation === null || !pdfViewer?.pdfDocument) {
    return
  }
  const shot = pendingNavigation
  pendingNavigation = null
  applyPdfOutlineV1Target(shot.target, shot.highlightBboxes).catch(() => {
    /* Outline jump failures from pdf.js must not reject pagesinit / viewer setup. */
  })
}

async function scrollToPdfOutlineV1Target(
  target: PdfOutlineV1NavigationTarget,
  highlightBboxes: ReadonlyArray<PdfOutlineV1NavigationTarget> = []
) {
  if (!Number.isInteger(target.pageIndex) || target.pageIndex < 0) {
    return
  }
  if (!pdfViewer?.pdfDocument) {
    pendingNavigation = { target, highlightBboxes }
    return
  }
  await applyPdfOutlineV1Target(target, highlightBboxes)
}

async function scrollToStoredReadingPosition(
  pageIndexZeroBased: number,
  normalizedY: number
) {
  if (!pdfViewer?.pdfDocument) return
  if (
    !Number.isInteger(pageIndexZeroBased) ||
    pageIndexZeroBased < 0 ||
    pageIndexZeroBased >= pdfViewer.pagesCount
  ) {
    return
  }
  const pageView = pdfViewer.getPageView(pageIndexZeroBased) as {
    viewport?: {
      width: number
      height: number
      convertToPdfPoint: (x: number, y: number) => number[]
    }
  } | null
  if (!pageView?.viewport) return
  const vp = pageView.viewport
  const vx = vp.width / 2
  const vy = normalizedYToViewportY(normalizedY, vp.height)
  const [pdfX, pdfY] = vp.convertToPdfPoint(vx, vy)
  pdfViewer.scrollPageIntoView({
    pageNumber: pageIndexZeroBased + 1,
    destArray: [null, { name: "XYZ" }, pdfX, pdfY, null],
  })
  queueMicrotask(() => emitViewportDescriptorIfChanged())
}

const ZOOM_STEP = 1.25

function zoomIn() {
  if (!pdfViewer) return
  userAdjustedScale = true
  pdfViewer.currentScale *= ZOOM_STEP
  emitViewportDescriptorIfChanged()
}

function zoomOut() {
  if (!pdfViewer) return
  userAdjustedScale = true
  pdfViewer.currentScale /= ZOOM_STEP
  emitViewportDescriptorIfChanged()
}

function highlightBlockSelection(
  highlightBboxes: ReadonlyArray<PdfOutlineV1NavigationTarget>
) {
  showSelectionBboxHighlights(highlightBboxes)
}

/**
 * Returns true when the bottom edge of the given bbox (0–1000 MinerU normalized, page-local) is
 * both visible in the scroll container's viewport and above `obstructionPx` from the container's
 * bottom edge (so the Reading Control Panel does not obscure it).
 */
function isLastContentBottomVisible(
  target: PdfOutlineV1NavigationTarget,
  obstructionPx: number
): boolean {
  const container = containerRef.value
  if (!container || !pdfViewer) return false
  const { pageIndex, bbox } = target
  if (
    bbox === null ||
    !Number.isInteger(pageIndex) ||
    pageIndex < 0 ||
    pageIndex >= pdfViewer.pagesCount
  ) {
    return false
  }
  const pageView = pdfViewer.getPageView(pageIndex)
  if (!pageView?.div) return false
  const pageRect = pageView.div.getBoundingClientRect()
  const containerRect = container.getBoundingClientRect()
  const bboxBottomClient = pageRect.top + (bbox[3] / 1000) * pageRect.height
  const panelTop = containerRect.bottom - obstructionPx
  return bboxBottomClient < panelTop && bboxBottomClient > containerRect.top
}

/** Suppresses wheel/touch-move scroll input for `holdMs` milliseconds. */
function suppressScrollInput(holdMs: number): void {
  if (scrollSuppressTimer !== null) {
    clearTimeout(scrollSuppressTimer)
  }
  scrollSuppressed = true
  scrollSuppressTimer = setTimeout(() => {
    scrollSuppressed = false
    scrollSuppressTimer = null
  }, holdMs)
}

/**
 * Scrolls so the bottom of the given bbox sits just above `obstructionPx` from the container
 * bottom, then suppresses wheel/touch-move scroll input for `holdMs` milliseconds so trailing
 * gesture events cannot immediately undo the snap.
 */
function snapToContentBottomAndHold(
  pageIndex: number,
  normalizedBboxBottom: number,
  obstructionPx: number,
  holdMs: number
): void {
  const container = containerRef.value
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
  const bboxBottomClient =
    pageRect.top + (normalizedBboxBottom / 1000) * pageRect.height
  const targetClient = containerRect.bottom - obstructionPx
  container.scrollTop += bboxBottomClient - targetClient
  suppressScrollInput(holdMs)
}

defineExpose({
  scrollToPdfOutlineV1Target,
  highlightBlockSelection,
  scrollToStoredReadingPosition,
  snapToContentBottomAndHold,
  suppressScrollInput,
  zoomIn,
  zoomOut,
  isLastContentBottomVisible,
})

async function loadPdf(bytes: ArrayBuffer | Uint8Array) {
  const container = containerRef.value
  const viewer = viewerRef.value
  if (!container || !viewer) return

  if (currentLoadingTask) {
    await currentLoadingTask.destroy()
    currentLoadingTask = null
  }
  if (pdfViewer) {
    clearBookBlockSelectionBboxHighlight()
    if (onPageChangingForViewport) {
      pdfViewer.eventBus.off("pagechanging", onPageChangingForViewport)
      onPageChangingForViewport = null
    }
    pdfViewer.setDocument(null as unknown as PDFDocumentProxy)
    pdfViewer = null
    pendingNavigation = null
  }

  detachViewportScrollListener(container)
  detachGestureListeners(container)
  teardownGeometryResample()
  lastEmittedPage = null
  lastEmittedYQuantized = null
  intrinsicFirstPageWidth = 0
  userAdjustedScale = false

  const eventBus = new EventBus()
  const linkService = new PDFLinkService({ eventBus })

  pdfViewer = new PDFViewer({
    container,
    viewer,
    eventBus,
    linkService,
    removePageBorders: false,
  })
  linkService.setViewer(pdfViewer)

  onPageChangingForViewport = () => {
    emitViewportDescriptorIfChanged()
  }
  eventBus.on("pagechanging", onPageChangingForViewport)

  onScrollForViewport = () => {
    emitViewportDescriptorIfChanged()
  }
  container.addEventListener("scroll", onScrollForViewport, { passive: true })

  onWheelForZoom = (e: WheelEvent) => {
    if (scrollSuppressed) {
      e.preventDefault()
      return
    }
    if (!(e.ctrlKey || e.metaKey)) return
    if (!pdfViewer) return
    e.preventDefault()
    const factor = wheelDeltaYToScaleFactor(e.deltaY)
    applyGestureScaleFactor(factor, e.clientX, e.clientY)
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
    if (scrollSuppressed) {
      e.preventDefault()
      return
    }
    const a = e.touches[0]
    const b = e.touches[1]
    if (!a || !b || !pdfViewer) return
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

  geometryRafEmitter = createCoalescedRequestAnimationFrameEmitter({
    emit: () => {
      applyResponsiveDefaultScale()
      emitViewportDescriptorIfChanged()
    },
  })
  viewportOnlyRafEmitter = createCoalescedRequestAnimationFrameEmitter({
    emit: () => emitViewportDescriptorIfChanged(),
  })
  detachGeometryResampleListeners =
    attachPdfBookViewerGeometryResampleListeners({
      container,
      eventBus,
      scheduleEmit: () => geometryRafEmitter!.schedule(),
      scheduleEmitOnScaleChange: () => viewportOnlyRafEmitter!.schedule(),
    })

  eventBus.on("pagesinit", () => {
    if (pdfViewer) {
      applyResponsiveDefaultScale({ force: true })
      flushPendingNavigation()
      emitViewportDescriptorIfChanged()
      emit("pagesReady")
    }
  })

  const data =
    bytes instanceof Uint8Array ? new Uint8Array(bytes) : new Uint8Array(bytes)
  const loadingTask = getDocument({ data })
  currentLoadingTask = loadingTask

  try {
    const pdf = await loadingTask.promise
    if (currentLoadingTask !== loadingTask) return
    const firstPage = await pdf.getPage(1)
    if (currentLoadingTask !== loadingTask) return
    intrinsicFirstPageWidth = firstPage.getViewport({ scale: 1 }).width
    userAdjustedScale = false
    pdfViewer.setDocument(pdf)
    linkService.setDocument(pdf)
  } catch {
    if (currentLoadingTask === loadingTask) {
      emit("loadError", "This file is not a valid PDF.")
    }
  }
}

watch(
  () => props.pdfBytes,
  async (bytes) => {
    await nextTick()
    if (bytes?.byteLength) {
      await loadPdf(bytes)
    }
  },
  { immediate: true }
)

onBeforeUnmount(async () => {
  if (scrollSuppressTimer !== null) {
    clearTimeout(scrollSuppressTimer)
    scrollSuppressTimer = null
  }
  scrollSuppressed = false
  const container = containerRef.value
  if (container) {
    detachViewportScrollListener(container)
    detachGestureListeners(container)
    teardownGeometryResample()
  }
  if (currentLoadingTask) {
    await currentLoadingTask.destroy()
    currentLoadingTask = null
  }
  if (pdfViewer) {
    if (onPageChangingForViewport) {
      pdfViewer.eventBus.off("pagechanging", onPageChangingForViewport)
      onPageChangingForViewport = null
    }
    pdfViewer.setDocument(null as unknown as PDFDocumentProxy)
    pdfViewer = null
  }
  clearBookBlockSelectionBboxHighlight()
  pendingNavigation = null
})
</script>

<style scoped>
.pdf-book-viewer-container {
  overflow: auto;
  position: absolute;
  inset: 0;
  background-color: #808080;
}
</style>
