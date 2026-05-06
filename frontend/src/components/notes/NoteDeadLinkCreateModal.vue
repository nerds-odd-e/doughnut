<template>
  <Modal v-if="modelValue !== null" @close_request="close">
    <template #body>
      <NoteNewDialog
        :notebook-root-notebook-id="notebookId"
        :target-folder-id="resolvedCreateParentFolderId ?? undefined"
        :parent-location-description="createParentLocationDescription"
        :initial-title="modelValue"
        :wiki-title-cache-refresh-source-note-id="sourceNoteId"
        @close-dialog="close"
      />
    </template>
  </Modal>
</template>

<script setup lang="ts">
import type { NoteRealm } from "@generated/doughnut-backend-api"
import { computed } from "vue"
import Modal from "@/components/commons/Modal.vue"
import { notebookSidebarUserActiveFolder } from "@/composables/useCurrentNoteSidebarState"
import { useNotebookRootCreateTarget } from "./useNoteSidebarTree"
import NoteNewDialog from "./NoteNewDialog.vue"

const props = defineProps<{
  notebookId: number
  noteRealm: NoteRealm
  modelValue: string | null
  sourceNoteId: number
}>()

const activeNoteRealmRef = computed(() => props.noteRealm)
const noteContextResolved = computed(
  () => props.noteRealm.note?.noteTopology != null
)

const { resolvedCreateParentFolderId, createParentLocationDescription } =
  useNotebookRootCreateTarget(
    notebookSidebarUserActiveFolder,
    activeNoteRealmRef,
    noteContextResolved
  )

const emit = defineEmits<{
  "update:modelValue": [value: string | null]
  closed: []
}>()

const close = () => {
  emit("update:modelValue", null)
  emit("closed")
}
</script>
