<template>
  <section
    v-if="book"
    class="settings-section daisy-mb-6"
    data-testid="notebook-attached-book"
  >
    <div class="section-header">
      <h4 class="section-title">{{ book.bookName }}</h4>
      <p class="section-description">
        Open the book reader to browse this notebook's attached PDF structure.
      </p>
    </div>
    <div class="daisy-flex daisy-flex-wrap daisy-gap-2">
      <button
        type="button"
        class="daisy-btn daisy-btn-primary daisy-btn-sm"
        @click="router.push({ name: 'bookReading', params: { notebookId: book.notebookId } })"
      >
        Read
      </button>
      <button
        type="button"
        class="daisy-btn daisy-btn-outline daisy-btn-error daisy-btn-sm"
        title="Remove attached book"
        @click="confirmRemoveBook"
      >
        Remove book
      </button>
    </div>
  </section>

  <section
    v-else-if="bookLoadFinished && !book"
    class="settings-section daisy-mb-6"
    data-testid="notebook-no-book"
  >
    <div class="section-header">
      <h4 class="section-title">Attached book</h4>
      <p class="section-description">
        No book attached to this notebook.
      </p>
    </div>
  </section>
</template>

<script setup lang="ts">
import { ref, watch } from "vue"
import { useRouter } from "vue-router"
import type { BookFull } from "@generated/doughnut-backend-api"
import { NotebookBooksController } from "@generated/doughnut-backend-api/sdk.gen"
import usePopups from "@/components/commons/Popups/usePopups"
import { apiCallWithLoading } from "@/managedApi/clientSetup"

const props = defineProps({
  notebookId: { type: Number, required: true },
})

const router = useRouter()
const { popups } = usePopups()

const book = ref<BookFull | null>(null)
const bookLoadFinished = ref(false)

const loadBook = async () => {
  bookLoadFinished.value = false
  try {
    const { data, error } = await apiCallWithLoading(() =>
      NotebookBooksController.getBook({
        path: { notebook: props.notebookId },
      })
    )
    book.value = !error && data ? data : null
  } finally {
    bookLoadFinished.value = true
  }
}

const confirmRemoveBook = async () => {
  const current = book.value
  if (!current) return
  const name = current.bookName
  if (
    !(await popups.confirm(
      `Remove "${name}" from this notebook? The PDF will be deleted from the server. This cannot be undone.`
    ))
  ) {
    return
  }
  const { error } = await apiCallWithLoading(() =>
    NotebookBooksController.deleteBook({
      path: { notebook: props.notebookId },
    })
  )
  if (!error) {
    await loadBook()
  }
}

watch(
  () => props.notebookId,
  async () => {
    await loadBook()
  },
  { immediate: true }
)
</script>

<style scoped>
.settings-section {
  background: oklch(var(--b1));
  border: 1px solid oklch(var(--b3));
  border-radius: 8px;
  padding: 1.5rem;
}

.section-header {
  margin-bottom: 1.25rem;
}

.section-title {
  font-size: 1.125rem;
  font-weight: 600;
  margin-bottom: 0.5rem;
  color: oklch(var(--bc));
}

.section-description {
  font-size: 0.875rem;
  color: oklch(var(--bc) / 0.7);
  line-height: 1.5;
}
</style>
