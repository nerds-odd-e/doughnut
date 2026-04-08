<template>
  <div
    data-testid="book-reading-page"
    class="book-reading-page daisy-flex daisy-flex-col daisy-h-full daisy-min-h-0"
  >
    <template v-if="book">
      <div
        role="status"
        aria-live="polite"
        aria-atomic="true"
        data-testid="book-reading-current-range-live"
        class="daisy-sr-only"
      >
        {{ currentRangeLiveText }}
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
              v-for="range in bookRangeRows"
              :key="range.id"
              type="button"
              data-testid="book-reading-book-range"
              class="book-reading-book-range"
              :data-book-range-depth="range.depth"
              :data-current-range="
                range.startAnchor.id === currentRangeAnchorId
                  ? 'true'
                  : undefined
              "
              :data-current-selection="
                range.id === currentSelectionRangeId ? 'true' : undefined
              "
              :data-direct-content-read="
                readRangeIdSet.has(range.id) ? 'true' : undefined
              "
              :aria-current="
                range.startAnchor.id === currentRangeAnchorId
                  ? 'location'
                  : undefined
              "
              :style="{ paddingLeft: `${range.depth * 0.75}rem` }"
              @click="onBookRangeClick(range)"
            >
              {{ range.title }}
              <span
                v-if="readRangeIdSet.has(range.id)"
                class="daisy-sr-only"
              >
                Marked as read
              </span>
            </button>
          </div>
        </aside>
        <main
          class="daisy-flex-1 daisy-min-h-0 daisy-min-w-0 daisy-relative"
        >
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
              v-if="readingControlPanelVisible"
              :selected-range-title="readingControlSelectedRangeTitle"
              @mark-as-read="markSelectedRangeAsRead"
            />
          </template>
        </main>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import ContentLoader from "@/components/commons/ContentLoader.vue"
import GlobalBar from "@/components/toolbars/GlobalBar.vue"
import PdfBookViewer from "@/components/book-reading/PdfBookViewer.vue"
import PdfControl from "@/components/book-reading/PdfControl.vue"
import ReadingControlPanel from "@/components/book-reading/ReadingControlPanel.vue"
import {
  parsePdfOutlineV1Anchor,
  type PdfOutlineV1NavigationTarget,
} from "@/lib/book-reading/pdfOutlineV1Anchor"
import { createLastReadPositionPatchDebouncer } from "@/lib/book-reading/debounceLastReadPositionPatch"
import { createCurrentRangeAnchorDebouncer } from "@/lib/book-reading/debounceCurrentRangeAnchorId"
import { nextLiveAnnouncementText } from "@/lib/book-reading/currentRangeLiveAnnouncement"
import { currentRangeAnchorIdFromAnchorPage } from "@/lib/book-reading/currentRangeAnchorFromAnchorPage"
import { readRangeIdsFromRecords } from "@/lib/book-reading/readRangeIdsFromRecords"
import type { ViewportYRange } from "@/lib/book-reading/pdfViewerViewportTopYDown"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import type {
  BookAnchorFull,
  BookFull,
  BookRangeFull,
  BookRangeReadingRecordListItem,
} from "@generated/doughnut-backend-api"
import { NotebookBooksController } from "@generated/doughnut-backend-api/sdk.gen"
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue"

const BOOK_READING_LAYOUT_BREAKPOINT_PX = 768
const CURRENT_RANGE_ANCHOR_DEBOUNCE_MS = 120
const LAST_READ_POSITION_PATCH_DEBOUNCE_MS = 400

const props = defineProps({
  notebookId: { type: Number, required: true },
})

function notebookBookFilePath(notebookId: number) {
  return `/api/notebooks/${notebookId}/book/file`
}

const book = ref<BookFull | null>(null)
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

type BookRangeRow = {
  id: number
  title: string
  depth: number
  startAnchor: BookAnchorFull
}

function buildFlatBookRanges(ranges: BookRangeFull[]): BookRangeRow[] {
  const childrenMap = new Map<number | null, BookRangeFull[]>()
  for (const range of ranges) {
    const parentId =
      range.parentRangeId != null ? Number(range.parentRangeId) : null
    const siblings = childrenMap.get(parentId) ?? []
    siblings.push(range)
    childrenMap.set(parentId, siblings)
  }
  const sortByOrder = (a: BookRangeFull, b: BookRangeFull) =>
    (a.siblingOrder ?? 0) - (b.siblingOrder ?? 0)

  const result: BookRangeRow[] = []
  function visit(parentId: number | null, depth: number) {
    const children = (childrenMap.get(parentId) ?? []).slice().sort(sortByOrder)
    for (const child of children) {
      result.push({
        id: child.id,
        title: child.title,
        depth,
        startAnchor: child.startAnchor,
      })
      visit(child.id, depth + 1)
    }
  }
  visit(null, 0)
  return result
}

const flatBookRanges = ref<BookRangeRow[]>([])
const bookRangeRows = computed(() => flatBookRanges.value)
const currentSelectionRangeId = ref<number | null>(null)
const currentRangeAnchorId = ref<number | null>(null)

const bookReadingRecords = ref<BookRangeReadingRecordListItem[]>([])
const readRangeIdSet = computed(() =>
  readRangeIdsFromRecords(bookReadingRecords.value)
)

const readingControlSelectedRangeTitle = computed(() => {
  const selId = currentSelectionRangeId.value
  if (selId === null) {
    return ""
  }
  return bookRangeRows.value.find((r) => r.id === selId)?.title ?? ""
})

const readingControlPanelVisible = computed(() => {
  const selId = currentSelectionRangeId.value
  const curAnchorId = currentRangeAnchorId.value
  if (selId === null || curAnchorId === null) {
    return false
  }
  const rows = bookRangeRows.value
  const selIdx = rows.findIndex((r) => r.id === selId)
  if (selIdx < 0 || selIdx >= rows.length - 1) {
    return false
  }
  if (readRangeIdSet.value.has(selId)) {
    return false
  }
  const successor = rows[selIdx + 1]!
  return successor.startAnchor.id === curAnchorId
})

async function markSelectedRangeAsRead() {
  const id = currentSelectionRangeId.value
  if (id === null) {
    return
  }
  const result = await apiCallWithLoading(async () => {
    const putRes =
      await NotebookBooksController.putNotebookBookRangeReadingRecord({
        path: { notebook: props.notebookId, bookRange: id },
      })
    if (putRes.error) {
      return putRes
    }
    return NotebookBooksController.getNotebookBookReadingRecords({
      path: { notebook: props.notebookId },
    })
  })
  if (result.error || result.data === undefined) {
    return
  }
  bookReadingRecords.value = result.data
  const rows = bookRangeRows.value
  const selIdx = rows.findIndex((r) => r.id === id)
  if (selIdx >= 0 && selIdx < rows.length - 1) {
    currentSelectionRangeId.value = rows[selIdx + 1]!.id
  }
}
const currentRangeLiveText = ref("")
const lastAnnouncedCurrentRangeTitle = ref<string | undefined>(undefined)

const currentRangeAnchorDebouncer = createCurrentRangeAnchorDebouncer({
  delayMs: CURRENT_RANGE_ANCHOR_DEBOUNCE_MS,
  commit: (id) => {
    currentRangeAnchorId.value = id
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
  const candidate = currentRangeAnchorIdFromAnchorPage(
    bookRangeRows.value.map((r) => r.startAnchor),
    payload.anchorPageIndexZeroBased,
    payload.viewport,
    payload.pagesCount
  )
  currentRangeAnchorDebouncer.propose(candidate)
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

watch(currentRangeAnchorId, (id) => {
  const { text, changed } = nextLiveAnnouncementText(
    lastAnnouncedCurrentRangeTitle.value,
    id,
    bookRangeRows.value
  )
  if (!changed) {
    return
  }
  lastAnnouncedCurrentRangeTitle.value = text
  currentRangeLiveText.value = text
})

watch(
  currentRangeAnchorId,
  (id) => {
    if (id === null || !bookLayoutOpened.value) {
      return
    }
    requestAnimationFrame(() => {
      if (!bookLayoutOpened.value) {
        return
      }
      const row = bookLayoutAsideRef.value?.querySelector(
        '[data-current-range="true"]'
      )
      row?.scrollIntoView({ block: "nearest", inline: "nearest" })
    })
  },
  { flush: "post" }
)

const initialLastRead = ref<{
  pageIndexZeroBased: number
  normalizedY: number
} | null>(null)

const pdfViewerRef = ref<{
  scrollToPdfOutlineV1Target: (
    target: PdfOutlineV1NavigationTarget
  ) => Promise<void>
  scrollToStoredReadingPosition: (
    pageIndexZeroBased: number,
    normalizedY: number
  ) => Promise<void>
  zoomIn: () => void
  zoomOut: () => void
} | null>(null)

function onPagesReady() {
  const snap = initialLastRead.value
  if (!snap) return
  pdfViewerRef.value
    ?.scrollToStoredReadingPosition(snap.pageIndexZeroBased, snap.normalizedY)
    .catch(() => undefined)
}

async function onBookRangeClick(range: BookRangeRow) {
  const parsed = parsePdfOutlineV1Anchor(range.startAnchor)
  if (parsed === null) {
    return
  }
  currentSelectionRangeId.value = range.id
  await pdfViewerRef.value?.scrollToPdfOutlineV1Target(parsed)
  currentRangeAnchorDebouncer.commitNow(range.startAnchor.id)
}

onMounted(async () => {
  window.addEventListener("resize", handleResize)
  if (windowWidth.value >= BOOK_READING_LAYOUT_BREAKPOINT_PX) {
    bookLayoutOpened.value = true
  }

  const { data, error } = await NotebookBooksController.getBook({
    path: { notebook: props.notebookId },
  })
  if (!error && data) {
    book.value = data
    flatBookRanges.value = buildFlatBookRanges(data.ranges ?? [])
    const recordsResult =
      await NotebookBooksController.getNotebookBookReadingRecords({
        path: { notebook: props.notebookId },
      })
    if (!recordsResult.error && recordsResult.data) {
      bookReadingRecords.value = recordsResult.data
    }
    if (data.hasSourceFile) {
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
    }
  }
})

onBeforeUnmount(() => {
  window.removeEventListener("resize", handleResize)
  currentRangeAnchorDebouncer.cancel()
  lastReadPositionPatchDebouncer.cancel()
})
</script>

<style scoped>
.book-reading-page {
  max-height: 100%;
  padding-top: env(safe-area-inset-top, 0px);
}

aside {
  max-height: 100%;
}

.book-reading-book-range {
  @apply daisy-w-full daisy-min-h-10 daisy-text-left daisy-rounded-md;
  @apply daisy-border-0 daisy-border-solid daisy-border-l-4 daisy-border-transparent;
  @apply daisy-py-2 daisy-pr-2 daisy-pl-2 daisy-text-sm daisy-leading-snug daisy-font-normal;
  @apply daisy-transition-colors daisy-duration-150;
  @apply hover:daisy-bg-base-300/55;
  @apply focus:daisy-outline-none focus-visible:daisy-ring-2 focus-visible:daisy-ring-primary/50;
  @apply focus-visible:daisy-ring-offset-2 focus-visible:daisy-ring-offset-base-200;
}

.book-reading-book-range[data-current-range="true"] {
  @apply daisy-bg-primary/35;
}

.book-reading-book-range[data-current-selection="true"] {
  @apply daisy-border-primary daisy-font-medium;
}

.book-reading-book-range[data-direct-content-read="true"] {
  @apply daisy-border-r-4 daisy-border-r-success;
}
</style>
