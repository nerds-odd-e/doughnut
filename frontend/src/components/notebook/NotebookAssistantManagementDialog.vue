<template>
  <h4 class="daisy-text-lg">Notebook Assistant Management</h4>

  <!-- AI Instructions Form -->
  <form @submit.prevent="updateAiInstructions">
    <TextInput
      v-model="additionalInstruction"
      field="additionalInstruction"
      label="Additional Instructions to AI"
    />
    <button type="submit" class="daisy-btn daisy-btn-primary">
      Update Notebook AI Assistant Settings
    </button>
  </form>

  <div class="daisy-mt-4">
    <button @click.prevent="downloadNotebookDump" class="daisy-btn daisy-btn-success">
      Download Notebook Dump
    </button>
  </div>
</template>

<script setup lang="ts">
import type { Notebook } from "@generated/backend"
import { NotebookController } from "@generated/backend/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import { useToast } from "@/composables/useToast"
import type { PropType } from "vue"
import { ref, watch } from "vue"
import { saveAs } from "file-saver"
import TextInput from "../form/TextInput.vue"

const props = defineProps({
  notebook: { type: Object as PropType<Notebook>, required: true },
  additionalInstructions: { type: String, default: "" },
})

const additionalInstruction = ref(props.additionalInstructions)

const { showSuccessToast } = useToast()

// Update when prop changes
watch(
  () => props.additionalInstructions,
  (newInstructions) => {
    additionalInstruction.value = newInstructions
  },
  { immediate: true }
)

const updateAiInstructions = async () => {
  const { error } = await apiCallWithLoading(() =>
    NotebookController.updateAiAssistant({
      path: { notebook: props.notebook.id },
      body: {
        additionalInstructions: additionalInstruction.value,
      },
    })
  )
  if (!error) {
    showSuccessToast("Notebook AI assistant settings updated successfully")
  }
}

const downloadNotebookDump = async () => {
  const { data: notes, error } = await NotebookController.downloadNotebookDump({
    path: { notebook: props.notebook.id },
  })
  if (!error && notes) {
    const blob = new Blob([JSON.stringify(notes, null, 2)], {
      type: "application/json",
    })
    saveAs(blob, "notebook-dump.json")
  }
}
</script>

<style scoped>
</style>
