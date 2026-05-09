<template>
  <div
    ref="containerRef"
    data-testid="pdf-book-viewer"
    class="pdf-book-viewer-container"
    :style="{ paddingBottom: props.bottomPaddingPx + 'px' }"
    @click="onContainerClick"
  >
    <div ref="viewerRef" class="pdfViewer" />
  </div>
  <div
    v-if="holdCallout"
    data-testid="new-book-block-callout"
    class="new-block-callout"
    :style="{ top: holdCallout.clientY + 'px', left: holdCallout.clientX + 'px' }"
  >
    <button
      data-testid="new-book-block-callout-confirm"
      class="daisy-btn daisy-btn-sm daisy-btn-primary"
      @click="onConfirmNewBlock"
    >
      New block
    </button>
    <button class="daisy-btn daisy-btn-sm" @click="holdCallout = null">
      Cancel
    </button>
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
  pdfViewerReadingPositionTopEdge,
  pdfViewerViewportTopYDown,
  type ViewportYRange,
} from "@/lib/book-reading/pdfViewerViewportTopYDown"
import { createIntervalScrollSuppression } from "@/lib/book-reading/intervalScrollSuppression"
import { usePdfBlockHighlight } from "@/composables/book-reading/usePdfBlockHighlight"
import { usePdfNavigation } from "@/composables/book-reading/usePdfNavigation"
import { usePdfLocatorGeometry } from "@/composables/book-reading/usePdfLocatorGeometry"
import { usePdfGestureZoom } from "@/composables/book-reading/usePdfGestureZoom"
import { getDocument, type PDFDocumentProxy } from "pdfjs-dist/build/pdf.mjs"
import {
  EventBus,
  PDFLinkService,
  PDFViewer,
} from "pdfjs-dist/web/pdf_viewer.mjs"
import "pdfjs-dist/web/pdf_viewer.css"
import type { PdfViewerScrollSuppressionApi } from "@/composables/bookReaderViewerRef"
import { nextTick, onBeforeUnmount, ref, watch } from "vue"

const props = withDefaults(
  defineProps<{
    pdfBytes: ArrayBuffer | Uint8Array | null
    bottomPaddingPx?: number
  }>(),
  { bottomPaddingPx: 0 }
)

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
  createBlockFromContent: [
    { contentBlockId: number; derivedTitle: string | undefined },
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
let viewportOnlyRafEmitter: ReturnType<
  typeof createCoalescedRequestAnimationFrameEmitter
> | null = null
let detachGeometryResampleListeners: (() => void) | null = null
let intrinsicFirstPageWidth = 0
let userAdjustedScale = false

let scrollSuppression: PdfViewerScrollSuppressionApi =
  createIntervalScrollSuppression()

function registerScrollSuppression(
  api: PdfViewerScrollSuppressionApi
): () => void {
  scrollSuppression = api
  return () => {
    scrollSuppression = createIntervalScrollSuppression()
  }
}

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

/**
 * Samples the current scroll position via `pdfViewerViewportTopYDown` and emits
 * `viewportAnchorPage` when the anchor page or viewport midpoint changes.
 */
function emitViewportDescriptorIfChanged() {
  const container = containerRef.value
  if (!container || !pdfViewer) return
  if (scrollSuppression.isHoldWindowActive()) {
    return
  }
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

const blockHighlight = usePdfBlockHighlight({
  getPdfViewer: () => pdfViewer,
  onCreateBlock: (payload) => emit("createBlockFromContent", payload),
})

const navigation = usePdfNavigation({
  getPdfViewer: () => pdfViewer,
  afterNavigate: () => emitViewportDescriptorIfChanged(),
  showHighlights: (targets) =>
    blockHighlight.showSelectionBboxHighlights(targets),
})

const locatorGeometry = usePdfLocatorGeometry({
  containerRef,
  getPdfViewer: () => pdfViewer,
})

const gestureZoom = usePdfGestureZoom({
  containerRef,
  getPdfViewer: () => pdfViewer,
  getScrollSuppression: () => scrollSuppression,
  onUserAdjusted: () => {
    userAdjustedScale = true
  },
  afterScaleChange: () => emitViewportDescriptorIfChanged(),
  onClearCallout: () => {
    blockHighlight.holdCallout.value = null
  },
})

const { holdCallout, onContainerClick, onConfirmNewBlock } = blockHighlight

defineExpose({
  displayLocator: navigation.displayLocator,
  resolveLocatorRect: locatorGeometry.resolveLocatorRect,
  scrollToBookNavigationTarget: navigation.scrollToBookNavigationTarget,
  highlightBlockSelection: blockHighlight.highlightBlockSelection,
  scrollToStoredReadingPosition: navigation.scrollToStoredReadingPosition,
  scrollPageNormalizedYToReadingClearance:
    locatorGeometry.scrollPageNormalizedYToReadingClearance,
  afterNextViewUpdate: locatorGeometry.afterNextViewUpdate,
  registerScrollSuppression,
  getPageRect: locatorGeometry.getPageRect,
  getScrollViewportHeightPx: locatorGeometry.getScrollViewportHeightPx,
  zoomIn: gestureZoom.zoomIn,
  zoomOut: gestureZoom.zoomOut,
  isLocatorBottomVisible: locatorGeometry.isLocatorBottomVisible,
  readingPanelAnchorTopPx: locatorGeometry.readingPanelAnchorTopPx,
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
    blockHighlight.clearBlockHighlight()
    if (onPageChangingForViewport) {
      pdfViewer.eventBus.off("pagechanging", onPageChangingForViewport)
      onPageChangingForViewport = null
    }
    pdfViewer.setDocument(null as unknown as PDFDocumentProxy)
    pdfViewer = null
    navigation.clearPendingNavigation()
  }

  detachViewportScrollListener(container)
  gestureZoom.detachGestureListeners(container)
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

  gestureZoom.attachGestureListeners(container)

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
      navigation.flushPendingNavigation()
      emitViewportDescriptorIfChanged()
      emit("pagesReady")
    }
  })

  const data =
    bytes instanceof Uint8Array ? new Uint8Array(bytes) : new Uint8Array(bytes)
  const loadingTask = getDocument({
    data,
    verbosity: 0,
  })
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
  } catch (e) {
    if (currentLoadingTask === loadingTask) {
      const fallback = "This file is not a valid PDF."
      if (e instanceof Error) {
        if (e.name === "InvalidPDFException") {
          emit("loadError", fallback)
        } else {
          emit("loadError", e.message || fallback)
        }
      } else {
        emit("loadError", fallback)
      }
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
  scrollSuppression.reset()
  const container = containerRef.value
  if (container) {
    detachViewportScrollListener(container)
    gestureZoom.detachGestureListeners(container)
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
  blockHighlight.clearBlockHighlight()
  navigation.clearPendingNavigation()
})
</script>

<style scoped>
.pdf-book-viewer-container {
  overflow: auto;
  position: absolute;
  inset: 0;
  background-color: #808080;
}

.new-block-callout {
  position: fixed;
  z-index: 1000;
  display: flex;
  gap: 0.5rem;
  padding: 0.5rem;
  background: var(--fallback-b1, oklch(var(--b1)));
  border: 1px solid var(--fallback-bc, oklch(var(--bc) / 0.2));
  border-radius: 0.5rem;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);
  transform: translate(-50%, -110%);
}
</style>
