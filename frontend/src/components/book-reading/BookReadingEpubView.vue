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
        :initial-locator="initialLocatorDisplayHref"
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
import { useBookReadingCurrentBlock } from "@/composables/useBookReadingCurrentBlock"
import { useReadingPanelAnchor } from "@/composables/useReadingPanelAnchor"
import { useBookReadingSelection } from "@/composables/useBookReadingSelection"
import { useNotebookBookReadingRecords } from "@/composables/useNotebookBookReadingRecords"
import {
  asEpubLocator,
  epubDisplayHref,
} from "@/lib/book-reading/asEpubLocator"
import {
  BOOK_READING_LAYOUT_BREAKPOINT_PX,
  bookLayoutAsideInitiallyOpen,
} from "@/lib/book-reading/bookReadingLayoutBreakpoint"
import { currentBlockIdFromEpubLocation } from "@/lib/book-reading/currentBlockIdFromEpubLocation"
import { splitEpubHref } from "@/lib/book-reading/epubHrefMatch"
import type {
  BookBlockFull,
  BookFull,
  ContentLocatorFull,
  EpubLocatorFull,
} from "@generated/doughnut-backend-api"
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue"

type EpubViewerExposed = Pick<
  BookReaderViewerRef,
  | "displayLocator"
  | "resolveLocatorRect"
  | "isLocatorBottomVisible"
  | "readingPanelAnchorTopPx"
>

const bookReadingBookLayoutPanelId = "book-reading-book-layout-panel"

const props = withDefaults(
  defineProps<{
    book: BookFull
    epubBytes: ArrayBuffer
    initialLocator?: ContentLocatorFull | null
    initialSelectedBlockId?: number | null
  }>(),
  { initialLocator: null, initialSelectedBlockId: null }
)

const initialLocatorDisplayHref = computed(() => {
  const epub = asEpubLocator(props.initialLocator ?? undefined)
  if (!epub) {
    return null
  }
  const s = epubDisplayHref(epub)
  return s.length > 0 ? s : null
})

const notebookId = computed(() => Number(props.book.notebookId))
const bookReading = useNotebookBookReadingRecords(notebookId)

const epubViewerRef = ref<EpubViewerExposed | null>(null)
const epubMainPaneRef = ref<HTMLElement | null>(null)

const bookBlocks = computed(() => props.book.blocks)

const selectedBlockId = ref<number | null>(props.initialSelectedBlockId ?? null)
const lastRelocateHref = ref<string | null>(null)

const { currentBlockId, currentBlockIdDebouncer, proposeReadingPosition } =
  useBookReadingCurrentBlock({
    notebookId,
    commitCurrentBlock: () => true,
    flushLastReadPositionPatchOnUnmount: true,
    proposeReadingPosition: (debouncer) => () => {
      const raw = lastRelocateHref.value
      if (raw === null || raw.length === 0) return
      const { path, fragment } = splitEpubHref(raw.trim())
      if (path.length === 0) return
      const locator: EpubLocatorFull = {
        type: "EpubLocator_Full",
        href: path,
        ...(fragment !== null ? { fragment } : {}),
      }
      const sel = selectedBlockId.value
      debouncer.propose(locator, sel === null ? undefined : sel)
    },
  })

const refreshReadingPanelAnchorAfterSelection = ref<(() => void) | null>(null)

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
const seedHref = initialLocatorDisplayHref.value
if (seedHref !== null && seedHref.length > 0) {
  const seededId = currentBlockIdFromEpubLocation(props.book.blocks, seedHref)
  if (seededId !== null) {
    currentBlockIdDebouncer.commitNow(seededId)
  } else if (props.initialSelectedBlockId !== null) {
    currentBlockIdDebouncer.commitNow(props.initialSelectedBlockId)
  }
} else if (props.initialSelectedBlockId !== null) {
  currentBlockIdDebouncer.commitNow(props.initialSelectedBlockId)
}

const windowWidth = ref(
  typeof window !== "undefined"
    ? window.innerWidth
    : BOOK_READING_LAYOUT_BREAKPOINT_PX
)
/**
 * Initialize synchronously so the book layout aside is rendered in its final open/closed
 * state before `EpubBookViewer` mounts. Otherwise epub.js sizes the rendition to the full
 * main width, then the aside appears and our resize observer triggers a `clear() + redisplay`
 * which can land on the wrong section for beginning-of-book saved positions.
 */
const bookLayoutOpened = ref(bookLayoutAsideInitiallyOpen(windowWidth.value))

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
  lastRelocateHref.value = payload.href
  const id = currentBlockIdFromEpubLocation(props.book.blocks, payload.href)
  if (id !== null) {
    currentBlockIdDebouncer.propose(id)
  }
  proposeReadingPosition()
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
  await bookReading.syncFromServer()
  await nextTick()
  updateReadingPanelAnchor()
})

onBeforeUnmount(() => {
  window.removeEventListener("resize", handleResize)
})
</script>
