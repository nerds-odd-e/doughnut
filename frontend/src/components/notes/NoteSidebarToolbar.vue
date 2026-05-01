<template>
  <nav
    class="daisy-navbar daisy-bg-base-200 daisy-min-h-0 daisy-py-1 daisy-px-1 daisy-sticky daisy-top-0 daisy-z-10"
    data-note-sidebar-toolbar
  >
    <div class="daisy-btn-group daisy-btn-group-sm">
      <NotebookRootNoteNewButton
        :notebook-id="notebookId"
        :target-folder-id="userActiveFolderId"
        button-title="New note"
        aria-label="New note"
      >
        <FilePlus class="w-5 h-5" />
      </NotebookRootNoteNewButton>
      <NoteNewButton
        v-if="note != null && activeNoteTopologyResolved"
        button-title="Add Child Note"
        v-bind="{ referenceNote: note!, insertMode: 'as-child' }"
        aria-label="Add Child Note"
      >
        <FolderPlus class="w-5 h-5" />
      </NoteNewButton>
    </div>
  </nav>
</template>

<script setup lang="ts">
import type { Note } from "@generated/doughnut-backend-api"
import { FilePlus, FolderPlus } from "lucide-vue-next"
import NoteNewButton from "./core/NoteNewButton.vue"
import NotebookRootNoteNewButton from "./core/NotebookRootNoteNewButton.vue"

defineProps<{
  notebookId: number
  note?: Note
  activeNoteTopologyResolved: boolean
  userActiveFolderId: number | null
}>()
</script>
