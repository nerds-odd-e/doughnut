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
      class="daisy-flex daisy-flex-1 daisy-min-h-0 daisy-min-w-0 daisy-flex-col"
    >
      <div
        v-if="pdfViewerLoadError"
        class="daisy-alert daisy-alert-error daisy-mb-2 daisy-mx-2 daisy-mt-2"
        data-testid="book-reading-pdf-viewer-load-error"
      >
        {{ pdfViewerLoadError }}
      </div>
      <div
        v-else
        class="daisy-flex daisy-min-h-0 daisy-min-w-0 daisy-flex-1 daisy-flex-col"
      >
        <div
          ref="pdfPaneRef"
          class="daisy-relative daisy-min-h-0 daisy-min-w-0 daisy-flex-1"
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
  <dialog
    class="daisy-modal"
    :class="{ 'daisy-modal-open': pendingBlockCreation !== null }"
    data-testid="new-block-title-dialog"
  >
    <div class="daisy-modal-box">
      <h2 class="daisy-text-lg daisy-font-semibold">Name the new block</h2>
      <input
        v-if="pendingBlockCreation !== null"
        v-model="pendingBlockTitleInput"
        data-testid="new-block-title-input"
        class="daisy-input daisy-input-bordered daisy-w-full daisy-mt-2"
        type="text"
      />
      <div class="daisy-modal-action">
        <button
          type="button"
          class="daisy-btn daisy-btn-primary"
          data-testid="new-block-title-confirm"
          @click="onConfirmBlockTitle"
        >
          Confirm
        </button>
        <button
          type="button"
          class="daisy-btn"
          @click="pendingBlockCreation = null"
        >
          Cancel
        </button>
      </div>
    </div>
    <form method="dialog" class="daisy-modal-backdrop">
      <button type="button" @click="pendingBlockCreation = null">close</button>
    </form>
  </dialog>
  <dialog
    class="daisy-modal"
    :class="{ 'daisy-modal-open': aiSuggestion !== null }"
    aria-labelledby="book-layout-reorganize-preview-title"
    data-testid="book-layout-reorganize-preview-dialog"
  >
    <div class="daisy-modal-box">
      <h2
        id="book-layout-reorganize-preview-title"
        class="daisy-text-lg daisy-font-semibold"
      >
        Reorganize layout (preview)
      </h2>
      <div
        class="daisy-max-h-[min(24rem,50vh)] daisy-overflow-y-auto daisy-py-2"
      >
        <div
          v-for="row in aiPreviewRows"
          :key="row.block.id"
          data-testid="book-layout-reorganize-preview-row"
          class="daisy-rounded daisy-py-1.5 daisy-pr-2 daisy-text-sm daisy-leading-snug"
          :class="{
            'daisy-bg-warning/15': row.depthChanged,
          }"
          :data-suggested-depth="row.suggestedDepth"
          :data-depth-changed="row.depthChanged ? 'true' : undefined"
          :style="{
            paddingInlineStart: `${0.75 * row.suggestedDepth}rem`,
          }"
        >
          {{ row.block.title }}
        </div>
      </div>
      <div class="daisy-modal-action">
        <button
          type="button"
          class="daisy-btn daisy-btn-primary"
          data-testid="book-layout-reorganize-preview-confirm"
          @click="onConfirmAiReorganize"
        >
          Confirm
        </button>
        <button
          type="button"
          class="daisy-btn"
          data-testid="book-layout-reorganize-preview-cancel"
          @click="dismissAiReorganizePreview"
        >
          Cancel
        </button>
      </div>
    </div>
    <form method="dialog" class="daisy-modal-backdrop">
      <button type="button" @click="dismissAiReorganizePreview">
        close
      </button>
    </form>
  </dialog>
</template>

<script setup lang="ts">
import BookLayoutToggleButton from "@/components/book-reading/BookLayoutToggleButton.vue"
import BookReadingBookLayout from "@/components/book-reading/BookReadingBookLayout.vue"
import CurrentBlockNavigationBar from "@/components/book-reading/CurrentBlockNavigationBar.vue"
import GlobalBar from "@/components/toolbars/GlobalBar.vue"
import PdfBookViewer from "@/components/book-reading/PdfBookViewer.vue"
import PdfControl from "@/components/book-reading/PdfControl.vue"
import ReadingControlPanel from "@/components/book-reading/ReadingControlPanel.vue"
import { pdfLocatorsFromBlock } from "@/lib/book-reading/asPdfLocator"
import { wireItemsToNavigationTargets } from "@/lib/book-reading/pdfOutlineV1Anchor"
import { structuralTitleForBlockId } from "@/lib/book-reading/currentBlockLiveAnnouncement"
import { currentBlockIdFromVisiblePage } from "@/lib/book-reading/currentBlockIdFromVisiblePage"
import { predecessorBookBlockIdInPreorder } from "@/lib/book-reading/predecessorBookBlockIdInPreorder"
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
import type {
  BookBlockFull,
  BookFull,
  BookMutationResponseFull,
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

function bookFullAfterLayoutMutation(
  previous: BookFull,
  mutation: BookMutationResponseFull
): BookFull {
  const prevById = new Map(previous.blocks.map((b) => [b.id, b]))
  const updatedBlocks = mutation.blocks.map((row) => {
    const prev = prevById.get(row.id)
    if (!prev) {
      throw new Error(`book layout mutation: unknown block id ${row.id}`)
    }
    return {
      ...prev,
      id: row.id,
      depth: row.depth,
      title: row.title,
      contentLocators: row.contentLocators ?? prev.contentLocators,
    }
  })
  return { ...previous, ...mutation, blocks: updatedBlocks }
}

const bookReadingBookLayoutPanelId = "book-reading-book-layout-panel"
const BOOK_READING_LAYOUT_BREAKPOINT_PX = 768
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

const selectedBlockId = ref<number | null>(props.initialSelectedBlockId ?? null)

const pendingLayoutBlockId = ref<number | null>(null)
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
 *   PdfBookViewer emits `viewportAnchorPage` (via `pdfViewerViewportTopYDown`)
 *   → here we call `currentBlockIdFromVisiblePage` to map anchor page + viewport Y-range to a block ID
 *   → result is debounced through `currentBlockIdDebouncer`.
 *
 * @see currentBlockIdFromVisiblePage — midpoint rule: block whose `y0` is above viewport mid wins;
 *      otherwise the previous block is returned (scrolling a page into view is not enough when `y0 > 0`).
 * @see pdfViewerViewportTopYDown — produces the `ViewportYRange` consumed here.
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

async function onBlockIndent(block: BookBlockFull) {
  if (pendingLayoutBlockId.value !== null) {
    return
  }
  pendingLayoutBlockId.value = block.id
  try {
    const { data, error } = await apiCallWithLoading(() =>
      NotebookBooksController.changeBookBlockDepth({
        path: { notebook: notebookId.value, bookBlock: block.id },
        body: { direction: "INDENT" },
      })
    )
    if (!error && data) {
      emit("update:book", bookFullAfterLayoutMutation(props.book, data))
      selectedBlockId.value = block.id
    }
  } finally {
    pendingLayoutBlockId.value = null
  }
}

async function onBlockOutdent(block: BookBlockFull) {
  if (pendingLayoutBlockId.value !== null) {
    return
  }
  pendingLayoutBlockId.value = block.id
  try {
    const { data, error } = await apiCallWithLoading(() =>
      NotebookBooksController.changeBookBlockDepth({
        path: { notebook: notebookId.value, bookBlock: block.id },
        body: { direction: "OUTDENT" },
      })
    )
    if (!error && data) {
      emit("update:book", bookFullAfterLayoutMutation(props.book, data))
      selectedBlockId.value = block.id
    }
  } finally {
    pendingLayoutBlockId.value = null
  }
}

async function onBlockCancel(block: BookBlockFull) {
  if (pendingLayoutBlockId.value !== null) {
    return
  }
  pendingLayoutBlockId.value = block.id
  try {
    const predecessorId = predecessorBookBlockIdInPreorder(
      bookBlocks.value,
      block.id
    )
    const { data, error } = await NotebookBooksController.cancelBookBlock({
      path: { notebook: notebookId.value, bookBlock: block.id },
    })
    if (!error && data) {
      const merged = bookFullAfterLayoutMutation(props.book, data)
      if (
        predecessorId !== null &&
        merged.blocks.some((b) => b.id === predecessorId)
      ) {
        selectedBlockId.value = predecessorId
        emit("update:book", merged)
        const pred = merged.blocks.find((b) => b.id === predecessorId)!
        await applyBookBlockSelection(pred)
      } else {
        selectedBlockId.value = null
        emit("update:book", merged)
      }
    }
  } finally {
    pendingLayoutBlockId.value = null
  }
}

async function onConfirmAiReorganize() {
  const mutation = await confirmAiReorganize()
  if (mutation) {
    emit("update:book", bookFullAfterLayoutMutation(props.book, mutation))
  }
}

const STRUCTURAL_TITLE_MAX_CHARS = 512

const pendingBlockCreation = ref<{
  contentBlockId: number
  structuralTitle: string
} | null>(null)
const pendingBlockTitleInput = ref("")

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
    pendingBlockTitleInput.value = derivedTitle
    pendingBlockCreation.value = {
      contentBlockId,
      structuralTitle: derivedTitle,
    }
  } else {
    createBlock(contentBlockId)
  }
}

async function onConfirmBlockTitle() {
  const pending = pendingBlockCreation.value
  pendingBlockCreation.value = null
  if (pending) {
    await createBlock(
      pending.contentBlockId,
      pendingBlockTitleInput.value || undefined
    )
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
})
</script>
