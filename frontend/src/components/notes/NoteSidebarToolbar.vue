<template>
  <nav :class="noteChromeToolbarNavClass" data-note-sidebar-toolbar>
    <div class="daisy-btn-group daisy-btn-group-sm">
      <NotebookRootNoteNewButton
        :notebook-id="notebookId"
        :target-folder-id="resolvedCreateParentFolderId ?? undefined"
        :parent-location-description="createParentLocationDescription"
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
        :parent-location-description="createParentLocationDescription"
        button-title="New folder"
        aria-label="New folder"
      >
        <FolderPlus class="w-5 h-5" />
      </FolderNewButton>
      <FolderOrganizeButton
        v-if="userActiveFolder != null"
        :notebook-id="notebookId"
        :moving-folder-id="userActiveFolder.id"
        :moving-folder-name="userActiveFolder.name"
      >
        <FolderInput class="w-5 h-5" />
      </FolderOrganizeButton>
    </div>
  </nav>
</template>

<script setup lang="ts">
import type { Note } from "@generated/doughnut-backend-api"
import { FolderInput, FolderPlus, NotebookPen } from "lucide-vue-next"
import { computed } from "vue"
import FolderNewButton from "./core/FolderNewButton.vue"
import FolderOrganizeButton from "./core/FolderOrganizeButton.vue"
import NotebookRootNoteNewButton from "./core/NotebookRootNoteNewButton.vue"
import { noteChromeToolbarNavClass } from "./noteChromeToolbarNavClass"
import type { SidebarUserActiveFolder } from "./useNoteSidebarTree"

const props = defineProps<{
  notebookId: number
  note?: Note
  activeNoteTopologyResolved: boolean
  /** Parent folder for new note / new folder (active sidebar folder, else active note's folder). */
  resolvedCreateParentFolderId: number | null
  createParentLocationDescription: string
  userActiveFolder: SidebarUserActiveFolder | null
}>()

const folderUnderFolderId = computed(() =>
  props.resolvedCreateParentFolderId != null
    ? props.resolvedCreateParentFolderId
    : undefined
)

const folderContextNoteId = computed(() =>
  props.resolvedCreateParentFolderId != null
    ? undefined
    : props.note != null && props.activeNoteTopologyResolved
      ? props.note.id
      : undefined
)
</script>
