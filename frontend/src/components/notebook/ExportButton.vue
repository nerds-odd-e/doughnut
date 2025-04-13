<template>
  <button
    class="daisy-btn daisy-btn-ghost daisy-btn-sm"
    @click="handleExport"
    :disabled="isLoading"
    :title="notebook ? 'Export notebook' : 'Export all notebooks'"
  >
    <i class="bi bi-download me-1"></i>
    <span v-if="isLoading">Exporting...</span>
    <span v-else>{{ notebook ? 'Export' : 'Export All' }}</span>
  </button>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useToast } from 'vue-toastification'
import type { Notebook } from '@/generated/backend'
import { useBackendApi } from '@/composables/useBackendApi'

const props = defineProps<{
  notebook?: Notebook
}>()

const toast = useToast()
const isLoading = ref(false)
const api = useBackendApi()

const handleExport = async () => {
  try {
    isLoading.value = true
    const response = props.notebook
      ? await api.exportController.exportNotebook(props.notebook.id)
      : await api.exportController.exportAllNotebooks()

    if (!response) {
      throw new Error('No data received')
    }

    // Create and download file
    const blob = new Blob([JSON.stringify(response, null, 2)], {
      type: 'application/json',
    })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = props.notebook
      ? `notebook-${props.notebook.id}.json`
      : 'all-notebooks.json'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)

    toast.success('Export completed successfully')
  } catch (error) {
    console.error('Export failed:', error)
    toast.error('Failed to export notebook(s)')
  } finally {
    isLoading.value = false
  }
}
</script>

<style scoped>
.export-button {
  display: inline-block;
}
</style> 