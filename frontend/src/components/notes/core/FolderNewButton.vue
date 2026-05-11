<template>
  <PopButton :title="buttonTitle" :aria-label="ariaLabel ?? buttonTitle">
    <template #button_face>
      <slot />
    </template>
    <template #default="{ closer }">
      <FolderNewForm
        :notebook-id="notebookId"
        :ancestor-folders="ancestorFolders"
        :context-folder="contextFolder"
        @close-dialog="closer"
      />
    </template>
  </PopButton>
</template>

<script setup lang="ts">
import type { Folder } from "@generated/doughnut-backend-api"
import PopButton from "../../commons/Popups/PopButton.vue"
import FolderNewForm from "../FolderNewForm.vue"

defineProps<{
  notebookId: number
  ancestorFolders: Folder[]
  contextFolder: Folder | null
  buttonTitle: string
  ariaLabel?: string
}>()
</script>
