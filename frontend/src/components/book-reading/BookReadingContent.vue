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
    :book-block-rows="flatBookBlocks"
    :current-block-anchor-id="currentBlockAnchorId"
    :selected-block-id="selectedBlockId"
    :disposition-for-block="bookReading.dispositionForBlock"
    @block-click="onBookBlockClick"
  >
    <main class="daisy-flex-1 daisy-min-h-0 daisy-min-w-0 daisy-relative">
      <div
        v-if="pdfViewerLoadError"
        class="daisy-alert daisy-alert-error daisy-mb-2 daisy-mx-2 daisy-mt-2"
        data-testid="book-reading-pdf-viewer-load-error"
      >
        {{ pdfViewerLoadError }}
      </div>
      <template v-else>
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
  </BookReadingBookLayout>
</template>

<script setup lang="ts">
import BookLayoutToggleButton from "@/components/book-reading/BookLayoutToggleButton.vue"
import BookReadingBookLayout, {
  type BookReadingBookLayoutBlockRow,
} from "@/components/book-reading/BookReadingBookLayout.vue"
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
import type { BookBlockFull, BookFull } from "@generated/doughnut-backend-api"
import { NotebookBooksController } from "@generated/doughnut-backend-api/sdk.gen"
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue"

const bookReadingBookLayoutPanelId = "book-reading-book-layout-panel"
const BOOK_READING_LAYOUT_BREAKPOINT_PX = 768
const CURRENT_BLOCK_ANCHOR_DEBOUNCE_MS = 120
const LAST_READ_POSITION_PATCH_DEBOUNCE_MS = 400
/** Vertical space (px) reserved at the bottom of the PDF main pane by ReadingControlPanel. */
const READING_PANEL_OBSTRUCTION_PX = 80
const SNAP_HOLD_MS = 500

const props = defineProps<{
  book: BookFull
  bookPdfBytes: ArrayBuffer
  initialLastRead: { pageIndexZeroBased: number; normalizedY: number } | null
}>()

const notebookId = computed(() => Number(props.book.notebookId))
const bookReading = useNotebookBookReadingRecords(notebookId)

const pdfViewerLoadError = ref<string | null>(null)

const pdfBarCurrentPage = ref<number | null>(null)
const pdfBarPagesTotal = ref<number | null>(null)

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

function buildFlatBookBlocks(
  blocks: BookBlockFull[]
): BookReadingBookLayoutBlockRow[] {
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

  const result: BookReadingBookLayoutBlockRow[] = []
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

const flatBookBlocks = ref<BookReadingBookLayoutBlockRow[]>([])
const currentBlockAnchorId = ref<number | null>(null)

const selectedBlockId = ref<number | null>(null)

const lastContentBottomVisible = ref(false)
const geometryEverVisibleForSelection = ref(false)

const snapbackAttempts = new Map<number, number>()

function shouldSnapBack(proposedAnchorId: number | null): boolean {
  if (proposedAnchorId === null) return false
  const selId = selectedBlockId.value
  if (selId === null) return false
  if (bookReading.hasRecordedDisposition(selId)) return false
  const rows = flatBookBlocks.value
  const selIdx = rows.findIndex((r) => r.id === selId)
  if (selIdx < 0 || selIdx >= rows.length - 1) return false
  const sel = rows[selIdx]!
  const successor = rows[selIdx + 1]!
  if (!sel.hasDirectContent) return false
  if (proposedAnchorId !== successor.startAnchor.id) return false
  if (!geometryEverVisibleForSelection.value) return false
  if (sel.allBboxes.length <= 1) return false
  return (snapbackAttempts.get(selId) ?? 0) < 1
}

function performSnapBack(): void {
  const selId = selectedBlockId.value
  if (selId === null) return
  const rows = flatBookBlocks.value
  const sel = rows.find((r) => r.id === selId)
  if (!sel || sel.allBboxes.length <= 1) return
  const lastBbox = sel.allBboxes[sel.allBboxes.length - 1]!
  snapbackAttempts.set(selId, (snapbackAttempts.get(selId) ?? 0) + 1)
  const parsedStart = parsePdfOutlineV1Anchor(sel.startAnchor)
  if (parsedStart !== null && parsedStart.pageIndex === lastBbox.pageIndex) {
    pdfViewerRef.value
      ?.scrollToPdfOutlineV1Target(
        parsedStart,
        wireItemsToNavigationTargets(sel.allBboxes)
      )
      .then(() => pdfViewerRef.value?.suppressScrollInput(SNAP_HOLD_MS))
      .catch(() => undefined)
  } else {
    pdfViewerRef.value?.snapToContentBottomAndHold(
      lastBbox.pageIndex,
      (lastBbox.bbox as number[])[3]!,
      READING_PANEL_OBSTRUCTION_PX,
      SNAP_HOLD_MS
    )
  }
}

function selectedBlockHasSuccessorAndNoDisposition(): {
  selId: number
  successor: BookReadingBookLayoutBlockRow
} | null {
  const selId = selectedBlockId.value
  if (selId === null) return null
  if (bookReading.hasRecordedDisposition(selId)) return null
  const rows = flatBookBlocks.value
  const selIdx = rows.findIndex((r) => r.id === selId)
  if (selIdx < 0 || selIdx >= rows.length - 1) return null
  return { selId, successor: rows[selIdx + 1]! }
}

// allBboxes: index 0 is the anchor; remaining entries are direct-content blocks.
// When length > 1, the last entry is the last direct-content bbox.
const blockAwaitingConfirmation =
  computed<BookReadingBookLayoutBlockRow | null>(() => {
    const context = selectedBlockHasSuccessorAndNoDisposition()
    if (context === null) return null
    const { selId, successor } = context
    const rows = flatBookBlocks.value
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
    if (shouldSnapBack(id)) {
      performSnapBack()
      return false
    }
    currentBlockAnchorId.value = id
    return true
  },
})

const lastReadPositionPatchDebouncer = createLastReadPositionPatchDebouncer({
  delayMs: LAST_READ_POSITION_PATCH_DEBOUNCE_MS,
  patch: (body) =>
    NotebookBooksController.patchNotebookBookReadingPosition({
      path: { notebook: notebookId.value },
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
    flatBookBlocks.value.map((r) => r.startAnchor),
    payload.anchorPageIndexZeroBased,
    payload.viewport,
    payload.pagesCount
  )
  currentBlockAnchorDebouncer.propose(candidate)
  const selIdForGeometry = selectedBlockId.value
  if (selIdForGeometry !== null) {
    const selForGeometry = flatBookBlocks.value.find(
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
}

watch(currentBlockAnchorId, (id) => {
  const { text, changed } = nextLiveAnnouncementText(
    lastAnnouncedCurrentBlockTitle.value,
    id,
    flatBookBlocks.value
  )
  if (!changed) {
    return
  }
  lastAnnouncedCurrentBlockTitle.value = text
  currentBlockLiveText.value = text
})

watch(currentBlockAnchorId, async (anchorId) => {
  if (anchorId === null) return
  const rows = flatBookBlocks.value
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
  snapbackAttempts.clear()
})

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
  snapToContentBottomAndHold: (
    pageIndex: number,
    normalizedBboxBottom: number,
    obstructionPx: number,
    holdMs: number
  ) => void
  suppressScrollInput: (holdMs: number) => void
  zoomIn: () => void
  zoomOut: () => void
  isLastContentBottomVisible: (
    target: PdfOutlineV1NavigationTarget,
    obstructionPx: number
  ) => boolean
} | null>(null)

async function applyBookBlockSelection(block: BookReadingBookLayoutBlockRow) {
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
  bookBlockRows: () => flatBookBlocks.value,
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
  const rows = flatBookBlocks.value
  const selIdx = rows.findIndex((r) => r.id === id)
  if (selIdx >= 0 && selIdx < rows.length - 1) {
    await applyBookBlockSelection(rows[selIdx + 1]!)
  }
}

function onPagesReady() {
  const snap = props.initialLastRead
  if (!snap) return
  pdfViewerRef.value
    ?.scrollToStoredReadingPosition(snap.pageIndexZeroBased, snap.normalizedY)
    .catch(() => undefined)
}

async function onBookBlockClick(block: BookReadingBookLayoutBlockRow) {
  await applyBookBlockSelection(block)
}

onMounted(async () => {
  window.addEventListener("resize", handleResize)
  if (windowWidth.value >= BOOK_READING_LAYOUT_BREAKPOINT_PX) {
    bookLayoutOpened.value = true
  }
  flatBookBlocks.value = buildFlatBookBlocks(props.book.blocks)
  await bookReading.syncFromServer()
})

onBeforeUnmount(() => {
  window.removeEventListener("resize", handleResize)
  currentBlockAnchorDebouncer.cancel()
  lastReadPositionPatchDebouncer.cancel()
})
</script>
