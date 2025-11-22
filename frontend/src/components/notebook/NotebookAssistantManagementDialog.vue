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
import useLoadingApi from "@/managedApi/useLoadingApi"
import type { PropType } from "vue"
import { ref, onMounted } from "vue"
import { saveAs } from "file-saver"
import TextInput from "../form/TextInput.vue"

const { managedApi } = useLoadingApi()
const props = defineProps({
  notebook: { type: Object as PropType<Notebook>, required: true },
})

const additionalInstruction = ref("")

const emit = defineEmits(["close"])

const updateAiInstructions = async () => {
  await managedApi.services.updateAiAssistant({
    path: { notebook: props.notebook.id },
    body: {
      additionalInstructions: additionalInstruction.value,
    },
  })
}

const downloadNotebookDump = async () => {
  const notes = await managedApi.services.downloadNotebookDump({
    path: { notebook: props.notebook.id },
  })
  const blob = new Blob([JSON.stringify(notes, null, 2)], {
    type: "application/json",
  })
  saveAs(blob, "notebook-dump.json")
}

const loadCurrentSettings = async () => {
  try {
    const assistant = await managedApi.services.getAiAssistant({
      path: { notebook: props.notebook.id },
    })
    if (assistant) {
      additionalInstruction.value = assistant.additionalInstructionsToAi || ""
    }
  } catch (error) {
    console.error("Failed to load AI assistant settings:", error)
  }
}

onMounted(async () => {
  await loadCurrentSettings()
})
</script>

<style scoped>
</style>
