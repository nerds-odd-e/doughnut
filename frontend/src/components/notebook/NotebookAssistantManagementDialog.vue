<template>
  <h3>Notebook Assistant Management</h3>

  <!-- AI Instructions Form -->
  <form @submit.prevent="updateAiInstructions">
    <TextInput
      v-model="additionalInstruction"
      field="additionalInstruction"
      label="Additional Instructions to AI"
    />
    <button type="submit">Update Notebook AI Assistant Settings</button>
  </form>

  <div class="mt-4">
    <button @click.prevent="createAssistantForNotebook">
      Create Assistant For Notebook
    </button>
    <button @click.prevent="downloadNotebookDump">
      Download Notebook Dump
    </button>
  </div>
</template>

<script setup lang="ts">
import type { Notebook } from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import type { PropType } from "vue"
import { ref } from "vue"
import { saveAs } from "file-saver"
import TextInput from "../form/TextInput.vue"

const { managedApi } = useLoadingApi()
const props = defineProps({
  notebook: { type: Object as PropType<Notebook>, required: true },
})

const additionalInstruction = ref("")

const emit = defineEmits(["close"])

const updateAiInstructions = async () => {
  await managedApi.restNotebookController.updateAiAssistant(
    props.notebook.id,
    additionalInstruction.value
  )
}

const createAssistantForNotebook = async () => {
  await managedApi.restAiAssistantCreationController.recreateNotebookAssistant(
    props.notebook.id,
    {
      additionalInstruction: additionalInstruction.value,
    }
  )
  emit("close")
}

const downloadNotebookDump = async () => {
  const notes = await managedApi.restNotebookController.downloadNotebookDump(
    props.notebook.id
  )
  const blob = new Blob([JSON.stringify(notes, null, 2)], {
    type: "application/json",
  })
  saveAs(blob, "notebook-dump.json")
}
</script>

<style scoped>
.mt-4 {
  margin-top: 1rem;
}
</style>
