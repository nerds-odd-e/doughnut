<template>
  <BookReadingShell
    :session="session"
    format="epub"
    :book-name="book.bookName"
  >
    <EpubBookViewer
      ref="epubViewerRef"
      :epub-bytes="epubBytes"
      :book="book"
      :initial-locator="initialLocator"
      @relocated="onEpubRelocated"
    />
  </BookReadingShell>
</template>

<script setup lang="ts">
import BookReadingShell from "@/components/book-reading/BookReadingShell.vue"
import EpubBookViewer from "@/components/book-reading/EpubBookViewer.vue"
import type { BookReaderViewerRef } from "@/composables/bookReaderViewerRef"
import type { EpubViewBlockStarts } from "@/composables/book-reading/useEpubLocatorGeometry"
import { useBookReadingSession } from "@/composables/useBookReadingSession"
import { asEpubLocator } from "@/lib/book-reading/asEpubLocator"
import { currentBlockIdFromViewStarts } from "@/lib/book-reading/currentBlockIdFromViewStarts"
import type {
  BookBlockFull,
  BookFull,
  ContentLocatorFull,
  EpubLocatorFull,
} from "@generated/donut-backend-api"
import { onBeforeUnmount, onMounted, ref } from "vue"

type EpubViewerExposed = Pick<
  BookReaderViewerRef,
  | "displayLocator"
  | "resolveLocatorRect"
  | "isLocatorBottomVisible"
  | "readingPanelAnchorTopPx"
> & {
  viewBlockStarts: () => EpubViewBlockStarts | null
  currentCfi: () => string | undefined
}

const props = withDefaults(
  defineProps<{
    book: BookFull
    epubBytes: ArrayBuffer
    initialLocator?: ContentLocatorFull | null
    initialSelectedBlockId?: number | null
  }>(),
  { initialLocator: null, initialSelectedBlockId: null }
)

const epubViewerRef = ref<EpubViewerExposed | null>(null)

const session = useBookReadingSession({
  book: () => props.book,
  initialSelectedBlockId: props.initialSelectedBlockId ?? null,
  surface: {
    showBlock,
    readingPositionLocator,
    viewer: epubViewerRef,
    reanchorPanelAfterSyncAndShow: true,
    flushPositionOnLeave: true,
  },
})
const {
  selectedBlockId,
  currentBlockId,
  currentBlockIdDebouncer,
  proposeReadingPosition,
  updateReadingPanelAnchor,
} = session

async function showBlock(block: BookBlockFull) {
  selectedBlockId.value = block.id
  const loc = asEpubLocator(block.contentLocators[0])
  if (loc) {
    await epubViewerRef.value?.displayLocator(loc)
  }
  currentBlockIdDebouncer.commitNow(currentBlockIdInView())
}

function readingPositionLocator(): EpubLocatorFull | null {
  const current = props.book.blocks.find((b) => b.id === currentBlockId.value)
  const blockStart = asEpubLocator(current?.contentLocators[0])
  if (!blockStart) return null
  const cfi = epubViewerRef.value?.currentCfi()
  return cfi ? { ...blockStart, cfi } : blockStart
}

function currentBlockIdInView(): number | null {
  const view = epubViewerRef.value?.viewBlockStarts()
  if (!view) {
    return null
  }
  return currentBlockIdFromViewStarts(
    props.book.blocks,
    {
      startTopPx: (block) => {
        const start = asEpubLocator(block.contentLocators[0])
        return start ? view.startTopPx(start) : null
      },
      landingLimitPx: view.landingLimitPx,
    },
    selectedBlockId.value
  )
}

function onEpubRelocated() {
  const id = currentBlockIdInView()
  if (id !== null) {
    currentBlockIdDebouncer.propose(id)
  }
  proposeReadingPosition()
  updateReadingPanelAnchor()
}

onMounted(() => {
  window.addEventListener("resize", updateReadingPanelAnchor)
})

onBeforeUnmount(() => {
  window.removeEventListener("resize", updateReadingPanelAnchor)
})
</script>
