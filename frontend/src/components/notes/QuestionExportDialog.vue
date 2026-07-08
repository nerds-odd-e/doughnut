<template>
  <AiRequestExportDialog
    title="Export Question Generation Request for ChatGPT"
    :fetch-export="fetchQuestionExport"
    @close="$emit('close')"
  />
</template>

<script setup lang="ts">
import AiRequestExportDialog from "@/components/commons/AiRequestExportDialog.vue"
import { PredefinedQuestionController } from "@generated/doughnut-backend-api/sdk.gen"
import {} from "@/managedApi/clientSetup"

const props = defineProps<{
  noteId: number
}>()

defineEmits<{
  (e: "close"): void
}>()

async function fetchQuestionExport() {
  const { data: response, error } =
    await PredefinedQuestionController.exportQuestionGeneration({
      path: { note: props.noteId },
    })
  if (!error && response) {
    return response
  }
  return null
}
</script>
