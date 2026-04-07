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
        data-testid="book-reading-viewport-current-live"
        class="daisy-sr-only"
      >
        {{ viewportCurrentLiveText }}
      </div>
      <GlobalBar>
        <button
          type="button"
          class="daisy-btn daisy-btn-sm daisy-btn-ghost daisy-shrink-0"
          :class="{ 'sidebar-expanded': outlineOpened }"
          data-testid="book-reading-outline-toggle"
          aria-label="Outline"
          title="Outline"
          :aria-expanded="outlineOpened"
          aria-controls="book-reading-outline-panel"
          @click="outlineOpened = !outlineOpened"
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
              <template v-if="outlineOpened">
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
        v-if="!isMdOrLarger && outlineOpened"
        class="daisy-fixed daisy-inset-0 daisy-bg-black/50 daisy-z-30"
        aria-hidden="true"
        @click="outlineOpened = false"
      />
      <div class="daisy-flex daisy-flex-1 daisy-min-h-0 daisy-relative">
        <aside
          id="book-reading-outline-panel"
          ref="outlineAsideRef"
          data-testid="book-reading-outline-aside"
          :class="[
            'daisy-bg-base-200 daisy-w-72 daisy-min-w-[16rem] daisy-max-w-[min(20rem,85vw)] daisy-transition-transform daisy-ease-in-out daisy-duration-200 daisy-overflow-y-auto daisy-overflow-x-hidden',
            isMdOrLarger
              ? outlineOpened
                ? 'daisy-relative daisy-shrink-0 daisy-border-r daisy-border-base-300'
                : 'daisy-hidden'
              : outlineOpened
                ? 'daisy-translate-x-0 daisy-fixed daisy-top-0 daisy-left-0 daisy-z-40 daisy-h-full daisy-pt-[env(safe-area-inset-top)]'
                : '-daisy-translate-x-full daisy-fixed daisy-top-0 daisy-left-0 daisy-z-40 daisy-h-full',
          ]"
        >
          <div
            data-testid="book-reading-outline"
            class="daisy-p-3 daisy-pb-8"
          >
            <button
              v-for="node in outlineRows"
              :key="node.id"
              type="button"
              data-testid="book-outline-node"
              class="book-reading-outline-row"
              :data-outline-depth="node.depth"
              :data-outline-current="
                node.startAnchor.id === viewportCurrentAnchorId
                  ? 'true'
                  : undefined
              "
              :data-outline-selected="
                node.id === selectedOutlineRangeId ? 'true' : undefined
              "
              :aria-current="
                node.startAnchor.id === viewportCurrentAnchorId
                  ? 'location'
                  : undefined
              "
              :style="{ paddingLeft: `${node.depth * 0.75}rem` }"
              @click="onOutlineRowClick(node)"
            >
              {{ node.title }}
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
          <PdfBookViewer
            v-else-if="bookPdfBytes"
            ref="pdfViewerRef"
            :pdf-bytes="bookPdfBytes"
            @load-error="onPdfLoadError"
            @viewport-anchor-page="onViewportAnchorPage"
            @pages-ready="onPagesReady"
          />
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
import {
  ANCHOR_FORMAT_PDF_MINERU_OUTLINE_V1,
  parseMineruOutlineV1StartAnchor,
} from "@/lib/book-reading/mineruOutlineV1PageIndex"
import { createLastReadPositionPatchDebouncer } from "@/lib/book-reading/debounceLastReadPositionPatch"
import { createViewportCurrentAnchorDebouncer } from "@/lib/book-reading/debounceViewportCurrentAnchorId"
import { nextLiveAnnouncementText } from "@/lib/book-reading/viewportCurrentLiveAnnouncement"
import { viewportCurrentAnchorIdFromAnchorPage } from "@/lib/book-reading/viewportCurrentRangeFromAnchorPage"
import type { ViewportYRange } from "@/lib/book-reading/pdfViewerViewportTopYDown"
import type {
  BookAnchorFull,
  BookFull,
  BookRangeFull,
} from "@generated/doughnut-backend-api"
import { NotebookBooksController } from "@generated/doughnut-backend-api/sdk.gen"
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue"

const BOOK_READING_LAYOUT_BREAKPOINT_PX = 768
const VIEWPORT_CURRENT_ANCHOR_DEBOUNCE_MS = 120
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

const outlineOpened = ref(false)
const outlineAsideRef = ref<HTMLElement | null>(null)
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

type OutlineNode = {
  id: number
  title: string
  depth: number
  startAnchor: BookAnchorFull
}

function buildFlatOutline(ranges: BookRangeFull[]): OutlineNode[] {
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

  const result: OutlineNode[] = []
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

const flatOutline = ref<OutlineNode[]>([])
const outlineRows = computed(() => flatOutline.value)
const selectedOutlineRangeId = ref<number | null>(null)
const viewportCurrentAnchorId = ref<number | null>(null)
const viewportCurrentLiveText = ref("")
const lastAnnouncedViewportTitle = ref<string | undefined>(undefined)

const viewportCurrentAnchorDebouncer = createViewportCurrentAnchorDebouncer({
  delayMs: VIEWPORT_CURRENT_ANCHOR_DEBOUNCE_MS,
  commit: (id) => {
    viewportCurrentAnchorId.value = id
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
  const candidate = viewportCurrentAnchorIdFromAnchorPage(
    outlineRows.value.map((n) => n.startAnchor),
    payload.anchorPageIndexZeroBased,
    payload.viewport,
    payload.pagesCount
  )
  viewportCurrentAnchorDebouncer.propose(candidate)
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

watch(viewportCurrentAnchorId, (id) => {
  const { text, changed } = nextLiveAnnouncementText(
    lastAnnouncedViewportTitle.value,
    id,
    outlineRows.value
  )
  if (!changed) {
    return
  }
  lastAnnouncedViewportTitle.value = text
  viewportCurrentLiveText.value = text
})

watch(
  viewportCurrentAnchorId,
  (id) => {
    if (id === null || !outlineOpened.value) {
      return
    }
    requestAnimationFrame(() => {
      if (!outlineOpened.value) {
        return
      }
      const row = outlineAsideRef.value?.querySelector(
        '[data-outline-current="true"]'
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
  scrollToMineruOutlineV1Target: (target: {
    pageIndexZeroBased: number
    bbox: readonly [number, number, number, number] | null
  }) => Promise<void>
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

async function onOutlineRowClick(node: OutlineNode) {
  const anchor = node.startAnchor
  if (anchor.anchorFormat !== ANCHOR_FORMAT_PDF_MINERU_OUTLINE_V1) {
    return
  }
  const parsed = parseMineruOutlineV1StartAnchor(anchor.value)
  if (parsed === null) {
    return
  }
  selectedOutlineRangeId.value = node.id
  await pdfViewerRef.value?.scrollToMineruOutlineV1Target({
    pageIndexZeroBased: parsed.pageIndex,
    bbox: parsed.bbox,
  })
  viewportCurrentAnchorDebouncer.commitNow(node.startAnchor.id)
}

onMounted(async () => {
  window.addEventListener("resize", handleResize)
  if (windowWidth.value >= BOOK_READING_LAYOUT_BREAKPOINT_PX) {
    outlineOpened.value = true
  }

  const { data, error } = await NotebookBooksController.getBook({
    path: { notebook: props.notebookId },
  })
  if (!error && data) {
    book.value = data
    flatOutline.value = buildFlatOutline(data.ranges ?? [])
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
  viewportCurrentAnchorDebouncer.cancel()
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

.book-reading-outline-row {
  @apply daisy-w-full daisy-min-h-10 daisy-text-left daisy-rounded-md;
  @apply daisy-border-0 daisy-border-solid daisy-border-l-4 daisy-border-transparent;
  @apply daisy-py-2 daisy-pr-2 daisy-pl-2 daisy-text-sm daisy-leading-snug daisy-font-normal;
  @apply daisy-transition-colors daisy-duration-150;
  @apply hover:daisy-bg-base-300/55;
  @apply focus:daisy-outline-none focus-visible:daisy-ring-2 focus-visible:daisy-ring-primary/50;
  @apply focus-visible:daisy-ring-offset-2 focus-visible:daisy-ring-offset-base-200;
}

.book-reading-outline-row[data-outline-current="true"] {
  @apply daisy-bg-primary/35;
}

.book-reading-outline-row[data-outline-selected="true"] {
  @apply daisy-border-primary daisy-font-medium;
}
</style>
