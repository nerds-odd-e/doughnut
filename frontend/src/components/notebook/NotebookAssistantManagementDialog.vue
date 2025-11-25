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
import {
  updateAiAssistant,
  downloadNotebookDump as downloadNotebookDumpApi,
  getAiAssistant,
} from "@generated/backend/sdk.gen"
import type { PropType } from "vue"
import { ref, onMounted } from "vue"
import { saveAs } from "file-saver"
import TextInput from "../form/TextInput.vue"

const props = defineProps({
  notebook: { type: Object as PropType<Notebook>, required: true },
})

const additionalInstruction = ref("")

const emit = defineEmits(["close"])

const updateAiInstructions = async () => {
  const { error } = await updateAiAssistant({
    path: { notebook: props.notebook.id },
    body: {
      additionalInstructions: additionalInstruction.value,
    },
  })
  if (!error) {
    // Success - handled by global interceptor
  }
}

const downloadNotebookDump = async () => {
  const { data: notes, error } = await downloadNotebookDumpApi({
    path: { notebook: props.notebook.id },
  })
  if (!error && notes) {
    const blob = new Blob([JSON.stringify(notes, null, 2)], {
      type: "application/json",
    })
    saveAs(blob, "notebook-dump.json")
  }
}

const loadCurrentSettings = async () => {
  const { data: assistant, error } = await getAiAssistant({
    path: { notebook: props.notebook.id },
  })
  if (!error && assistant) {
    additionalInstruction.value = assistant.additionalInstructionsToAi || ""
  }
}

onMounted(async () => {
  await loadCurrentSettings()
})
</script>

<style scoped>
</style>
