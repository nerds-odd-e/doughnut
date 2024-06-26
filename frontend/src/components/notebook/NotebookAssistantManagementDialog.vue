<template>
  <h3>Notebook Assistant Management</h3>
  <button @click.prevent="createAssistantForNotebook">Create Assistant For Notebook</button>
</template>

<script setup lang="ts">
import { Notebook } from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import { PropType } from "vue"

const { managedApi } = useLoadingApi()
const props = defineProps({
  notebook: { type: Object as PropType<Notebook>, required: true },
})

const emit = defineEmits(["close"])

const createAssistantForNotebook = async () => {
  await managedApi.restAiController.recreateNotebookAssistant(props.notebook.id)
  emit("close")
}
</script>
