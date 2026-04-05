<template>
  <div
    data-testid="book-reading-page"
    class="book-reading-page daisy-flex daisy-flex-col daisy-h-full daisy-min-h-0"
  >
    <template v-if="book">
      <GlobalBar>
        <button
          type="button"
          class="daisy-btn daisy-btn-sm daisy-btn-ghost daisy-shrink-0"
          :class="{ 'sidebar-expanded': outlineOpened }"
          data-testid="book-reading-outline-toggle"
          aria-label="Outline"
          title="Outline"
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
      </GlobalBar>
      <div
        v-if="!isMdOrLarger && outlineOpened"
        class="daisy-fixed daisy-inset-0 daisy-bg-black/50 daisy-z-30"
        aria-hidden="true"
        @click="outlineOpened = false"
      />
      <div class="daisy-flex daisy-flex-1 daisy-min-h-0 daisy-relative">
        <aside
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
              v-for="node in flatOutline"
              :key="node.id"
              type="button"
              data-testid="book-outline-node"
              :data-outline-depth="node.depth"
              class="daisy-btn daisy-btn-ghost daisy-btn-sm daisy-w-full daisy-justify-start daisy-normal-case daisy-h-auto daisy-min-h-10 daisy-py-2 daisy-px-2 daisy-text-sm daisy-leading-snug daisy-font-normal"
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
import {
  ANCHOR_FORMAT_PDF_MINERU_OUTLINE_V1,
  extractPageIndexZeroBased,
} from "@/lib/book-reading/mineruOutlineV1PageIndex"
import type {
  BookAnchorFull,
  BookFull,
  BookRangeFull,
} from "@generated/doughnut-backend-api"
import { NotebookBooksController } from "@generated/doughnut-backend-api/sdk.gen"
import { computed, onBeforeUnmount, onMounted, ref } from "vue"

const BOOK_READING_LAYOUT_BREAKPOINT_PX = 768

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

const outlineOpened = ref(false)
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
}

type OutlineNode = {
  id: number
  title: string
  depth: number
  startAnchor?: BookAnchorFull
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

const pdfViewerRef = ref<{
  scrollToPageIndexZeroBased: (index: number) => void
} | null>(null)

function onOutlineRowClick(node: OutlineNode) {
  const anchor = node.startAnchor
  if (
    !anchor ||
    anchor.anchorFormat !== ANCHOR_FORMAT_PDF_MINERU_OUTLINE_V1 ||
    anchor.value == null
  ) {
    return
  }
  const pageIndex = extractPageIndexZeroBased(anchor.value)
  if (pageIndex === null) {
    return
  }
  pdfViewerRef.value?.scrollToPageIndexZeroBased(pageIndex)
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
      try {
        const res = await fetch(notebookBookFilePath(props.notebookId), {
          credentials: "same-origin",
        })
        if (!res.ok) {
          pdfError.value = "Could not load the book file."
          return
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
})
</script>

<style scoped>
.book-reading-page {
  max-height: 100%;
}

aside {
  max-height: 100%;
}
</style>
