<template>
  <div
    role="status"
    aria-live="polite"
    aria-atomic="true"
    data-testid="book-reading-current-block-live"
    class="sr-only"
  >
    {{ currentBlockLiveText }}
  </div>
  <GlobalBar>
    <BookLayoutToggleButton
      v-model:opened="bookLayoutOpened"
      :panel-id="bookReadingBookLayoutPanelId"
    />
    <router-link
      :to="{ name: 'notebookPage', params: { notebookId: notebookId } }"
      class="daisy-btn daisy-btn-sm daisy-btn-ghost shrink-0 no-underline"
    >
      Notebook
    </router-link>
    <span
      class="truncate text-sm font-medium min-w-0 ml-1"
      :title="book.bookName"
    >
      {{ book.bookName }}
    </span>
    <PdfControl
      class="ml-auto mr-2"
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
    :pending-layout-block-id="pendingLayoutBlockId"
    :full-layout-busy="aiSuggestPending"
    :disposition-for-block="bookReading.dispositionForBlock"
    @block-click="onBookBlockClick"
    @block-indent="onBlockIndent"
    @block-outdent="onBlockOutdent"
    @block-cancel="onBlockCancel"
    @request-ai-reorganize="requestAiReorganize"
  >
    <main
      class="flex flex-1 min-h-0 min-w-0 flex-col"
    >
      <div
        v-if="pdfViewerLoadError"
        class="daisy-alert daisy-alert-error mb-2 mx-2 mt-2"
        data-testid="book-reading-pdf-viewer-load-error"
      >
        {{ pdfViewerLoadError }}
      </div>
      <div
        v-else
        class="flex min-h-0 min-w-0 flex-1 flex-col"
      >
        <div
          ref="pdfPaneRef"
          class="relative min-h-0 min-w-0 flex-1"
        >
          <PdfBookViewer
            ref="pdfViewerRef"
            :pdf-bytes="bookPdfBytes"
            :bottom-padding-px="READING_PANEL_OBSTRUCTION_PX"
            @load-error="onPdfLoadError"
            @viewport-anchor-page="onViewportAnchorPage"
            @pages-ready="onPagesReady"
            @create-block-from-content="onCreateBlockFromContent"
          />
          <ReadingControlPanel
            v-if="blockAwaitingConfirmation"
            :selected-block-title="blockAwaitingConfirmation.title"
            :snap-animation-key="snapAnimationKey"
            :anchor-top-px="readingPanelAnchorTopPx"
            @mark-as-read="() => markSelectedBlockDisposition('READ')"
            @mark-as-skimmed="() => markSelectedBlockDisposition('SKIMMED')"
            @mark-as-skipped="() => markSelectedBlockDisposition('SKIPPED')"
          />
          <CurrentBlockNavigationBar
            v-if="currentBlockForNavBar"
            :current-block-title="currentBlockForNavBar.title"
            @read-from-here="onReadFromHere"
            @back-to-selected="onBackToSelected"
          />
        </div>
      </div>
    </main>
  </BookReadingBookLayout>
  <NewBookBlockTitleDialog
    :open="pendingBlockCreation !== null"
    :default-title="pendingBlockCreation?.structuralTitle"
    @confirm="onConfirmBlockTitle"
    @cancel="pendingBlockCreation = null"
  />
  <BookLayoutReorganizePreviewDialog
    :open="aiSuggestion !== null"
    :preview-rows="aiPreviewRows"
    @confirm="onConfirmAiReorganize"
    @cancel="dismissAiReorganizePreview"
  />
</template>

<script setup lang="ts">
import BookLayoutToggleButton from "@/components/book-reading/BookLayoutToggleButton.vue"
import BookLayoutReorganizePreviewDialog from "@/components/book-reading/BookLayoutReorganizePreviewDialog.vue"
import BookReadingBookLayout from "@/components/book-reading/BookReadingBookLayout.vue"
import CurrentBlockNavigationBar from "@/components/book-reading/CurrentBlockNavigationBar.vue"
import GlobalBar from "@/components/toolbars/GlobalBar.vue"
import NewBookBlockTitleDialog from "@/components/book-reading/NewBookBlockTitleDialog.vue"
import PdfBookViewer from "@/components/book-reading/PdfBookViewer.vue"
import PdfControl from "@/components/book-reading/PdfControl.vue"
import ReadingControlPanel from "@/components/book-reading/ReadingControlPanel.vue"
import {
  BOOK_READING_LAYOUT_BREAKPOINT_PX,
  bookLayoutAsideInitiallyOpen,
} from "@/lib/book-reading/bookReadingLayoutBreakpoint"
import { pdfLocatorsFromBlock } from "@/lib/book-reading/asPdfLocator"
import { wireItemsToNavigationTargets } from "@/lib/book-reading/pdfOutlineV1Anchor"
import { structuralTitleForBlockId } from "@/lib/book-reading/currentBlockLiveAnnouncement"
import { currentBlockIdFromVisiblePage } from "@/lib/book-reading/currentBlockIdFromVisiblePage"
import type { ViewportYRange } from "@/lib/book-reading/pdfViewerViewportTopYDown"
import {
  READING_PANEL_OBSTRUCTION_PX,
  useReadingPanelAnchor,
} from "@/composables/useReadingPanelAnchor"
import { useBookReadingSnapBack } from "@/composables/useBookReadingSnapBack"
import type { BookReadingPdfViewerRef } from "@/composables/bookReaderViewerRef"
import { useBookReadingCurrentBlock } from "@/composables/useBookReadingCurrentBlock"
import { useBookReadingSelection } from "@/composables/useBookReadingSelection"
import { useBookLayoutAiReorganize } from "@/composables/useBookLayoutAiReorganize"
import { useNotebookBookReadingRecords } from "@/composables/useNotebookBookReadingRecords"
import {
  bookFullAfterLayoutMutation,
  useBookLayoutMutations,
} from "@/composables/book-reading/useBookLayoutMutations"
import type {
  BookBlockFull,
  BookFull,
  PdfLocatorFull,
} from "@generated/doughnut-backend-api"
import { NotebookBooksController } from "@generated/doughnut-backend-api/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue"

type ViewportPayload = {
  anchorPageIndexZeroBased: number
  viewport: ViewportYRange | null
  pagesCount: number
  readingPosition?: { pageIndexZeroBased: number; normalizedTop: number } | null
}

const emit = defineEmits<{
  "update:book": [book: BookFull]
}>()

const bookReadingBookLayoutPanelId = "book-reading-book-layout-panel"
const SNAP_HOLD_MS = 500
const STRUCTURAL_TITLE_MAX_CHARS = 512

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
const viewportPayload = ref<ViewportPayload | null>(null)

const pdfBarCurrentPage = computed(() => {
  const p = viewportPayload.value
  return p && p.pagesCount > 0 ? p.anchorPageIndexZeroBased + 1 : null
})

const pdfBarPagesTotal = computed(() => {
  const p = viewportPayload.value
  return p && p.pagesCount > 0 ? p.pagesCount : null
})

const lastReadingForPatch = computed(() => {
  const p = viewportPayload.value
  if (!p) return null
  let reading: { pageIndexZeroBased: number; normalizedTop: number } | null =
    null
  if (p.readingPosition !== undefined) {
    reading = p.readingPosition
  } else if (p.viewport !== null) {
    reading = {
      pageIndexZeroBased: p.anchorPageIndexZeroBased,
      normalizedTop: p.viewport.top,
    }
  }
  if (reading === null) return null
  return {
    pageIndex: reading.pageIndexZeroBased,
    normalizedY: Math.round(reading.normalizedTop),
  }
})

const windowWidth = ref(
  typeof window !== "undefined"
    ? window.innerWidth
    : BOOK_READING_LAYOUT_BREAKPOINT_PX
)
const bookLayoutOpened = ref(bookLayoutAsideInitiallyOpen(windowWidth.value))

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
const selectedBlockId = ref<number | null>(props.initialSelectedBlockId ?? null)

const {
  suggestPending: aiSuggestPending,
  suggestion: aiSuggestion,
  previewRows: aiPreviewRows,
  requestSuggest: requestAiReorganize,
  confirmSuggest: confirmAiReorganize,
  dismiss: dismissAiReorganizePreview,
} = useBookLayoutAiReorganize(notebookId, bookBlocks)

const { currentBlockId, currentBlockIdDebouncer, proposeReadingPosition } =
  useBookReadingCurrentBlock({
    notebookId,
    commitCurrentBlock: commitCurrentBlockId,
    proposeReadingPosition: (debouncer) => () => {
      const last = lastReadingForPatch.value
      if (last === null) return
      const sel = selectedBlockId.value
      const y = Math.max(0, Math.min(1000, last.normalizedY))
      const locator: PdfLocatorFull = {
        type: "PdfLocator_Full",
        pageIndex: last.pageIndex,
        bbox: [0, y, 0, y],
      }
      debouncer.propose(locator, sel === null ? undefined : sel)
    },
  })

const currentBlockForNavBar = computed(() => {
  const curId = currentBlockId.value
  const selId = selectedBlockId.value
  if (curId === null || selId === null || curId === selId) return null
  return bookBlocks.value.find((b) => b.id === curId) ?? null
})

const pdfViewerRef = ref<BookReadingPdfViewerRef | null>(null)
const pdfPaneRef = ref<HTMLElement | null>(null)

const {
  snapAnimationKey,
  blockAwaitingConfirmation: snapBlockAwaitingConfirmation,
  lastContentBottomVisible,
  shouldSnapBack,
  performSnapBack,
  updateLastDirectContentGeometry,
  clearSnapbackAttemptsForBlock,
} = useBookReadingSnapBack({
  bookBlocks,
  selectedBlockId,
  currentBlockId,
  hasRecordedDisposition: bookReading.hasRecordedDisposition,
  pdfViewerRef,
  obstructionPx: READING_PANEL_OBSTRUCTION_PX,
  snapHoldMs: SNAP_HOLD_MS,
})

const {
  blockAwaitingConfirmation,
  applyBookBlockSelection,
  markSelectedBlockDisposition,
} = useBookReadingSelection({
  bookBlocks,
  currentBlockId,
  hasRecordedDisposition: bookReading.hasRecordedDisposition,
  submitReadingDisposition: bookReading.submitReadingDisposition,
  selectedBlockId,
  initialSelectedBlockId: props.initialSelectedBlockId ?? null,
  repairSelectionWhenBlocksChange: true,
  overrideBlockAwaitingConfirmation: snapBlockAwaitingConfirmation,
  onMarkedRead: (id) => clearSnapbackAttemptsForBlock(id),
  onAdvance: async (block) => {
    const targets = wireItemsToNavigationTargets(pdfLocatorsFromBlock(block))
    const parsed = targets[0] ?? null
    if (parsed === null) {
      return
    }
    selectedBlockId.value = block.id
    await pdfViewerRef.value?.scrollToBookNavigationTarget(parsed, targets)
    currentBlockIdDebouncer.commitNow(block.id)
  },
})

const readingPanelBlockRef = computed(() =>
  lastContentBottomVisible.value ? blockAwaitingConfirmation.value : null
)

const { readingPanelAnchorTopPx, updateReadingPanelAnchor } =
  useReadingPanelAnchor({
    viewerRef: pdfViewerRef,
    blockRef: readingPanelBlockRef,
    mainPaneRef: pdfPaneRef,
  })

const { pendingLayoutBlockId, onBlockIndent, onBlockOutdent, onBlockCancel } =
  useBookLayoutMutations({
    notebookId,
    bookBlocks,
    getPropBook: () => props.book,
    selectedBlockId,
    applyBookBlockSelection,
    onBookUpdated: (book) => emit("update:book", book),
  })

function commitCurrentBlockId(id: number | null): boolean {
  if (shouldSnapBack(id)) {
    performSnapBack()
    return false
  }
  return true
}

const currentBlockLiveText = computed(() =>
  structuralTitleForBlockId(currentBlockId.value, bookBlocks.value)
)

/**
 * Scroll → current-block pipeline:
 *   PdfBookViewer emits `viewportAnchorPage` → here we map anchor page + viewport Y-range to a block ID
 *   → result is debounced through `currentBlockIdDebouncer`.
 */
function onViewportAnchorPage(payload: ViewportPayload) {
  viewportPayload.value = payload
  const candidate = currentBlockIdFromVisiblePage(
    bookBlocks.value.map((r) => {
      const first = pdfLocatorsFromBlock(r)[0]
      return {
        id: r.id,
        firstBbox: first
          ? { pageIndex: first.pageIndex, bbox: first.bbox }
          : undefined,
      }
    }),
    payload.anchorPageIndexZeroBased,
    payload.viewport,
    payload.pagesCount
  )
  currentBlockIdDebouncer.propose(candidate)
  updateLastDirectContentGeometry()
  updateReadingPanelAnchor()
  proposeReadingPosition()
}

watch(selectedBlockId, (id) => {
  readingPanelAnchorTopPx.value = null
  if (id === null) {
    return
  }
  proposeReadingPosition()
})

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

async function onConfirmAiReorganize() {
  const mutation = await confirmAiReorganize()
  if (mutation) {
    emit("update:book", bookFullAfterLayoutMutation(props.book, mutation))
  }
}

const pendingBlockCreation = ref<{
  contentBlockId: number
  structuralTitle: string
} | null>(null)

async function createBlock(contentBlockId: number, structuralTitle?: string) {
  const { data, error } = await apiCallWithLoading(() =>
    NotebookBooksController.createBookBlockFromContent({
      path: { notebook: notebookId.value },
      body: { fromBookContentBlockId: contentBlockId, structuralTitle },
    })
  )
  if (!error && data) {
    const newBlock = data.blocks.find(
      (b) => !props.book.blocks.some((old) => old.id === b.id)
    )
    emit("update:book", data)
    if (newBlock) {
      selectedBlockId.value = newBlock.id
    }
  }
}

function onCreateBlockFromContent({
  contentBlockId,
  derivedTitle,
}: {
  contentBlockId: number
  derivedTitle: string | undefined
}) {
  if (
    derivedTitle !== undefined &&
    derivedTitle.length >= STRUCTURAL_TITLE_MAX_CHARS
  ) {
    pendingBlockCreation.value = {
      contentBlockId,
      structuralTitle: derivedTitle,
    }
  } else {
    createBlock(contentBlockId)
  }
}

async function onConfirmBlockTitle(title: string | undefined) {
  const pending = pendingBlockCreation.value
  pendingBlockCreation.value = null
  if (pending) {
    await createBlock(pending.contentBlockId, title)
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
  await bookReading.syncFromServer()
})

onBeforeUnmount(() => {
  window.removeEventListener("resize", handleResize)
})
</script>
