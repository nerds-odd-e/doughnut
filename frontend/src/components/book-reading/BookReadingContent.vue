<template>
  <BookReadingShell
    :session="session"
    format="pdf"
    :book-name="book.bookName"
    :load-error="pdfViewerLoadError"
    :snap-animation-key="snapAnimationKey"
    @block-indent="onBlockIndent"
    @block-outdent="onBlockOutdent"
    @block-cancel="onBlockCancel"
    @request-ai-reorganize="requestAiReorganize"
  >
    <template #bar-end>
      <PdfControl
        class="ml-auto mr-2"
        :current-page="pdfBarCurrentPage"
        :pages-total="pdfBarPagesTotal"
        @zoom-in="pdfViewerRef?.zoomIn()"
        @zoom-out="pdfViewerRef?.zoomOut()"
      />
    </template>
    <PdfBookViewer
      ref="pdfViewerRef"
      :pdf-bytes="bookPdfBytes"
      :bottom-padding-px="READING_PANEL_OBSTRUCTION_PX"
      @load-error="onPdfLoadError"
      @viewport-anchor-page="onViewportAnchorPage"
      @pages-ready="onPagesReady"
      @create-block-from-content="onCreateBlockFromContent"
    />
    <template #pane-end>
      <CurrentBlockNavigationBar
        v-if="currentBlockForNavBar"
        :current-block-title="currentBlockForNavBar.title"
        @read-from-here="onReadFromHere"
        @back-to-selected="onBackToSelected"
      />
    </template>
  </BookReadingShell>
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
import BookLayoutReorganizePreviewDialog from "@/components/book-reading/BookLayoutReorganizePreviewDialog.vue"
import BookReadingShell from "@/components/book-reading/BookReadingShell.vue"
import CurrentBlockNavigationBar from "@/components/book-reading/CurrentBlockNavigationBar.vue"
import NewBookBlockTitleDialog from "@/components/book-reading/NewBookBlockTitleDialog.vue"
import PdfBookViewer from "@/components/book-reading/PdfBookViewer.vue"
import PdfControl from "@/components/book-reading/PdfControl.vue"
import { pdfLocatorsFromBlock } from "@/lib/book-reading/asPdfLocator"
import { wireItemsToNavigationTargets } from "@/lib/book-reading/pdfOutlineV1Anchor"
import { currentBlockIdFromVisiblePage } from "@/lib/book-reading/currentBlockIdFromVisiblePage"
import type { ViewportYRange } from "@/lib/book-reading/pdfViewerViewportTopYDown"
import { READING_PANEL_OBSTRUCTION_PX } from "@/composables/useReadingPanelAnchor"
import { useBookReadingSnapBack } from "@/composables/useBookReadingSnapBack"
import type { BookReadingPdfViewerRef } from "@/composables/bookReaderViewerRef"
import { useBookLayoutAiReorganize } from "@/composables/useBookLayoutAiReorganize"
import { useBookReadingSession } from "@/composables/useBookReadingSession"
import {
  bookFullAfterLayoutMutation,
  useBookLayoutMutations,
} from "@/composables/book-reading/useBookLayoutMutations"
import type {
  BookBlockFull,
  BookFull,
  PdfLocatorFull,
} from "@generated/donut-backend-api"
import { NotebookBooksController } from "@generated/donut-backend-api/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import { computed, ref, watch } from "vue"

type ViewportPayload = {
  anchorPageIndexZeroBased: number
  viewport: ViewportYRange | null
  pagesCount: number
  readingPosition?: { pageIndexZeroBased: number; normalizedTop: number } | null
}

const emit = defineEmits<{
  "update:book": [book: BookFull]
}>()

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

function onPdfLoadError(message: string) {
  pdfViewerLoadError.value = message
}

const pdfViewerRef = ref<BookReadingPdfViewerRef | null>(null)

const session = useBookReadingSession({
  book: () => props.book,
  initialSelectedBlockId: props.initialSelectedBlockId ?? null,
  surface: {
    showBlock,
    readingPositionLocator,
    viewer: pdfViewerRef,
    commitCurrentBlock: commitCurrentBlockId,
    blockAwaitingConfirmation: () => snapBlockAwaitingConfirmation.value,
    canAnchorPanel: () => lastContentBottomVisible.value,
    onMarkedRead: (id) => clearSnapbackAttemptsForBlock(id),
    repairSelection: true,
  },
})
const {
  notebookId,
  bookBlocks,
  bookReading,
  selectedBlockId,
  currentBlockId,
  currentBlockIdDebouncer,
  proposeReadingPosition,
  applyBookBlockSelection,
  updateReadingPanelAnchor,
} = session

async function showBlock(block: BookBlockFull) {
  const targets = wireItemsToNavigationTargets(pdfLocatorsFromBlock(block))
  const parsed = targets[0] ?? null
  if (parsed === null) {
    return
  }
  selectedBlockId.value = block.id
  await pdfViewerRef.value?.scrollToBookNavigationTarget(parsed, targets)
  currentBlockIdDebouncer.commitNow(block.id)
}

function readingPositionLocator(): PdfLocatorFull | null {
  const last = lastReadingForPatch.value
  if (last === null) return null
  const y = Math.max(0, Math.min(1000, last.normalizedY))
  return {
    type: "PdfLocator_Full",
    pageIndex: last.pageIndex,
    bbox: [0, y, 0, y],
  }
}

const {
  suggestion: aiSuggestion,
  previewRows: aiPreviewRows,
  requestSuggest: requestAiReorganize,
  confirmSuggest: confirmAiReorganize,
  dismiss: dismissAiReorganizePreview,
} = useBookLayoutAiReorganize(notebookId, bookBlocks)

const currentBlockForNavBar = computed(() => {
  const curId = currentBlockId.value
  const selId = selectedBlockId.value
  if (curId === null || selId === null || curId === selId) return null
  return bookBlocks.value.find((b) => b.id === curId) ?? null
})

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

const { onBlockIndent, onBlockOutdent, onBlockCancel } = useBookLayoutMutations(
  {
    notebookId,
    bookBlocks,
    getPropBook: () => props.book,
    selectedBlockId,
    applyBookBlockSelection,
    onBookUpdated: (book) => emit("update:book", book),
  }
)

function commitCurrentBlockId(id: number | null): boolean {
  if (shouldSnapBack(id)) {
    performSnapBack()
    return false
  }
  return true
}

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
</script>
