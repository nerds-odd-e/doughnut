<template>
  <div
    data-testid="book-reading-page"
    class="book-reading-page daisy-flex daisy-flex-col daisy-h-full daisy-min-h-0"
  >
    <BookReadingContent
      v-if="book"
      :book="book"
      :notebook-id="props.notebookId"
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

const book = ref<BookFull | null>(null)

onMounted(async () => {
  const { data, error } = await NotebookBooksController.getBook({
    path: { notebook: props.notebookId },
  })
  if (!error && data) {
    book.value = data
  }
})
</script>

<style scoped>
.book-reading-page {
  max-height: 100%;
  padding-top: env(safe-area-inset-top, 0px);
}
</style>
