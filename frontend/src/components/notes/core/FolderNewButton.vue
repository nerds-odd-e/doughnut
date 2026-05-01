<template>
  <PopButton :title="buttonTitle" :aria-label="ariaLabel ?? buttonTitle">
    <template #button_face>
      <slot />
    </template>
    <template #default="{ closer }">
      <FolderNewDialog
        :notebook-id="notebookId"
        :under-note-id="underNoteId"
        @close-dialog="closer"
      />
    </template>
  </PopButton>
</template>

<script setup lang="ts">
import PopButton from "../../commons/Popups/PopButton.vue"
import FolderNewDialog from "../FolderNewDialog.vue"

defineProps<{
  notebookId: number
  underNoteId?: number
  buttonTitle: string
  ariaLabel?: string
}>()
</script>
