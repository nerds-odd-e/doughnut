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
    <button
      type="button"
      class="daisy-btn daisy-btn-sm daisy-btn-ghost daisy-shrink-0"
      :class="{ 'sidebar-expanded': bookLayoutOpened }"
      data-testid="book-reading-book-layout-toggle"
      aria-label="Book layout"
      title="Book layout"
      :aria-expanded="bookLayoutOpened"
      aria-controls="book-reading-book-layout-panel"
      @click="bookLayoutOpened = !bookLayoutOpened"
    >
      <div class="daisy-w-4 daisy-h-4">
        <svg
          xmlns="http://www.w3.org/2000/svg"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
          class="w-4 h-4"
        >
          <template v-if="bookLayoutOpened">
            <path d="M19 12H5M12 19l-7-7 7-7" />
          </template>
          <template v-else>
            <line x1="3" y1="6" x2="21" y2="6" />
            <line x1="6" y1="12" x2="21" y2="12" />
            <line x1="3" y1="18" x2="21" y2="18" />
          </template>
        </svg>
      </div>
    </button>
    <router-link
      :to="{ name: 'notebookEdit', params: { notebookId: props.notebookId } }"
      class="daisy-btn daisy-btn-sm daisy-btn-ghost daisy-shrink-0 daisy-no-underline"
    >
      Notebook
    </router-link>
    <span
      class="daisy-truncate daisy-text-sm daisy-font-medium daisy-min-w-0 daisy-ml-1"
      :title="book.bookName ?? undefined"
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
  <div
    v-if="!isMdOrLarger && bookLayoutOpened"
    class="daisy-fixed daisy-inset-0 daisy-bg-black/50 daisy-z-30"
    aria-hidden="true"
    @click="bookLayoutOpened = false"
  />
  <div class="daisy-flex daisy-flex-1 daisy-min-h-0 daisy-relative">
    <aside
      id="book-reading-book-layout-panel"
      ref="bookLayoutAsideRef"
      data-testid="book-reading-book-layout-aside"
      :class="[
        'daisy-bg-base-200 daisy-w-72 daisy-min-w-[16rem] daisy-max-w-[min(20rem,85vw)] daisy-transition-transform daisy-ease-in-out daisy-duration-200 daisy-overflow-y-auto daisy-overflow-x-hidden',
        isMdOrLarger
          ? bookLayoutOpened
            ? 'daisy-relative daisy-shrink-0 daisy-border-r daisy-border-base-300'
            : 'daisy-hidden'
          : bookLayoutOpened
            ? 'daisy-translate-x-0 daisy-fixed daisy-top-0 daisy-left-0 daisy-z-40 daisy-h-full daisy-pt-[env(safe-area-inset-top)]'
            : '-daisy-translate-x-full daisy-fixed daisy-top-0 daisy-left-0 daisy-z-40 daisy-h-full',
      ]"
    >
      <div
        data-testid="book-reading-book-layout"
        class="daisy-p-3 daisy-pb-8"
      >
        <button
          v-for="block in bookBlockRows"
          :key="block.id"
          type="button"
          data-testid="book-reading-book-block"
          class="book-reading-book-block"
          :data-book-block-depth="block.depth"
          :data-current-block="
            block.startAnchor.id === currentBlockAnchorId
              ? 'true'
              : undefined
          "
          :data-current-selection="
            block.id === selectedBlockId ? 'true' : undefined
          "
          :data-direct-content-read="
            bookReading.dispositionForBlock(block.id) === 'READ'
              ? 'true'
              : undefined
          "
          :data-direct-content-skimmed="
            bookReading.dispositionForBlock(block.id) === 'SKIMMED'
              ? 'true'
              : undefined
          "
          :data-direct-content-skipped="
            bookReading.dispositionForBlock(block.id) === 'SKIPPED'
              ? 'true'
              : undefined
          "
          :aria-current="
            block.startAnchor.id === currentBlockAnchorId
              ? 'location'
              : undefined
          "
          :style="{ paddingLeft: `${block.depth * 0.75}rem` }"
          @click="onBookBlockClick(block)"
        >
          {{ block.title }}
          <span
            v-if="bookReading.dispositionForBlock(block.id) === 'READ'"
            class="daisy-sr-only"
          >
            Marked as read
          </span>
          <span
            v-else-if="bookReading.dispositionForBlock(block.id) === 'SKIMMED'"
            class="daisy-sr-only"
          >
            Marked as skimmed
          </span>
          <span
            v-else-if="bookReading.dispositionForBlock(block.id) === 'SKIPPED'"
            class="daisy-sr-only"
          >
            Marked as skipped
          </span>
        </button>
      </div>
    </aside>
    <main class="daisy-flex-1 daisy-min-h-0 daisy-min-w-0 daisy-relative">
      <div
        v-if="pdfLoading"
        class="daisy-px-2 daisy-py-2 sm:daisy-px-4"
      >
        <ContentLoader />
      </div>
      <div
        v-else-if="pdfError"
        class="daisy-alert daisy-alert-error daisy-mb-2 daisy-mx-2 daisy-mt-2"
        data-testid="book-reading-pdf-error"
      >
        {{ pdfError }}
      </div>
      <template v-else-if="bookPdfBytes">
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
          @mark-as-read="() => markSelectedDisposition('READ')"
          @mark-as-skimmed="() => markSelectedDisposition('SKIMMED')"
          @mark-as-skipped="() => markSelectedDisposition('SKIPPED')"
        />
      </template>
    </main>
  </div>
</template>

<script setup lang="ts">
import ContentLoader from "@/components/commons/ContentLoader.vue"
import GlobalBar from "@/components/toolbars/GlobalBar.vue"
import PdfBookViewer from "@/components/book-reading/PdfBookViewer.vue"
import PdfControl from "@/components/book-reading/PdfControl.vue"
import ReadingControlPanel from "@/components/book-reading/ReadingControlPanel.vue"
import {
  wireItemsToNavigationTargets,
  parsePdfOutlineV1Anchor,
  type PdfOutlineV1NavigationTarget,
} from "@/lib/book-reading/pdfOutlineV1Anchor"
import { createLastReadPositionPatchDebouncer } from "@/lib/book-reading/debounceLastReadPositionPatch"
import { createCurrentBlockAnchorDebouncer } from "@/lib/book-reading/debounceCurrentBlockAnchorId"
import { nextLiveAnnouncementText } from "@/lib/book-reading/currentBlockLiveAnnouncement"
import { currentBlockAnchorIdFromAnchorPage } from "@/lib/book-reading/currentBlockAnchorFromAnchorPage"
import type { ViewportYRange } from "@/lib/book-reading/pdfViewerViewportTopYDown"
import { useBookReadingBlockSelection } from "@/composables/useBookReadingBlockSelection"
import { useNotebookBookReadingRecords } from "@/composables/useNotebookBookReadingRecords"
import type { BookBlockReadingDisposition } from "@/lib/book-reading/readBlockIdsFromRecords"
import type {
  BookAnchorFull,
  BookBlockContentBboxItemFull,
  BookBlockFull,
  BookFull,
} from "@generated/doughnut-backend-api"
import { NotebookBooksController } from "@generated/doughnut-backend-api/sdk.gen"
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue"

const BOOK_READING_LAYOUT_BREAKPOINT_PX = 768
const CURRENT_BLOCK_ANCHOR_DEBOUNCE_MS = 120
const LAST_READ_POSITION_PATCH_DEBOUNCE_MS = 400
/** Vertical space (px) reserved at the bottom of the PDF main pane by ReadingControlPanel. */
const READING_PANEL_OBSTRUCTION_PX = 80

const props = defineProps<{
  book: BookFull
  notebookId: number
}>()

const bookReading = useNotebookBookReadingRecords(() => props.notebookId)

function notebookBookFilePath(notebookId: number) {
  return `/api/notebooks/${notebookId}/book/file`
}

const bookPdfBytes = ref<ArrayBuffer | null>(null)
const pdfLoading = ref(false)
const pdfError = ref<string | null>(null)

const pdfBarCurrentPage = ref<number | null>(null)
const pdfBarPagesTotal = ref<number | null>(null)

function resetPdfPageIndicator() {
  pdfBarCurrentPage.value = null
  pdfBarPagesTotal.value = null
}

const bookLayoutOpened = ref(false)
const bookLayoutAsideRef = ref<HTMLElement | null>(null)
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
  pdfError.value = message
  resetPdfPageIndicator()
}

type BookBlockRow = {
  id: number
  title: string
  depth: number
  startAnchor: BookAnchorFull
  hasDirectContent: boolean
  allBboxes: BookBlockContentBboxItemFull[]
}

function buildFlatBookBlocks(blocks: BookBlockFull[]): BookBlockRow[] {
  const childrenMap = new Map<number | null, BookBlockFull[]>()
  for (const block of blocks) {
    const parentId =
      block.parentBlockId != null ? Number(block.parentBlockId) : null
    const siblings = childrenMap.get(parentId) ?? []
    siblings.push(block)
    childrenMap.set(parentId, siblings)
  }
  const sortByOrder = (a: BookBlockFull, b: BookBlockFull) =>
    (a.siblingOrder ?? 0) - (b.siblingOrder ?? 0)

  const result: BookBlockRow[] = []
  function visit(parentId: number | null, depth: number) {
    const children = (childrenMap.get(parentId) ?? []).slice().sort(sortByOrder)
    for (const child of children) {
      result.push({
        id: child.id,
        title: child.title,
        depth,
        startAnchor: child.startAnchor,
        hasDirectContent: child.hasDirectContent ?? true,
        allBboxes: child.allBboxes ?? [],
      })
      visit(child.id, depth + 1)
    }
  }
  visit(null, 0)
  return result
}

const flatBookBlocks = ref<BookBlockRow[]>([])
const bookBlockRows = computed(() => flatBookBlocks.value)
const currentBlockAnchorId = ref<number | null>(null)

const selectedBlockId = ref<number | null>(null)
const lastPositionInsideSelectedBlock = ref<{
  pageIndexZeroBased: number
  normalizedTop: number
} | null>(null)
const snapBackAttempts = new Map<number, number>()

const lastContentBottomVisible = ref(false)
const geometryEverVisibleForSelection = ref(false)

function selectedBlockHasSuccessorAndNoDisposition(): {
  selId: number
  successor: (typeof bookBlockRows.value)[number]
} | null {
  const selId = selectedBlockId.value
  if (selId === null) return null
  if (bookReading.hasRecordedDisposition(selId)) return null
  const rows = bookBlockRows.value
  const selIdx = rows.findIndex((r) => r.id === selId)
  if (selIdx < 0 || selIdx >= rows.length - 1) return null
  return { selId, successor: rows[selIdx + 1]! }
}

// allBboxes: index 0 is the anchor; remaining entries are direct-content blocks.
// When length > 1, the last entry is the last direct-content bbox.
const blockAwaitingConfirmation = computed<BookBlockRow | null>(() => {
  const context = selectedBlockHasSuccessorAndNoDisposition()
  if (context === null) return null
  const { selId, successor } = context
  const rows = bookBlockRows.value
  const sel = rows.find((r) => r.id === selId)!
  if (!sel.hasDirectContent) return null
  const lastBbox =
    sel.allBboxes.length > 1 ? sel.allBboxes[sel.allBboxes.length - 1]! : null
  if (lastBbox !== null) {
    if (lastContentBottomVisible.value) return sel
    if (geometryEverVisibleForSelection.value) {
      return successor.startAnchor.id === currentBlockAnchorId.value
        ? null
        : sel
    }
    return null
  }
  // Fallback: no usable bbox → use successor-anchor rule
  return successor.startAnchor.id === currentBlockAnchorId.value ? sel : null
})

const currentBlockLiveText = ref("")
const lastAnnouncedCurrentBlockTitle = ref<string | undefined>(undefined)

const currentBlockAnchorDebouncer = createCurrentBlockAnchorDebouncer({
  delayMs: CURRENT_BLOCK_ANCHOR_DEBOUNCE_MS,
  commit: (id) => {
    currentBlockAnchorId.value = id
  },
})

const lastReadPositionPatchDebouncer = createLastReadPositionPatchDebouncer({
  delayMs: LAST_READ_POSITION_PATCH_DEBOUNCE_MS,
  patch: (body) =>
    NotebookBooksController.patchNotebookBookReadingPosition({
      path: { notebook: props.notebookId },
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
  const candidate = currentBlockAnchorIdFromAnchorPage(
    bookBlockRows.value.map((r) => r.startAnchor),
    payload.anchorPageIndexZeroBased,
    payload.viewport,
    payload.pagesCount
  )
  currentBlockAnchorDebouncer.propose(candidate)
  const selIdForGeometry = selectedBlockId.value
  if (selIdForGeometry !== null) {
    const selForGeometry = bookBlockRows.value.find(
      (r) => r.id === selIdForGeometry
    )
    if (selForGeometry?.hasDirectContent) {
      const lastBboxForGeometry =
        selForGeometry.allBboxes.length > 1
          ? selForGeometry.allBboxes[selForGeometry.allBboxes.length - 1]!
          : null
      if (lastBboxForGeometry !== null) {
        const geometryVisible =
          pdfViewerRef.value?.isLastContentBottomVisible(
            {
              pageIndex: lastBboxForGeometry.pageIndex,
              bbox: lastBboxForGeometry.bbox as [
                number,
                number,
                number,
                number,
              ],
            },
            READING_PANEL_OBSTRUCTION_PX
          ) ?? false
        lastContentBottomVisible.value = geometryVisible
        if (geometryVisible) {
          geometryEverVisibleForSelection.value = true
        }
      }
    }
  }
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
    lastReadPositionPatchDebouncer.propose(
      reading.pageIndexZeroBased,
      Math.round(reading.normalizedTop)
    )
  }
  const selId = selectedBlockId.value
  if (selId !== null && reading !== null) {
    const selRow = bookBlockRows.value.find((r) => r.id === selId)
    if (selRow && candidate === selRow.startAnchor.id) {
      lastPositionInsideSelectedBlock.value = reading
    }
  }
}

watch(currentBlockAnchorId, (id) => {
  const { text, changed } = nextLiveAnnouncementText(
    lastAnnouncedCurrentBlockTitle.value,
    id,
    bookBlockRows.value
  )
  if (!changed) {
    return
  }
  lastAnnouncedCurrentBlockTitle.value = text
  currentBlockLiveText.value = text
})

watch(
  currentBlockAnchorId,
  (id) => {
    if (id === null || !bookLayoutOpened.value) {
      return
    }
    requestAnimationFrame(() => {
      if (!bookLayoutOpened.value) {
        return
      }
      const row = bookLayoutAsideRef.value?.querySelector(
        '[data-current-block="true"]'
      )
      row?.scrollIntoView({ block: "nearest", inline: "nearest" })
    })
  },
  { flush: "post" }
)

watch(currentBlockAnchorId, async (anchorId) => {
  if (anchorId === null) return
  const rows = bookBlockRows.value
  const bIdx = rows.findIndex((r) => r.startAnchor.id === anchorId)
  if (bIdx <= 0) return
  const predecessor = rows[bIdx - 1]!
  if (
    !predecessor.hasDirectContent &&
    !bookReading.hasRecordedDisposition(predecessor.id)
  ) {
    await bookReading.submitMarkRead(predecessor.id)
  }
})

watch(selectedBlockId, () => {
  lastContentBottomVisible.value = false
  geometryEverVisibleForSelection.value = false
})

watch(blockAwaitingConfirmation, (block) => {
  if (block === null) return
  const selId = block.id
  const attempts = snapBackAttempts.get(selId) ?? 0
  if (attempts >= 1) return
  const pos = lastPositionInsideSelectedBlock.value
  if (pos === null) return
  snapBackAttempts.set(selId, attempts + 1)
  pdfViewerRef.value?.scrollToStoredReadingPosition(
    pos.pageIndexZeroBased,
    pos.normalizedTop
  )
})

const initialLastRead = ref<{
  pageIndexZeroBased: number
  normalizedY: number
} | null>(null)

const pdfViewerRef = ref<{
  scrollToPdfOutlineV1Target: (
    target: PdfOutlineV1NavigationTarget,
    highlightBboxes?: ReadonlyArray<PdfOutlineV1NavigationTarget>
  ) => Promise<void>
  highlightBlockSelection: (
    highlightBboxes: ReadonlyArray<PdfOutlineV1NavigationTarget>
  ) => void
  scrollToStoredReadingPosition: (
    pageIndexZeroBased: number,
    normalizedY: number
  ) => Promise<void>
  zoomIn: () => void
  zoomOut: () => void
  isLastContentBottomVisible: (
    target: PdfOutlineV1NavigationTarget,
    obstructionPx: number
  ) => boolean
} | null>(null)

async function applyBookBlockSelection(block: BookBlockRow) {
  const parsed = parsePdfOutlineV1Anchor(block.startAnchor)
  if (parsed === null) {
    return
  }
  selectedBlockId.value = block.id
  await pdfViewerRef.value?.scrollToPdfOutlineV1Target(
    parsed,
    wireItemsToNavigationTargets(block.allBboxes)
  )
  currentBlockAnchorDebouncer.commitNow(block.startAnchor.id)
}

useBookReadingBlockSelection({
  bookBlockRows: () => bookBlockRows.value,
  currentBlockAnchorId,
  onDwellSelectBlock: (row) => {
    selectedBlockId.value = row.id
    pdfViewerRef.value?.highlightBlockSelection(
      wireItemsToNavigationTargets(row.allBboxes)
    )
  },
})

async function markSelectedDisposition(status: BookBlockReadingDisposition) {
  const id = selectedBlockId.value
  if (id === null) {
    return
  }
  const ok = await bookReading.submitReadingDisposition(id, status)
  if (!ok) {
    return
  }
  const rows = bookBlockRows.value
  const selIdx = rows.findIndex((r) => r.id === id)
  if (selIdx >= 0 && selIdx < rows.length - 1) {
    await applyBookBlockSelection(rows[selIdx + 1]!)
  }
}

function onPagesReady() {
  const snap = initialLastRead.value
  if (!snap) return
  pdfViewerRef.value
    ?.scrollToStoredReadingPosition(snap.pageIndexZeroBased, snap.normalizedY)
    .catch(() => undefined)
}

async function onBookBlockClick(block: BookBlockRow) {
  await applyBookBlockSelection(block)
}

onMounted(async () => {
  window.addEventListener("resize", handleResize)
  if (windowWidth.value >= BOOK_READING_LAYOUT_BREAKPOINT_PX) {
    bookLayoutOpened.value = true
  }

  flatBookBlocks.value = buildFlatBookBlocks(props.book.blocks ?? [])
  if (props.book.hasSourceFile) {
    pdfLoading.value = true
    pdfError.value = null
    resetPdfPageIndicator()
    try {
      const [res, posResult] = await Promise.all([
        fetch(notebookBookFilePath(props.notebookId), {
          credentials: "same-origin",
        }),
        NotebookBooksController.getNotebookBookReadingPosition({
          path: { notebook: props.notebookId },
        }).catch(() => null),
        bookReading.syncFromServer(),
      ])
      if (!res.ok) {
        pdfError.value = "Could not load the book file."
        return
      }
      if (
        posResult !== null &&
        !posResult.error &&
        posResult.data &&
        typeof posResult.data.pageIndex === "number" &&
        typeof posResult.data.normalizedY === "number"
      ) {
        initialLastRead.value = {
          pageIndexZeroBased: posResult.data.pageIndex,
          normalizedY: posResult.data.normalizedY,
        }
      }
      bookPdfBytes.value = await res.arrayBuffer()
    } catch {
      pdfError.value = "Could not load the book file."
    } finally {
      pdfLoading.value = false
    }
  } else {
    await bookReading.syncFromServer()
  }
})

onBeforeUnmount(() => {
  window.removeEventListener("resize", handleResize)
  currentBlockAnchorDebouncer.cancel()
  lastReadPositionPatchDebouncer.cancel()
})
</script>

<style scoped>
aside {
  max-height: 100%;
}

.book-reading-book-block {
  @apply daisy-w-full daisy-min-h-10 daisy-text-left daisy-rounded-md;
  @apply daisy-border-0 daisy-border-solid daisy-border-l-4 daisy-border-transparent;
  @apply daisy-py-2 daisy-pr-2 daisy-pl-2 daisy-text-sm daisy-leading-snug daisy-font-normal;
  @apply daisy-transition-colors daisy-duration-150;
  @apply hover:daisy-bg-base-300/55;
  @apply focus:daisy-outline-none focus-visible:daisy-ring-2 focus-visible:daisy-ring-primary/50;
  @apply focus-visible:daisy-ring-offset-2 focus-visible:daisy-ring-offset-base-200;
}

.book-reading-book-block[data-current-block="true"] {
  @apply daisy-bg-primary/35;
}

.book-reading-book-block[data-current-selection="true"] {
  @apply daisy-border-primary daisy-font-medium;
}

.book-reading-book-block[data-direct-content-read="true"] {
  @apply daisy-border-r-4 daisy-border-r-success;
}

.book-reading-book-block[data-direct-content-skimmed="true"] {
  @apply daisy-border-r-4 daisy-border-r-warning;
}

.book-reading-book-block[data-direct-content-skipped="true"] {
  @apply daisy-border-r-4 daisy-border-r-neutral;
}
</style>
