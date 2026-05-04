<template>
  <nav
    class="daisy-navbar daisy-bg-base-200 daisy-min-h-0 daisy-py-1 daisy-px-1 daisy-sticky daisy-top-0 daisy-z-10"
    data-note-sidebar-toolbar
  >
    <div class="daisy-btn-group daisy-btn-group-sm">
      <NotebookRootNoteNewButton
        :notebook-id="notebookId"
        :target-folder-id="userActiveFolderId"
        :title-search-anchor-note="note"
        button-title="New note"
        aria-label="New note"
      >
        <FilePlus class="w-5 h-5" />
      </NotebookRootNoteNewButton>
      <FolderNewButton
        :notebook-id="notebookId"
        :under-folder-id="folderUnderFolderId"
        :under-note-id="folderContextNoteId"
        button-title="New folder"
        aria-label="New folder"
      >
        <FolderPlus class="w-5 h-5" />
      </FolderNewButton>
    </div>
  </nav>
</template>

<script setup lang="ts">
import type { Note } from "@generated/doughnut-backend-api"
import { FilePlus, FolderPlus } from "lucide-vue-next"
import { computed } from "vue"
import FolderNewButton from "./core/FolderNewButton.vue"
import NotebookRootNoteNewButton from "./core/NotebookRootNoteNewButton.vue"

const props = defineProps<{
  notebookId: number
  note?: Note
  activeNoteTopologyResolved: boolean
  userActiveFolderId: number | null
}>()

const folderUnderFolderId = computed(() =>
  props.userActiveFolderId != null ? props.userActiveFolderId : undefined
)

const folderContextNoteId = computed(() =>
  props.userActiveFolderId != null
    ? undefined
    : props.note != null && props.activeNoteTopologyResolved
      ? props.note.id
      : undefined
)
</script>
