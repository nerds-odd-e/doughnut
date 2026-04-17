<template>
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
      data-testid="book-reading-epub-global-bar-title"
      :title="book.bookName"
    >
      {{ book.bookName }}
    </span>
    <span class="daisy-ml-auto daisy-shrink-0" aria-hidden="true" />
  </GlobalBar>
  <BookReadingBookLayout
    v-model:opened="bookLayoutOpened"
    :panel-id="bookReadingBookLayoutPanelId"
    :is-md-or-larger="isMdOrLarger"
    :blocks="book.blocks"
    :current-block-id="currentBlockId"
    :selected-block-id="selectedBlockId"
    :disposition-for-block="bookReading.dispositionForBlock"
    @block-click="onBookBlockClick"
  >
    <main
      ref="epubMainPaneRef"
      class="daisy-flex daisy-flex-1 daisy-min-h-0 daisy-min-w-0 daisy-flex-col daisy-relative"
    >
      <EpubBookViewer
        ref="epubViewerRef"
        :epub-bytes="epubBytes"
        :book="book"
        :initial-locator="initialEpubLocator"
        @relocated="onEpubRelocated"
      />
      <ReadingControlPanel
        v-if="blockAwaitingConfirmation"
        :selected-block-title="blockAwaitingConfirmation.title"
        :anchor-top-px="readingPanelAnchorTopPx"
        @mark-as-read="() => markSelectedBlockDisposition('READ')"
        @mark-as-skimmed="() => markSelectedBlockDisposition('SKIMMED')"
        @mark-as-skipped="() => markSelectedBlockDisposition('SKIPPED')"
      />
    </main>
  </BookReadingBookLayout>
</template>

<script setup lang="ts">
import BookLayoutToggleButton from "@/components/book-reading/BookLayoutToggleButton.vue"
import BookReadingBookLayout from "@/components/book-reading/BookReadingBookLayout.vue"
import EpubBookViewer from "@/components/book-reading/EpubBookViewer.vue"
import GlobalBar from "@/components/toolbars/GlobalBar.vue"
import ReadingControlPanel from "@/components/book-reading/ReadingControlPanel.vue"
import type { BookReaderViewerRef } from "@/composables/bookReaderViewerRef"
import { useReadingPanelAnchor } from "@/composables/useReadingPanelAnchor"
import { useBookReadingSelection } from "@/composables/useBookReadingSelection"
import { useNotebookBookReadingRecords } from "@/composables/useNotebookBookReadingRecords"
import { asEpubLocator } from "@/lib/book-reading/asEpubLocator"
import { createCurrentBlockIdDebouncer } from "@/lib/book-reading/debounceCurrentBlockId"
import { createLastReadPositionPatchDebouncer } from "@/lib/book-reading/debounceLastReadPositionPatch"
import { currentBlockIdFromEpubLocation } from "@/lib/book-reading/currentBlockIdFromEpubLocation"
import type { BookBlockFull, BookFull } from "@generated/doughnut-backend-api"
import { NotebookBooksController } from "@generated/doughnut-backend-api/sdk.gen"
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue"

type EpubViewerExposed = Pick<
  BookReaderViewerRef,
  | "displayLocator"
  | "resolveLocatorRect"
  | "isLocatorBottomVisible"
  | "readingPanelAnchorTopPx"
>

const BOOK_READING_LAYOUT_BREAKPOINT_PX = 768
const CURRENT_BLOCK_ID_DEBOUNCE_MS = 120
const LAST_READ_POSITION_PATCH_DEBOUNCE_MS = 400
const bookReadingBookLayoutPanelId = "book-reading-book-layout-panel"

const props = withDefaults(
  defineProps<{
    book: BookFull
    epubBytes: ArrayBuffer
    initialEpubLocator?: string | null
    initialSelectedBlockId?: number | null
  }>(),
  { initialEpubLocator: null, initialSelectedBlockId: null }
)

const notebookId = computed(() => Number(props.book.notebookId))
const bookReading = useNotebookBookReadingRecords(notebookId)

const epubViewerRef = ref<EpubViewerExposed | null>(null)
const epubMainPaneRef = ref<HTMLElement | null>(null)

const currentBlockIdDebouncer = createCurrentBlockIdDebouncer({
  delayMs: CURRENT_BLOCK_ID_DEBOUNCE_MS,
  commit: () => true,
})
const { currentBlockId } = currentBlockIdDebouncer

const bookBlocks = computed(() => props.book.blocks)

const refreshReadingPanelAnchorAfterSelection = ref<(() => void) | null>(null)

const {
  selectedBlockId,
  blockAwaitingConfirmation,
  applyBookBlockSelection,
  markSelectedBlockDisposition,
} = useBookReadingSelection({
  bookBlocks,
  currentBlockId,
  hasRecordedDisposition: bookReading.hasRecordedDisposition,
  submitReadingDisposition: bookReading.submitReadingDisposition,
  initialSelectedBlockId: props.initialSelectedBlockId ?? null,
  onAdvance: async (block) => {
    selectedBlockId.value = block.id
    const loc = asEpubLocator(block.contentLocators[0])
    if (loc) {
      await epubViewerRef.value?.displayLocator(loc)
    }
    currentBlockIdDebouncer.commitNow(block.id)
  },
  afterAdvance: async () => {
    await nextTick()
    refreshReadingPanelAnchorAfterSelection.value?.()
  },
})

/**
 * When the page restores a saved EPUB position, the tiny continuous-scrolled viewport may
 * already render the target section so epub.js never fires a fresh `relocated` event for it.
 * Seed the current-block debouncer from the saved locator so the layout reflects where we
 * just resumed before any scroll-driven event arrives. Fall back to the saved selection if
 * the href cannot be mapped.
 */
if (props.initialEpubLocator !== null && props.initialEpubLocator.length > 0) {
  const seededId = currentBlockIdFromEpubLocation(
    props.book.blocks,
    props.initialEpubLocator
  )
  if (seededId !== null) {
    currentBlockIdDebouncer.commitNow(seededId)
  } else if (props.initialSelectedBlockId !== null) {
    currentBlockIdDebouncer.commitNow(props.initialSelectedBlockId)
  }
} else if (props.initialSelectedBlockId !== null) {
  currentBlockIdDebouncer.commitNow(props.initialSelectedBlockId)
}

const lastReadPositionPatchDebouncer = createLastReadPositionPatchDebouncer({
  delayMs: LAST_READ_POSITION_PATCH_DEBOUNCE_MS,
  patch: (body) =>
    NotebookBooksController.patchNotebookBookReadingPosition({
      path: { notebook: notebookId.value },
      body,
    }),
})

const bookLayoutOpened = ref(false)
const windowWidth = ref(
  typeof window !== "undefined"
    ? window.innerWidth
    : BOOK_READING_LAYOUT_BREAKPOINT_PX
)

function handleResize() {
  windowWidth.value = window.innerWidth
  updateReadingPanelAnchor()
}

const isMdOrLarger = computed(
  () => windowWidth.value >= BOOK_READING_LAYOUT_BREAKPOINT_PX
)

const { readingPanelAnchorTopPx, updateReadingPanelAnchor } =
  useReadingPanelAnchor({
    viewerRef: epubViewerRef,
    blockRef: blockAwaitingConfirmation,
    mainPaneRef: epubMainPaneRef,
  })
refreshReadingPanelAnchorAfterSelection.value = updateReadingPanelAnchor

function onEpubRelocated(payload: { href: string }) {
  const id = currentBlockIdFromEpubLocation(props.book.blocks, payload.href)
  if (id !== null) {
    currentBlockIdDebouncer.propose(id)
  }
  const sel = selectedBlockId.value
  lastReadPositionPatchDebouncer.proposeEpubLocator(
    payload.href,
    sel === null ? undefined : sel
  )
  updateReadingPanelAnchor()
}

watch(selectedBlockId, () => {
  readingPanelAnchorTopPx.value = null
})

async function onBookBlockClick(block: BookBlockFull) {
  await applyBookBlockSelection(block)
}

onMounted(async () => {
  window.addEventListener("resize", handleResize)
  if (windowWidth.value >= BOOK_READING_LAYOUT_BREAKPOINT_PX) {
    bookLayoutOpened.value = true
  }
  await bookReading.syncFromServer()
  await nextTick()
  updateReadingPanelAnchor()
})

onBeforeUnmount(() => {
  window.removeEventListener("resize", handleResize)
  currentBlockIdDebouncer.cancel()
  lastReadPositionPatchDebouncer.flush()
})
</script>
