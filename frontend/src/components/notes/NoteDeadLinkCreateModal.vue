<template>
  <Modal v-if="modelValue !== null" @close_request="close">
    <template #body>
      <NoteNewDialog
        :notebook-root-notebook-id="notebookId"
        :target-folder-id="folderId ?? undefined"
        :initial-title="modelValue"
        :wiki-title-cache-refresh-source-note-id="sourceNoteId"
        @close-dialog="close"
      />
    </template>
  </Modal>
</template>

<script setup lang="ts">
import Modal from "@/components/commons/Modal.vue"
import NoteNewDialog from "./NoteNewDialog.vue"

defineProps<{
  notebookId: number
  folderId?: number | null
  modelValue: string | null
  sourceNoteId: number
}>()

const emit = defineEmits<{
  "update:modelValue": [value: string | null]
  closed: []
}>()

const close = () => {
  emit("update:modelValue", null)
  emit("closed")
}
</script>
