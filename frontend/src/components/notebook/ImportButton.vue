<template>
  <div>
    <button
      class="daisy-btn daisy-btn-ghost daisy-btn-sm"
      @click="triggerFileInput"
      :disabled="isLoading"
      :title="props.notebook ? 'Import notebook' : 'Import notebooks'"
    >
      <i class="bi bi-upload me-1"></i>
      <span v-if="isLoading">Importing...</span>
      <span v-else>{{ props.notebook ? 'Import' : 'Import All' }}</span>
    </button>
    <input
      type="file"
      ref="fileInput"
      accept=".json"
      style="display: none"
      @change="onFileSelected"
    />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useToast } from 'vue-toastification'
import { useBackendApi } from '@/composables/useBackendApi'
import type { Notebook } from '@/generated/backend'

const props = defineProps<{
  notebook?: Notebook
}>()

const api = useBackendApi()
const toast = useToast()
const fileInput = ref<HTMLInputElement | null>(null)
const isLoading = ref(false)

const triggerFileInput = () => {
  if (isLoading.value) return
  fileInput.value?.click()
}

const onFileSelected = async (event: Event) => {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file) return

  isLoading.value = true
  try {
    const reader = new FileReader()
    reader.onload = async (e) => {
      try {
        const rawData = JSON.parse(e.target?.result as string)
        
        // インポートデータのフォーマットを確認
        if (!rawData.metadata || !rawData.notebooks) {
          throw new Error('Invalid import data format')
        }

        const importData = {
          metadata: {
            version: rawData.metadata.version || '1.0.0',
            exportedBy: rawData.metadata.exportedBy || 'Unknown',
            exportedAt: rawData.metadata.exportedAt || new Date().toISOString(),
          },
          notebooks: rawData.notebooks.map((notebook: any) => ({
            title: notebook.title,
            notes: notebook.notes.map((note: any) => ({
              title: note.title,
              details: note.details || '',
            })),
          })),
        }
        
        // 単一のノートブックのインポートかすべてのノートブックのインポートかを判断
        if (props.notebook) {
          await api.importController.importNotebook(importData)
        } else {
          await api.importController.importAllNotebooks(importData)
        }
        
        toast.success('Import completed successfully')
        // ファイル選択をリセット
        if (fileInput.value) {
          fileInput.value.value = ''
        }
      } catch (error) {
        console.error('Import failed:', error)
        toast.error('Failed to import notebook(s)')
      } finally {
        isLoading.value = false
      }
    }
    reader.readAsText(file)
  } catch (error) {
    console.error('File reading failed:', error)
    toast.error('Failed to read file')
    isLoading.value = false
  }
}
</script>

<style scoped>
.import-button {
  display: inline-block;
}
</style> 