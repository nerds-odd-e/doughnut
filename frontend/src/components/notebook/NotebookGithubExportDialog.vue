<template>
  <div class="daisy-modal-box">
    <h3 class="daisy-font-bold daisy-text-lg">Export to GitHub</h3>
    <form @submit.prevent="handleSubmit" class="daisy-form-control">
      <div class="daisy-my-4">
        <label class="daisy-label">Repository Name</label>
        <input
          v-model="repositoryName"
          type="text"
          placeholder="Enter repository name"
          class="daisy-input daisy-input-bordered daisy-w-full"
          data-cy="repository-input"
          required
        />
      </div>
      <div class="daisy-modal-action">
        <button 
          type="button" 
          class="daisy-btn" 
          @click="$emit('close-dialog')"
        >Cancel</button>
        <button 
          type="submit" 
          class="daisy-btn daisy-btn-primary"
          :disabled="isLoading"
        >
          {{ isLoading ? 'Exporting...' : 'Export' }}
        </button>
      </div>
    </form>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue"
import { useToast } from "@/composables/useToast"

const emit = defineEmits<{
  (e: "close-dialog"): void
}>()

const { showSuccessToast, showErrorToast } = useToast()

const repositoryName = ref("")
const isLoading = ref(false)

const handleSubmit = async () => {
  isLoading.value = true
  try {
    showSuccessToast("Notebook exported to GitHub successfully")
    emit("close-dialog")
  } catch (error) {
    showErrorToast(
      error instanceof Error ? error.message : "Failed to export to GitHub"
    )
  } finally {
    isLoading.value = false
  }
}
</script> 