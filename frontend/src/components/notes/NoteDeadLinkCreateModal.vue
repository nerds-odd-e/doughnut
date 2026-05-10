<template>
  <Modal v-if="modelValue !== null" @close_request="close">
    <template #body>
      <NoteNewForm
        :notebook-root-notebook-id="notebookId"
        :initial-folder="resolvedCreateParentFolder ?? undefined"
        :parent-location-description="createParentLocationDescription"
        :initial-title="modelValue"
        :wiki-title-cache-refresh-source-note-id="sourceNoteId"
        :ancestor-folders="noteRealm.ancestorFolders ?? []"
        @close-dialog="close"
      />
    </template>
  </Modal>
</template>

<script setup lang="ts">
import type { NoteRealm } from "@generated/doughnut-backend-api"
import { computed } from "vue"
import Modal from "@/components/commons/Modal.vue"
import { notebookSidebarActiveFolder } from "@/composables/useCurrentNoteSidebarState"
import { useNotebookRootCreateTarget } from "./useNoteSidebarTree"
import NoteNewForm from "./NoteNewForm.vue"

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

const { resolvedCreateParentFolder, createParentLocationDescription } =
  useNotebookRootCreateTarget(
    notebookSidebarActiveFolder,
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
