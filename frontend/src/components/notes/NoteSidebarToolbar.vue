<template>
  <nav
    :class="[noteChromeToolbarNavClass, noteSidebarToolbarNavStickyClass]"
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
        <NotebookPen class="w-5 h-5" />
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
import { FolderPlus, NotebookPen } from "lucide-vue-next"
import { computed } from "vue"
import FolderNewButton from "./core/FolderNewButton.vue"
import NotebookRootNoteNewButton from "./core/NotebookRootNoteNewButton.vue"
import {
  noteChromeToolbarNavClass,
  noteSidebarToolbarNavStickyClass,
} from "./noteChromeToolbarNavClass"

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
