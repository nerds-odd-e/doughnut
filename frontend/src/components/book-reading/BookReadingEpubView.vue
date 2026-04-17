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
    :disposition-for-block="noDisposition"
    @block-click="onBookBlockClick"
  >
    <main
      class="daisy-flex daisy-flex-1 daisy-min-h-0 daisy-min-w-0 daisy-flex-col"
    >
      <EpubBookViewer
        ref="epubViewerRef"
        :epub-bytes="epubBytes"
        :book="book"
        :initial-locator="initialEpubLocator"
        @relocated="onEpubRelocated"
      />
    </main>
  </BookReadingBookLayout>
</template>

<script setup lang="ts">
import BookLayoutToggleButton from "@/components/book-reading/BookLayoutToggleButton.vue"
import BookReadingBookLayout from "@/components/book-reading/BookReadingBookLayout.vue"
import EpubBookViewer from "@/components/book-reading/EpubBookViewer.vue"
import GlobalBar from "@/components/toolbars/GlobalBar.vue"
import { createCurrentBlockIdDebouncer } from "@/lib/book-reading/debounceCurrentBlockId"
import { currentBlockIdFromEpubLocation } from "@/lib/book-reading/currentBlockIdFromEpubLocation"
import type { BookBlockFull, BookFull } from "@generated/doughnut-backend-api"
import { computed, onBeforeUnmount, onMounted, ref } from "vue"

type EpubViewerExposed = {
  displayEpubTarget: (href: string) => Promise<void>
}

const BOOK_READING_LAYOUT_BREAKPOINT_PX = 768
const CURRENT_BLOCK_ID_DEBOUNCE_MS = 120
const bookReadingBookLayoutPanelId = "book-reading-book-layout-panel"

const props = withDefaults(
  defineProps<{
    book: BookFull
    epubBytes: ArrayBuffer
    initialEpubLocator?: string | null
  }>(),
  { initialEpubLocator: null }
)

const notebookId = computed(() => Number(props.book.notebookId))

const epubViewerRef = ref<EpubViewerExposed | null>(null)

const currentBlockIdDebouncer = createCurrentBlockIdDebouncer({
  delayMs: CURRENT_BLOCK_ID_DEBOUNCE_MS,
  commit: () => true,
})
const { currentBlockId } = currentBlockIdDebouncer

const selectedBlockId = ref<number | null>(null)

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

const noDisposition = () => undefined

function onEpubRelocated(payload: { href: string }) {
  const id = currentBlockIdFromEpubLocation(props.book.blocks, payload.href)
  if (id !== null) {
    currentBlockIdDebouncer.propose(id)
  }
}

async function onBookBlockClick(block: BookBlockFull) {
  selectedBlockId.value = block.id
  const href = block.epubStartHref
  const viewer = epubViewerRef.value
  if (href && viewer) {
    await viewer.displayEpubTarget(href)
  }
  currentBlockIdDebouncer.commitNow(block.id)
}

onMounted(() => {
  window.addEventListener("resize", handleResize)
  if (windowWidth.value >= BOOK_READING_LAYOUT_BREAKPOINT_PX) {
    bookLayoutOpened.value = true
  }
})

onBeforeUnmount(() => {
  window.removeEventListener("resize", handleResize)
  currentBlockIdDebouncer.cancel()
})
</script>
