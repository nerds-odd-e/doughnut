<template>
  <div
    data-testid="book-reading-page"
    class="book-reading-page daisy-flex daisy-flex-col daisy-h-full daisy-min-h-0"
  >
    <BookReadingContent
      v-if="book"
      :book="book"
      :notebook-id="props.notebookId"
      :pdf-loading="pdfLoading"
      :pdf-error="pdfError"
      :book-pdf-bytes="bookPdfBytes"
      :initial-last-read="initialLastRead"
      @pdf-load-error="(msg) => (pdfError = msg)"
    />
  </div>
</template>

<script setup lang="ts">
import BookReadingContent from "@/components/book-reading/BookReadingContent.vue"
import type { BookFull } from "@generated/doughnut-backend-api"
import { NotebookBooksController } from "@generated/doughnut-backend-api/sdk.gen"
import { onMounted, ref } from "vue"

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
const initialLastRead = ref<{
  pageIndexZeroBased: number
  normalizedY: number
} | null>(null)

onMounted(async () => {
  const { data, error } = await NotebookBooksController.getBook({
    path: { notebook: props.notebookId },
  })
  if (!error && data) {
    book.value = data
    if (data.hasSourceFile) {
      pdfLoading.value = true
      pdfError.value = null
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
</script>

<style scoped>
.book-reading-page {
  max-height: 100%;
  padding-top: env(safe-area-inset-top, 0px);
}
</style>
