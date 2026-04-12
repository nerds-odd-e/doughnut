<template>
  <div
    role="status"
    aria-live="polite"
    aria-atomic="true"
    data-testid="book-reading-current-block-live"
    class="daisy-sr-only"
  >
    {{ currentBlockLiveText }}
  </div>
  <GlobalBar>
    <BookLayoutToggleButton
      v-model:opened="bookLayoutOpened"
      :panel-id="bookReadingBookLayoutPanelId"
    />
    <router-link
      :to="{ name: 'notebookEdit', params: { notebookId: notebookId } }"
      class="daisy-btn daisy-btn-sm daisy-btn-ghost daisy-shrink-0 daisy-no-underline"
    >
      Notebook
    </router-link>
    <span
      class="daisy-truncate daisy-text-sm daisy-font-medium daisy-min-w-0 daisy-ml-1"
      :title="book.bookName"
    >
      {{ book.bookName }}
    </span>
    <PdfControl
      class="daisy-ml-auto daisy-mr-2"
      :current-page="pdfBarCurrentPage"
      :pages-total="pdfBarPagesTotal"
      @zoom-in="pdfViewerRef?.zoomIn()"
      @zoom-out="pdfViewerRef?.zoomOut()"
    />
  </GlobalBar>
  <BookReadingBookLayout
    v-model:opened="bookLayoutOpened"
    :panel-id="bookReadingBookLayoutPanelId"
    :is-md-or-larger="isMdOrLarger"
    :blocks="bookBlocks"
    :current-block-id="currentBlockId"
    :selected-block-id="selectedBlockId"
    :disposition-for-block="bookReading.dispositionForBlock"
    @block-click="onBookBlockClick"
    @block-indent="onBlockIndent"
  >
    <main
      ref="mainPaneRef"
      class="daisy-flex-1 daisy-min-h-0 daisy-min-w-0 daisy-relative"
    >
      <div
        v-if="pdfViewerLoadError"
        class="daisy-alert daisy-alert-error daisy-mb-2 daisy-mx-2 daisy-mt-2"
        data-testid="book-reading-pdf-viewer-load-error"
      >
        {{ pdfViewerLoadError }}
      </div>
      <template v-else>
        <PdfBookViewer
          ref="pdfViewerRef"
          :pdf-bytes="bookPdfBytes"
          @load-error="onPdfLoadError"
          @viewport-anchor-page="onViewportAnchorPage"
          @pages-ready="onPagesReady"
        />
        <ReadingControlPanel
          v-if="blockAwaitingConfirmation"
          :selected-block-title="blockAwaitingConfirmation.title"
          :snap-animation-key="snapAnimationKey"
          :anchor-top-px="readingPanelAnchorTopPx"
          @mark-as-read="() => markSelectedDisposition('READ')"
          @mark-as-skimmed="() => markSelectedDisposition('SKIMMED')"
          @mark-as-skipped="() => markSelectedDisposition('SKIPPED')"
        />
        <CurrentBlockNavigationBar
          v-if="currentBlockForNavBar"
          :current-block-title="currentBlockForNavBar.title"
          @read-from-here="onReadFromHere"
          @back-to-selected="onBackToSelected"
        />
      </template>
    </main>
  </BookReadingBookLayout>
</template>

<script setup lang="ts">
import BookLayoutToggleButton from "@/components/book-reading/BookLayoutToggleButton.vue"
import BookReadingBookLayout from "@/components/book-reading/BookReadingBookLayout.vue"
import CurrentBlockNavigationBar from "@/components/book-reading/CurrentBlockNavigationBar.vue"
import GlobalBar from "@/components/toolbars/GlobalBar.vue"
import PdfBookViewer from "@/components/book-reading/PdfBookViewer.vue"
import PdfControl from "@/components/book-reading/PdfControl.vue"
import ReadingControlPanel from "@/components/book-reading/ReadingControlPanel.vue"
import { wireItemsToNavigationTargets } from "@/lib/book-reading/pdfOutlineV1Anchor"
import { createLastReadPositionPatchDebouncer } from "@/lib/book-reading/debounceLastReadPositionPatch"
import { createCurrentBlockIdDebouncer } from "@/lib/book-reading/debounceCurrentBlockId"
import { nextLiveAnnouncementText } from "@/lib/book-reading/currentBlockLiveAnnouncement"
import { currentBlockIdFromVisiblePage } from "@/lib/book-reading/currentBlockIdFromVisiblePage"
import type { ViewportYRange } from "@/lib/book-reading/pdfViewerViewportTopYDown"
import {
  useBookReadingSnapBack,
  type BookReadingPdfViewerRef,
} from "@/composables/useBookReadingSnapBack"
import { useNotebookBookReadingRecords } from "@/composables/useNotebookBookReadingRecords"
import type { BookBlockReadingDisposition } from "@/lib/book-reading/readBlockIdsFromRecords"
import type { BookBlockFull, BookFull } from "@generated/doughnut-backend-api"
import { NotebookBooksController } from "@generated/doughnut-backend-api/sdk.gen"
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue"

const emit = defineEmits<{
  "update:book": [book: BookFull]
}>()

const bookReadingBookLayoutPanelId = "book-reading-book-layout-panel"
const BOOK_READING_LAYOUT_BREAKPOINT_PX = 768
const CURRENT_BLOCK_ID_DEBOUNCE_MS = 120
const LAST_READ_POSITION_PATCH_DEBOUNCE_MS = 400
/** Vertical space (px) reserved at the bottom of the PDF main pane by ReadingControlPanel. */
const READING_PANEL_OBSTRUCTION_PX = 80
/** Fallback to bottom-docked panel if anchored top would leave too little main-pane height. */
const MIN_READING_PANEL_RESERVE_PX = 88
const SNAP_HOLD_MS = 500

const props = withDefaults(
  defineProps<{
    book: BookFull
    bookPdfBytes: ArrayBuffer
    initialLastRead: { pageIndexZeroBased: number; normalizedY: number } | null
    initialSelectedBlockId?: number | null
  }>(),
  { initialSelectedBlockId: null }
)

const notebookId = computed(() => Number(props.book.notebookId))
const bookReading = useNotebookBookReadingRecords(notebookId)

const pdfViewerLoadError = ref<string | null>(null)

const pdfBarCurrentPage = ref<number | null>(null)
const pdfBarPagesTotal = ref<number | null>(null)

const bookLayoutOpened = ref(false)
const windowWidth = ref(
  typeof window !== "undefined"
    ? window.innerWidth
    : BOOK_READING_LAYOUT_BREAKPOINT_PX
)

function handleResize() {
  windowWidth.value = window.innerWidth
}

const isMdOrLarger = computed(
  () => windowWidth.value >= BOOK_READING_LAYOUT_BREAKPOINT_PX
)

function onPdfLoadError(message: string) {
  pdfViewerLoadError.value = message
}

const bookBlocks = computed(() => props.book.blocks)

const currentBlockId = ref<number | null>(null)

const selectedBlockId = ref<number | null>(props.initialSelectedBlockId ?? null)

const currentBlockForNavBar = computed(() => {
  const curId = currentBlockId.value
  const selId = selectedBlockId.value
  if (curId === null || selId === null || curId === selId) return null
  return bookBlocks.value.find((b) => b.id === curId) ?? null
})

const lastReadingForPatch = ref<{
  pageIndex: number
  normalizedY: number
} | null>(null)

const pdfViewerRef = ref<BookReadingPdfViewerRef | null>(null)
const mainPaneRef = ref<HTMLElement | null>(null)
const readingPanelAnchorTopPx = ref<number | null>(null)

const {
  snapAnimationKey,
  blockAwaitingConfirmation,
  lastContentBottomVisible,
  shouldSnapBack,
  performSnapBack,
  updateLastDirectContentGeometry,
  clearSnapbackAttemptsForBlock,
  hasDirectContent,
} = useBookReadingSnapBack({
  bookBlocks,
  selectedBlockId,
  currentBlockId,
  hasRecordedDisposition: bookReading.hasRecordedDisposition,
  pdfViewerRef,
  obstructionPx: READING_PANEL_OBSTRUCTION_PX,
  snapHoldMs: SNAP_HOLD_MS,
})

function updateReadingPanelAnchor() {
  const mainEl = mainPaneRef.value
  const pdf = pdfViewerRef.value
  const selId = selectedBlockId.value
  if (!mainEl || !pdf || selId === null || !lastContentBottomVisible.value) {
    readingPanelAnchorTopPx.value = null
    return
  }
  const block = bookBlocks.value.find((b) => b.id === selId)
  if (!block || block.allBboxes.length < 2) {
    readingPanelAnchorTopPx.value = null
    return
  }
  const lastBbox = block.allBboxes[block.allBboxes.length - 1]!
  const target = {
    pageIndex: lastBbox.pageIndex,
    bbox: lastBbox.bbox as [number, number, number, number],
  }
  let top = pdf.readingPanelAnchorTopPx(
    mainEl,
    target,
    READING_PANEL_OBSTRUCTION_PX
  )
  if (top !== null) {
    const mainH = mainEl.getBoundingClientRect().height
    if (mainH > 0 && top + MIN_READING_PANEL_RESERVE_PX > mainH - 8) {
      top = null
    }
  }
  readingPanelAnchorTopPx.value = top
}

const currentBlockLiveText = ref("")
const lastAnnouncedCurrentBlockTitle = ref<string | undefined>(undefined)

const currentBlockIdDebouncer = createCurrentBlockIdDebouncer({
  delayMs: CURRENT_BLOCK_ID_DEBOUNCE_MS,
  commit: (id) => {
    if (shouldSnapBack(id)) {
      performSnapBack()
      return false
    }
    currentBlockId.value = id
    return true
  },
})

const lastReadPositionPatchDebouncer = createLastReadPositionPatchDebouncer({
  delayMs: LAST_READ_POSITION_PATCH_DEBOUNCE_MS,
  patch: (body) =>
    NotebookBooksController.patchNotebookBookReadingPosition({
      path: { notebook: notebookId.value },
      body,
    }),
})

function onViewportAnchorPage(payload: {
  anchorPageIndexZeroBased: number
  viewport: ViewportYRange | null
  pagesCount: number
  readingPosition?: {
    pageIndexZeroBased: number
    normalizedTop: number
  } | null
}) {
  if (payload.pagesCount > 0) {
    pdfBarCurrentPage.value = payload.anchorPageIndexZeroBased + 1
    pdfBarPagesTotal.value = payload.pagesCount
  }
  const candidate = currentBlockIdFromVisiblePage(
    bookBlocks.value.map((r) => ({
      id: r.id,
      firstBbox: r.allBboxes?.[0],
    })),
    payload.anchorPageIndexZeroBased,
    payload.viewport,
    payload.pagesCount
  )
  currentBlockIdDebouncer.propose(candidate)
  updateLastDirectContentGeometry()
  updateReadingPanelAnchor()
  let reading: { pageIndexZeroBased: number; normalizedTop: number } | null =
    null
  if (payload.readingPosition !== undefined) {
    reading = payload.readingPosition
  } else if (payload.viewport !== null) {
    reading = {
      pageIndexZeroBased: payload.anchorPageIndexZeroBased,
      normalizedTop: payload.viewport.top,
    }
  }
  if (reading !== null) {
    const pageIndex = reading.pageIndexZeroBased
    const normalizedY = Math.round(reading.normalizedTop)
    lastReadingForPatch.value = { pageIndex, normalizedY }
    const sel = selectedBlockId.value
    lastReadPositionPatchDebouncer.propose(
      pageIndex,
      normalizedY,
      sel === null ? undefined : sel
    )
  }
}

watch(selectedBlockId, (id) => {
  readingPanelAnchorTopPx.value = null
  const last = lastReadingForPatch.value
  if (last === null || id === null) {
    return
  }
  lastReadPositionPatchDebouncer.propose(last.pageIndex, last.normalizedY, id)
})

watch(currentBlockId, (id) => {
  const { text, changed } = nextLiveAnnouncementText(
    lastAnnouncedCurrentBlockTitle.value,
    id,
    bookBlocks.value
  )
  if (!changed) {
    return
  }
  lastAnnouncedCurrentBlockTitle.value = text
  currentBlockLiveText.value = text
})

watch(currentBlockId, async (blockId) => {
  if (blockId === null) return
  const rows = bookBlocks.value
  const bIdx = rows.findIndex((r) => r.id === blockId)
  if (bIdx <= 0) return
  const predecessor = rows[bIdx - 1]!
  if (
    !hasDirectContent(predecessor) &&
    predecessor.allBboxes.length > 0 &&
    !bookReading.hasRecordedDisposition(predecessor.id)
  ) {
    await bookReading.submitReadingDisposition(predecessor.id, "READ")
  }
})

watch(
  bookBlocks,
  (blocks) => {
    if (blocks.length === 0) {
      selectedBlockId.value = null
      return
    }
    const id = selectedBlockId.value
    if (id === null || !blocks.some((r) => r.id === id)) {
      selectedBlockId.value = blocks[0]!.id
    }
  },
  { immediate: true }
)

async function applyBookBlockSelection(block: BookBlockFull) {
  const targets = wireItemsToNavigationTargets(block.allBboxes)
  const parsed = targets[0] ?? null
  if (parsed === null) {
    return
  }
  selectedBlockId.value = block.id
  await pdfViewerRef.value?.scrollToBookNavigationTarget(parsed, targets)
  currentBlockIdDebouncer.commitNow(block.id)
}

async function markSelectedDisposition(status: BookBlockReadingDisposition) {
  const id = selectedBlockId.value
  if (id === null) {
    return
  }
  const ok = await bookReading.submitReadingDisposition(id, status)
  if (!ok) {
    return
  }
  if (status === "READ") {
    clearSnapbackAttemptsForBlock(id)
  }
  const rows = bookBlocks.value
  const selIdx = rows.findIndex((r) => r.id === id)
  if (selIdx >= 0 && selIdx < rows.length - 1) {
    await applyBookBlockSelection(rows[selIdx + 1]!)
  }
}

function onPagesReady() {
  const snap = props.initialLastRead
  if (!snap) return
  pdfViewerRef.value
    ?.scrollToStoredReadingPosition(snap.pageIndexZeroBased, snap.normalizedY)
    .catch(() => undefined)
}

async function onBookBlockClick(block: BookBlockFull) {
  await applyBookBlockSelection(block)
}

async function onBlockIndent(block: BookBlockFull) {
  const { data, error } = await NotebookBooksController.changeBookBlockDepth({
    path: { notebook: notebookId.value, bookBlock: block.id },
    body: { direction: "INDENT" },
  })
  if (!error && data) {
    emit("update:book", data)
    selectedBlockId.value = block.id
  }
}

async function onReadFromHere() {
  const block = currentBlockForNavBar.value
  if (block) await applyBookBlockSelection(block)
}

async function onBackToSelected() {
  const selId = selectedBlockId.value
  if (selId === null) return
  const block = bookBlocks.value.find((b) => b.id === selId)
  if (block) await applyBookBlockSelection(block)
}

onMounted(async () => {
  window.addEventListener("resize", handleResize)
  if (windowWidth.value >= BOOK_READING_LAYOUT_BREAKPOINT_PX) {
    bookLayoutOpened.value = true
  }
  await bookReading.syncFromServer()
})

onBeforeUnmount(() => {
  window.removeEventListener("resize", handleResize)
  currentBlockIdDebouncer.cancel()
  lastReadPositionPatchDebouncer.cancel()
})
</script>
