<template>
  <section class="bg-base-100 border border-base-300 rounded-lg p-6 mb-6">
    <div class="mb-5">
      <h4 class="text-lg font-semibold mb-2 text-base-content">
        Notebook Indexing
      </h4>
      <p class="text-sm text-base-content/70 leading-normal">
        Manage the search index for this notebook. Reset to rebuild from scratch, or update to index new content.
      </p>
    </div>
    <div class="flex flex-wrap gap-2">
      <button
        class="daisy-btn daisy-btn-outline daisy-btn-sm"
        @click="reindexNotebook"
        :disabled="isIndexing"
      >
        <span v-if="isIndexing">Working...</span>
        <span v-else>Reset notebook index</span>
      </button>
      <button
        class="daisy-btn daisy-btn-outline daisy-btn-sm"
        @click="updateIndexNotebook"
        :disabled="isIndexing"
      >
        <span v-if="isIndexing">Working...</span>
        <span v-else>Update index</span>
      </button>
    </div>
  </section>
</template>

<script setup lang="ts">
import { ref } from "vue"
import { NotebookController } from "@generated/donut-backend-api/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import { useToast } from "@/composables/useToast"

const props = defineProps({
  notebookId: { type: Number, required: true },
})

const { showSuccessToast } = useToast()
const isIndexing = ref(false)

const reindexNotebook = async () => {
  isIndexing.value = true
  const { error } = await apiCallWithLoading(() =>
    NotebookController.resetNotebookIndex({
      path: { notebook: props.notebookId },
    })
  )
  if (!error) {
    showSuccessToast("Notebook index reset successfully")
  }
  isIndexing.value = false
}

const updateIndexNotebook = async () => {
  isIndexing.value = true
  const { error } = await apiCallWithLoading(() =>
    NotebookController.updateNotebookIndex({
      path: { notebook: props.notebookId },
    })
  )
  if (!error) {
    showSuccessToast("Notebook index updated successfully")
  }
  isIndexing.value = false
}
</script>
