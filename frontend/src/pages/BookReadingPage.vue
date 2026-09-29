<template>
  <div
    data-testid="book-reading-page"
    class="book-reading-page flex flex-col h-full min-h-0"
  >
    <BookReader :key="notebookId" :notebook-id="notebookId" />
  </div>
</template>

<script setup lang="ts">
import BookReader from "@/components/book-reading/BookReader.vue"

defineProps({
  notebookId: { type: Number, required: true },
})
</script>

<style scoped>
.book-reading-page {
  max-height: 100%;
  padding-top: env(safe-area-inset-top, 0px);
}
</style>
