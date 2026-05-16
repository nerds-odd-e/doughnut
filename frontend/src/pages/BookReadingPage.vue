<template>
  <div
    data-testid="book-reading-page"
    class="book-reading-page flex flex-col h-full min-h-0"
  >
    <template v-if="book">
      <div
        v-if="fileLoading"
        class="flex flex-1 min-h-0 flex-col"
      >
        <div class="px-2 py-2 sm:px-4 flex-1">
          <ContentLoader />
        </div>
      </div>
      <div
        v-else-if="fileError"
        class="px-2 py-2 sm:px-4"
      >
        <div
          class="daisy-alert daisy-alert-error mb-2 mx-2 mt-2"
          data-testid="book-reading-book-file-load-error"
        >
          {{ fileError }}
        </div>
      </div>
      <BookReadingEpubView
        v-else-if="bootstrap?.kind === 'epub'"
        :book="bootstrap.book"
        :epub-bytes="bootstrap.bytes"
        :initial-locator="bootstrap.initialLocator"
        :initial-selected-block-id="bootstrap.initialSelectedBlockId"
      />
      <BookReadingContent
        v-else-if="bootstrap?.kind === 'pdf'"
        :book="bootstrap.book"
        :book-pdf-bytes="bootstrap.bytes"
        :initial-last-read="bootstrap.initialLastRead"
        :initial-selected-block-id="bootstrap.initialSelectedBlockId"
        @update:book="mergeBookIntoBootstrap"
      />
    </template>
  </div>
</template>

<script setup lang="ts">
import BookReadingContent from "@/components/book-reading/BookReadingContent.vue"
import BookReadingEpubView from "@/components/book-reading/BookReadingEpubView.vue"
import ContentLoader from "@/components/commons/ContentLoader.vue"
import { useBookReadingBootstrap } from "@/composables/useBookReadingBootstrap"

const props = defineProps({
  notebookId: { type: Number, required: true },
})

const { book, fileLoading, fileError, bootstrap, mergeBookIntoBootstrap } =
  useBookReadingBootstrap(props.notebookId)
</script>

<style scoped>
.book-reading-page {
  max-height: 100%;
  padding-top: env(safe-area-inset-top, 0px);
}
</style>
