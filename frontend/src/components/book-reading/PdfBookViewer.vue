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
  pdfViewerViewportTopYDown,
  type ViewportYRange,
} from "@/lib/book-reading/pdfViewerViewportTopYDown"
import { attachOutlineSelectionBboxHighlight } from "@/lib/book-reading/outlineSelectionBboxHighlight"
import { mineruOutlineV1BboxToXyzDestArray } from "@/lib/book-reading/mineruOutlineV1PageIndex"
import type { MineruOutlineV1Bbox } from "@/lib/book-reading/mineruOutlineV1PageIndex"
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
    },
  ]
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
let detachGeometryResampleListeners: (() => void) | null = null
let intrinsicFirstPageWidth = 0
let userAdjustedScale = false

const SCALE_EPSILON = 0.001

function teardownGeometryResample() {
  geometryRafEmitter?.cancel()
  geometryRafEmitter = null
  detachGeometryResampleListeners?.()
  detachGeometryResampleListeners = null
}

function detachViewportScrollListener(container: HTMLElement) {
  if (onScrollForViewport) {
    container.removeEventListener("scroll", onScrollForViewport)
    onScrollForViewport = null
  }
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
  emit("viewportAnchorPage", {
    anchorPageIndexZeroBased: sample.anchorPageIndexZeroBased,
    viewport: sample.viewport,
    pagesCount: pdfViewer.pagesCount,
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

type PendingNav = {
  pageIndexZeroBased: number
  bbox: MineruOutlineV1Bbox | null
}

let pendingNavigation: PendingNav | null = null
let detachOutlineSelectionBboxHighlight: (() => void) | null = null

function clearOutlineSelectionBboxHighlight() {
  detachOutlineSelectionBboxHighlight?.()
  detachOutlineSelectionBboxHighlight = null
}

function showOutlineSelectionBboxHighlight(
  pageNumber: number,
  bbox: MineruOutlineV1Bbox
) {
  clearOutlineSelectionBboxHighlight()
  if (!pdfViewer) return
  const pageView = pdfViewer.getPageView(pageNumber - 1)
  if (!pageView?.div) return
  detachOutlineSelectionBboxHighlight = attachOutlineSelectionBboxHighlight(
    pageView.div,
    pageView.viewport.width,
    pageView.viewport.height,
    bbox
  )
}

async function applyMineruOutlineV1Target(
  pageIndexZeroBased: number,
  bbox: MineruOutlineV1Bbox | null
) {
  if (!pdfViewer?.pdfDocument) return
  if (
    !Number.isInteger(pageIndexZeroBased) ||
    pageIndexZeroBased < 0 ||
    pageIndexZeroBased >= pdfViewer.pagesCount
  ) {
    return
  }
  const pageNumber = pageIndexZeroBased + 1
  if (bbox === null) {
    clearOutlineSelectionBboxHighlight()
    pdfViewer.scrollPageIntoView({ pageNumber })
  } else {
    const page = await pdfViewer.pdfDocument.getPage(pageNumber)
    const vp = page.getViewport({ scale: 1 })
    const destArray = mineruOutlineV1BboxToXyzDestArray(
      vp.width,
      vp.height,
      bbox
    )
    pdfViewer.scrollPageIntoView({ pageNumber, destArray: [...destArray] })
    showOutlineSelectionBboxHighlight(pageNumber, bbox)
  }
  queueMicrotask(() => emitViewportDescriptorIfChanged())
}

function flushPendingNavigation() {
  if (pendingNavigation === null || !pdfViewer?.pdfDocument) {
    return
  }
  const shot = pendingNavigation
  pendingNavigation = null
  applyMineruOutlineV1Target(shot.pageIndexZeroBased, shot.bbox).catch(() => {
    /* Outline jump failures from pdf.js must not reject pagesinit / viewer setup. */
  })
}

async function scrollToMineruOutlineV1Target(target: {
  pageIndexZeroBased: number
  bbox: MineruOutlineV1Bbox | null
}) {
  const { pageIndexZeroBased, bbox } = target
  if (!Number.isInteger(pageIndexZeroBased) || pageIndexZeroBased < 0) {
    return
  }
  if (!pdfViewer?.pdfDocument) {
    pendingNavigation = { pageIndexZeroBased, bbox }
    return
  }
  await applyMineruOutlineV1Target(pageIndexZeroBased, bbox)
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

defineExpose({ scrollToMineruOutlineV1Target, zoomIn, zoomOut })

async function loadPdf(bytes: ArrayBuffer | Uint8Array) {
  const container = containerRef.value
  const viewer = viewerRef.value
  if (!container || !viewer) return

  if (currentLoadingTask) {
    await currentLoadingTask.destroy()
    currentLoadingTask = null
  }
  if (pdfViewer) {
    clearOutlineSelectionBboxHighlight()
    if (onPageChangingForViewport) {
      pdfViewer.eventBus.off("pagechanging", onPageChangingForViewport)
      onPageChangingForViewport = null
    }
    pdfViewer.setDocument(null as unknown as PDFDocumentProxy)
    pdfViewer = null
    pendingNavigation = null
  }

  detachViewportScrollListener(container)
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

  geometryRafEmitter = createCoalescedRequestAnimationFrameEmitter({
    emit: () => {
      applyResponsiveDefaultScale()
      emitViewportDescriptorIfChanged()
    },
  })
  detachGeometryResampleListeners =
    attachPdfBookViewerGeometryResampleListeners({
      container,
      eventBus,
      scheduleEmit: () => geometryRafEmitter!.schedule(),
    })

  eventBus.on("pagesinit", () => {
    if (pdfViewer) {
      applyResponsiveDefaultScale({ force: true })
      flushPendingNavigation()
      emitViewportDescriptorIfChanged()
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
  const container = containerRef.value
  if (container) {
    detachViewportScrollListener(container)
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
  clearOutlineSelectionBboxHighlight()
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
