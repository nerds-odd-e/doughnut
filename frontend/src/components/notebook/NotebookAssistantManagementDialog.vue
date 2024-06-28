<template>
  <h3>Notebook Assistant Management</h3>
  <TextInput v-model="additionalInstruction" field="additionalInstruction" />
  <button @click.prevent="createAssistantForNotebook">Create Assistant For Notebook</button>
  <button @click.prevent="downloadNotebookDump">Download Notebook Dump</button>
</template>

<script setup lang="ts">
import { Notebook } from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import { PropType, ref } from "vue"
import TextInput from "../form/TextInput.vue"

const { managedApi } = useLoadingApi()
const props = defineProps({
  notebook: { type: Object as PropType<Notebook>, required: true },
})

const additionalInstruction = ref("")

const emit = defineEmits(["close"])

const createAssistantForNotebook = async () => {
  await managedApi.restAiController.recreateNotebookAssistant(props.notebook.id, {
    additionalInstruction: additionalInstruction.value,
  })
  emit("close")
}

const downloadNotebookDump = async () => {
  const notes = await managedApi.restNotebookController.downloadNotebookDump(props.notebook.id)
  const blob = new Blob([JSON.stringify(notes, null, 2)], { type: 'application/json' })

  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = 'notebook-dump.json'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}
</script>
