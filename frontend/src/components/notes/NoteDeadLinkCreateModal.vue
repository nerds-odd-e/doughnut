<template>
  <Modal v-if="modelValue !== null" @close_request="close">
    <template #body>
      <NoteNewDialog
        :reference-note="referenceNote"
        :insert-mode="insertMode"
        :initial-title="modelValue"
        @close-dialog="close"
      />
    </template>
  </Modal>
</template>

<script setup lang="ts">
import type { Note } from "@generated/doughnut-backend-api"
import type { InsertMode } from "@/models/InsertMode"
import Modal from "@/components/commons/Modal.vue"
import NoteNewDialog from "./NoteNewDialog.vue"

defineProps<{
  referenceNote: Note
  insertMode: InsertMode
  modelValue: string | null
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
