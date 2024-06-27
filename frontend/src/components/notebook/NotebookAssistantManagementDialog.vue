<template>
  <h3>Notebook Assistant Management</h3>
  <TextInput v-model="additionalInstruction" field="additionalInstruction" />
  <button @click.prevent="createAssistantForNotebook">Create Assistant For Notebook</button>
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
</script>
