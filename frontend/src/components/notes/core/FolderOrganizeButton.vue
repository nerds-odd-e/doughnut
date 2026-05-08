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
      <FolderOrganizeForm
        :notebook-id="notebookId"
        :moving-folder-id="movingFolderId"
        :moving-folder-name="movingFolderName"
        :ancestor-folders="ancestorFolders"
        @close-dialog="closer"
      />
    </template>
  </PopButton>
</template>

<script setup lang="ts">
import type { Folder } from "@generated/doughnut-backend-api"
import PopButton from "../../commons/Popups/PopButton.vue"
import FolderOrganizeForm from "../FolderOrganizeForm.vue"

defineProps<{
  notebookId: number
  movingFolderId: number
  movingFolderName: string
  ancestorFolders: Folder[]
}>()
</script>
