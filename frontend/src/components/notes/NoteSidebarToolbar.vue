<template>
  <nav class="daisy-navbar daisy-bg-base-200 daisy-min-h-0 daisy-py-1 daisy-px-1 daisy-sticky daisy-top-0 daisy-z-10">
    <div class="daisy-btn-group daisy-btn-group-sm">
      <NotebookRootNoteNewButton
        v-if="preferNotebookRootCreation"
        :notebook-id="notebookId"
        button-title="Add note"
      >
        <FolderPlus class="w-5 h-5" />
      </NotebookRootNoteNewButton>
      <NoteNewButton
        v-if="note != null && topologyHeadResolved"
        button-title="Add Child Note"
        v-bind="{ referenceNote: note!, insertMode: 'as-child' }"
      >
        <FolderPlus class="w-5 h-5" />
      </NoteNewButton>
      <NoteNewButton
        v-if="note?.parentId && topologyHeadResolved"
        button-title="Add Next Sibling Note"
        v-bind="{ referenceNote: note!, insertMode: 'after' }"
      >
        <Folders class="w-5 h-5" />
      </NoteNewButton>
    </div>
  </nav>
</template>

<script setup lang="ts">
import type { Note } from "@generated/doughnut-backend-api"
import { computed } from "vue"
import { FolderPlus, Folders } from "lucide-vue-next"
import NoteNewButton from "./core/NoteNewButton.vue"
import NotebookRootNoteNewButton from "./core/NotebookRootNoteNewButton.vue"

const props = defineProps<{
  notebookId: number
  note?: Note
  topologyHeadResolved: boolean
}>()

const preferNotebookRootCreation = computed(() => !props.topologyHeadResolved)
</script>
