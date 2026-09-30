<template>
  <BookReadingShell
    :session="session"
    format="pdf"
    :book-name="book.bookName"
    :load-error="pdfViewerLoadError"
    @overlay-wheel="pdfViewerRef?.scrollByWheel($event)"
  >
    <template #bar-end>
      <PdfControl
        class="ml-auto mr-2"
        :current-page="currentPage"
        :pages-total="pagesTotal"
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
  </BookReadingShell>
  <NewBookBlockTitleDialog
    :open="pendingBlockCreation !== null"
    :default-title="pendingBlockCreation?.structuralTitle"
    @confirm="onConfirmBlockTitle"
    @cancel="pendingBlockCreation = null"
  />
</template>

<script setup lang="ts">
import BookReadingShell from "@/components/book-reading/BookReadingShell.vue"
import NewBookBlockTitleDialog from "@/components/book-reading/NewBookBlockTitleDialog.vue"
import PdfBookViewer from "@/components/book-reading/PdfBookViewer.vue"
import PdfControl from "@/components/book-reading/PdfControl.vue"
import { pdfLocatorsFromBlock } from "@/lib/book-reading/asPdfLocator"
import { currentBlockIdFromViewStarts } from "@/lib/book-reading/currentBlockIdFromViewStarts"
import { wireItemsToNavigationTargets } from "@/lib/book-reading/pdfOutlineV1Anchor"
import {
  usePdfViewportPosition,
  type PdfViewportPayload,
} from "@/composables/book-reading/usePdfViewportPosition"
import { READING_PANEL_OBSTRUCTION_PX } from "@/composables/useReadingPanelAnchor"
import { useReadingPanelTarget } from "@/composables/useReadingPanelTarget"
import type { BookReadingPdfViewerRef } from "@/composables/bookReaderViewerRef"
import { useBookReadingSession } from "@/composables/useBookReadingSession"
import type { BookBlockFull, BookFull } from "@generated/donut-backend-api"
import { NotebookBooksController } from "@generated/donut-backend-api/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import { ref, watch } from "vue"

const emit = defineEmits<{
  "update:book": [book: BookFull]
}>()

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
const {
  payload: viewportPayload,
  currentPage,
  pagesTotal,
  readingPositionLocator,
} = usePdfViewportPosition()

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
    blockAwaitingConfirmation: () => readingPanelTargetBlock.value,
    anchoredBlock: () => readingPanelAnchoredBlock.value,
    repairSelection: true,
    reorganize: { onBookUpdated: (book) => emit("update:book", book) },
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
  currentBlockIdDebouncer.commitNow(currentBlockIdInView() ?? block.id)
}

function currentBlockIdInView(): number | null {
  const view = pdfViewerRef.value?.viewBlockStarts()
  if (!view) {
    return null
  }
  return currentBlockIdFromViewStarts(
    bookBlocks.value,
    {
      startTopPx: (block) => {
        const start = wireItemsToNavigationTargets(
          pdfLocatorsFromBlock(block)
        )[0]
        return start ? view.startTopPx(start) : null
      },
      landingLimitPx: view.landingLimitPx,
    },
    selectedBlockId.value
  )
}

const {
  blockAwaitingConfirmation: readingPanelTargetBlock,
  anchoredBlock: readingPanelAnchoredBlock,
  updateLastDirectContentGeometry,
} = useReadingPanelTarget({
  bookBlocks,
  selectedBlockId,
  currentBlockId,
  hasRecordedDisposition: bookReading.hasRecordedDisposition,
  pdfViewerRef,
  obstructionPx: READING_PANEL_OBSTRUCTION_PX,
})

/** PdfBookViewer's viewport → the debounced current block, panel anchor, and reading position. */
function onViewportAnchorPage(payload: PdfViewportPayload) {
  viewportPayload.value = payload
  const id = currentBlockIdInView()
  if (id !== null) {
    currentBlockIdDebouncer.propose(id)
  }
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
  pendingBlockCreation.value = {
    contentBlockId,
    structuralTitle: derivedTitle ?? "",
  }
}

async function onConfirmBlockTitle(title: string | undefined) {
  const pending = pendingBlockCreation.value
  pendingBlockCreation.value = null
  if (pending) {
    await createBlock(pending.contentBlockId, title)
  }
}
</script>
