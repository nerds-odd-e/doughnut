<template>
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
      data-testid="book-reading-epub-global-bar-title"
      :title="book.bookName"
    >
      {{ book.bookName }}
    </span>
    <span class="ml-auto shrink-0" aria-hidden="true" />
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
    @change-mark="bookReading.submitReadingDisposition"
    @clear-mark="bookReading.clearReadingDisposition"
  >
    <main
      ref="epubMainPaneRef"
      class="flex flex-1 min-h-0 min-w-0 flex-col relative"
    >
      <EpubBookViewer
        ref="epubViewerRef"
        :epub-bytes="epubBytes"
        :book="book"
        :initial-locator="initialLocatorDisplayHref"
        @relocated="onEpubRelocated"
      />
      <ReadingOverlayDock>
        <ReadingControlPanel
          v-if="blockAwaitingConfirmation"
          :selected-block-title="blockAwaitingConfirmation.title"
          :anchor-top-px="readingPanelAnchorTopPx"
          @mark-as-read="() => markSelectedBlockDisposition('READ')"
          @mark-as-skimmed="() => markSelectedBlockDisposition('SKIMMED')"
          @mark-as-skipped="() => markSelectedBlockDisposition('SKIPPED')"
        />
      </ReadingOverlayDock>
    </main>
  </BookReadingBookLayout>
</template>

<script setup lang="ts">
import BookLayoutToggleButton from "@/components/book-reading/BookLayoutToggleButton.vue"
import BookReadingBookLayout from "@/components/book-reading/BookReadingBookLayout.vue"
import EpubBookViewer from "@/components/book-reading/EpubBookViewer.vue"
import GlobalBar from "@/components/toolbars/GlobalBar.vue"
import ReadingControlPanel from "@/components/book-reading/ReadingControlPanel.vue"
import ReadingOverlayDock from "@/components/book-reading/ReadingOverlayDock.vue"
import type { BookReaderViewerRef } from "@/composables/bookReaderViewerRef"
import { useBookReadingCurrentBlock } from "@/composables/useBookReadingCurrentBlock"
import { useSidebarDrawer } from "@/composables/useSidebarDrawer"
import { useReadingPanelAnchor } from "@/composables/useReadingPanelAnchor"
import { useBookReadingSelection } from "@/composables/useBookReadingSelection"
import { useNotebookBookReadingRecords } from "@/composables/useNotebookBookReadingRecords"
import {
  asEpubLocator,
  epubDisplayHref,
} from "@/lib/book-reading/asEpubLocator"
import {
  currentBlockIdFromEpubView,
  type EpubViewBlockStarts,
} from "@/lib/book-reading/currentBlockIdFromEpubView"
import type {
  BookBlockFull,
  BookFull,
  ContentLocatorFull,
} from "@generated/donut-backend-api"
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue"

type EpubViewerExposed = Pick<
  BookReaderViewerRef,
  | "displayLocator"
  | "resolveLocatorRect"
  | "isLocatorBottomVisible"
  | "readingPanelAnchorTopPx"
> & { viewBlockStarts: () => EpubViewBlockStarts | null }

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
const { currentBlockId, currentBlockIdDebouncer, proposeReadingPosition } =
  useBookReadingCurrentBlock({
    notebookId,
    flushLastReadPositionPatchOnUnmount: true,
    proposeReadingPosition: (debouncer) => () => {
      const current = props.book.blocks.find(
        (b) => b.id === currentBlockId.value
      )
      const locator = asEpubLocator(current?.contentLocators[0])
      if (!locator) return
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
    currentBlockIdDebouncer.commitNow(currentBlockIdInView())
  },
  afterAdvance: async () => {
    await nextTick()
    refreshReadingPanelAnchorAfterSelection.value?.()
  },
})

function currentBlockIdInView(): number | null {
  const view = epubViewerRef.value?.viewBlockStarts()
  if (!view) {
    return null
  }
  return currentBlockIdFromEpubView(
    props.book.blocks,
    view,
    selectedBlockId.value
  )
}

/**
 * Decided synchronously so the book layout aside is in its final open/closed state before
 * `EpubBookViewer` mounts; otherwise a late resize can redisplay the wrong section.
 */
const { opened: bookLayoutOpened, isMdOrLarger } = useSidebarDrawer()

const { readingPanelAnchorTopPx, updateReadingPanelAnchor } =
  useReadingPanelAnchor({
    viewerRef: epubViewerRef,
    blockRef: blockAwaitingConfirmation,
    mainPaneRef: epubMainPaneRef,
  })
refreshReadingPanelAnchorAfterSelection.value = updateReadingPanelAnchor

function onEpubRelocated() {
  const id = currentBlockIdInView()
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
  window.addEventListener("resize", updateReadingPanelAnchor)
  await bookReading.syncFromServer()
  await nextTick()
  updateReadingPanelAnchor()
})

onBeforeUnmount(() => {
  window.removeEventListener("resize", updateReadingPanelAnchor)
})
</script>
