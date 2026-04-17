<template>
  <div
    data-testid="book-reading-page"
    class="book-reading-page daisy-flex daisy-flex-col daisy-h-full daisy-min-h-0"
  >
    <template v-if="book">
      <div
        v-if="bookFileLoading"
        class="daisy-flex daisy-flex-1 daisy-min-h-0 daisy-flex-col"
      >
        <div class="daisy-px-2 daisy-py-2 sm:daisy-px-4 daisy-flex-1">
          <ContentLoader />
        </div>
      </div>
      <div
        v-else-if="bookFileLoadError"
        class="daisy-px-2 daisy-py-2 sm:daisy-px-4"
      >
        <div
          class="daisy-alert daisy-alert-error daisy-mb-2 daisy-mx-2 daisy-mt-2"
          data-testid="book-reading-book-file-load-error"
        >
          {{ bookFileLoadError }}
        </div>
      </div>
      <BookReadingEpubView
        v-else-if="book.format === 'epub' && bookFileBytes !== null"
        :book="book"
        :epub-bytes="bookFileBytes"
        :initial-epub-locator="initialEpubLocator"
        :initial-selected-block-id="initialSelectedBlockId"
      />
      <BookReadingContent
        v-else-if="bookFileBytes !== null"
        :book="book"
        :book-pdf-bytes="bookFileBytes"
        :initial-last-read="initialLastRead"
        :initial-selected-block-id="initialSelectedBlockId"
        @update:book="(updated) => (book = updated)"
      />
    </template>
  </div>
</template>

<script setup lang="ts">
import BookReadingContent from "@/components/book-reading/BookReadingContent.vue"
import BookReadingEpubView from "@/components/book-reading/BookReadingEpubView.vue"
import ContentLoader from "@/components/commons/ContentLoader.vue"
import { epubDisplayHref } from "@/lib/book-reading/asEpubLocator"
import type {
  BookFull,
  BookUserLastReadPosition,
  EpubLocatorFull,
  PdfLocatorFull,
} from "@generated/doughnut-backend-api"
import { NotebookBooksController } from "@generated/doughnut-backend-api/sdk.gen"
import { onMounted, ref } from "vue"

function initialReadingStateFromSavedPosition(pos: BookUserLastReadPosition): {
  initialLastRead: {
    pageIndexZeroBased: number
    normalizedY: number
  } | null
  initialEpubLocator: string | null
} {
  const loc = pos.locator
  if (!loc) {
    return { initialLastRead: null, initialEpubLocator: null }
  }
  if (loc.type === "EpubLocator_Full") {
    const s = epubDisplayHref(loc as EpubLocatorFull)
    return {
      initialLastRead: null,
      initialEpubLocator: s.length > 0 ? s : null,
    }
  }
  if (loc.type === "PdfLocator_Full") {
    const pdf = loc as PdfLocatorFull
    const y = pdf.bbox?.[1] ?? 0
    return {
      initialLastRead: {
        pageIndexZeroBased: pdf.pageIndex,
        normalizedY: Math.round(y),
      },
      initialEpubLocator: null,
    }
  }
  return { initialLastRead: null, initialEpubLocator: null }
}

const props = defineProps({
  notebookId: { type: Number, required: true },
})

function notebookBookFilePath(notebookId: number) {
  return `/api/notebooks/${notebookId}/book/file`
}

const book = ref<BookFull | null>(null)
const bookFileBytes = ref<ArrayBuffer | null>(null)
const bookFileLoading = ref(false)
const bookFileLoadError = ref<string | null>(null)
const initialLastRead = ref<{
  pageIndexZeroBased: number
  normalizedY: number
} | null>(null)
const initialSelectedBlockId = ref<number | null>(null)
const initialEpubLocator = ref<string | null>(null)

onMounted(async () => {
  const { data, error } = await NotebookBooksController.getBook({
    path: { notebook: props.notebookId },
  })
  if (!error && data) {
    book.value = data
    const notebook = Number(data.notebookId)
    bookFileLoading.value = true
    bookFileLoadError.value = null
    try {
      const [res, posResult] = await Promise.all([
        fetch(notebookBookFilePath(notebook), {
          credentials: "same-origin",
        }),
        NotebookBooksController.getNotebookBookReadingPosition({
          path: { notebook },
        }).catch(() => null),
      ])
      if (!res.ok) {
        bookFileLoadError.value = "Could not load the book file."
        return
      }
      const pos =
        posResult !== null && !posResult.error && posResult.data
          ? posResult.data
          : null
      initialSelectedBlockId.value =
        typeof pos?.selectedBookBlockId === "number"
          ? pos.selectedBookBlockId
          : null
      const fromLoc = pos ? initialReadingStateFromSavedPosition(pos) : null
      initialEpubLocator.value = fromLoc?.initialEpubLocator ?? null
      initialLastRead.value = fromLoc?.initialLastRead ?? null
      bookFileBytes.value = await res.arrayBuffer()
    } catch {
      bookFileLoadError.value = "Could not load the book file."
    } finally {
      bookFileLoading.value = false
    }
  }
})
</script>

<style scoped>
.book-reading-page {
  max-height: 100%;
  padding-top: env(safe-area-inset-top, 0px);
}
</style>
