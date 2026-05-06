<template>
  <PopButton
    title="Folder…"
    aria-label="Folder…"
    align-modal-top
    btn-class="daisy-btn daisy-btn-ghost daisy-btn-sm"
  >
    <template #button_face>
      <slot />
    </template>
    <template #default="{ closer }">
      <FolderOrganizeDialog
        :notebook-id="notebookId"
        :moving-folder-id="movingFolderId"
        :moving-folder-name="movingFolderName"
        @close-dialog="closer"
      />
    </template>
  </PopButton>
</template>

<script setup lang="ts">
import PopButton from "../../commons/Popups/PopButton.vue"
import FolderOrganizeDialog from "../FolderOrganizeDialog.vue"

defineProps<{
  notebookId: number
  movingFolderId: number
  movingFolderName: string
}>()
</script>
