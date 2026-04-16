<template>
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
      data-testid="book-reading-epub-global-bar-title"
      :title="book.bookName"
    >
      {{ book.bookName }}
    </span>
    <span class="daisy-ml-auto daisy-shrink-0" aria-hidden="true" />
  </GlobalBar>
  <BookReadingBookLayout
    v-model:opened="bookLayoutOpened"
    :panel-id="bookReadingBookLayoutPanelId"
    :is-md-or-larger="isMdOrLarger"
    :blocks="book.blocks"
    :current-block-id="null"
    :selected-block-id="null"
    :disposition-for-block="noDisposition"
    side-drawer-mode="titleOnly"
    :side-drawer-title="book.bookName"
  >
    <main
      class="daisy-flex daisy-flex-1 daisy-min-h-0 daisy-min-w-0 daisy-flex-col daisy-items-center daisy-justify-center daisy-px-4 daisy-py-8"
    >
      <p
        data-testid="book-reading-epub-placeholder"
        class="daisy-text-center daisy-text-base-content/70 daisy-text-sm"
      >
        EPUB reading view is not available yet.
      </p>
    </main>
  </BookReadingBookLayout>
</template>

<script setup lang="ts">
import BookLayoutToggleButton from "@/components/book-reading/BookLayoutToggleButton.vue"
import BookReadingBookLayout from "@/components/book-reading/BookReadingBookLayout.vue"
import GlobalBar from "@/components/toolbars/GlobalBar.vue"
import type { BookFull } from "@generated/doughnut-backend-api"
import { computed, onBeforeUnmount, onMounted, ref } from "vue"

const BOOK_READING_LAYOUT_BREAKPOINT_PX = 768
const bookReadingBookLayoutPanelId = "book-reading-book-layout-panel"

const props = defineProps<{
  book: BookFull
}>()

const notebookId = computed(() => Number(props.book.notebookId))

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

function noDisposition(_blockId: number) {
  return undefined
}

onMounted(() => {
  window.addEventListener("resize", handleResize)
  if (windowWidth.value >= BOOK_READING_LAYOUT_BREAKPOINT_PX) {
    bookLayoutOpened.value = true
  }
})

onBeforeUnmount(() => {
  window.removeEventListener("resize", handleResize)
})
</script>
