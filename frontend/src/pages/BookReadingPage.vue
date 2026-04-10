<template>
  <div
    data-testid="book-reading-page"
    class="book-reading-page daisy-flex daisy-flex-col daisy-h-full daisy-min-h-0"
  >
    <template v-if="book">
      <div
        v-if="pdfLoading"
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
      <BookReadingContent
        v-else
        :book="book"
        :book-pdf-bytes="contentPdfBytes"
        :initial-last-read="initialLastRead"
      />
    </template>
  </div>
</template>

<script setup lang="ts">
import BookReadingContent from "@/components/book-reading/BookReadingContent.vue"
import ContentLoader from "@/components/commons/ContentLoader.vue"
import type { BookFull } from "@generated/doughnut-backend-api"
import { NotebookBooksController } from "@generated/doughnut-backend-api/sdk.gen"
import { computed, onMounted, ref } from "vue"

const props = defineProps({
  notebookId: { type: Number, required: true },
})

function notebookBookFilePath(notebookId: number) {
  return `/api/notebooks/${notebookId}/book/file`
}

const book = ref<BookFull | null>(null)
const bookPdfBytes = ref<ArrayBuffer | null>(null)
const pdfLoading = ref(false)
const bookFileLoadError = ref<string | null>(null)
const initialLastRead = ref<{
  pageIndexZeroBased: number
  normalizedY: number
} | null>(null)

const contentPdfBytes = computed(() => {
  if (!book.value) return undefined
  return bookPdfBytes.value ?? undefined
})

onMounted(async () => {
  const { data, error } = await NotebookBooksController.getBook({
    path: { notebook: props.notebookId },
  })
  if (!error && data) {
    book.value = data
    const notebook = Number(data.notebookId)
    pdfLoading.value = true
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
      bookFileLoadError.value = "Could not load the book file."
    } finally {
      pdfLoading.value = false
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
