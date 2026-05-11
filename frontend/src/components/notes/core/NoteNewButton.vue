<template>
  <PopButton
    title="New note"
    aria-label="New note"
    align-modal-top
  >
    <template #button_face>
      <slot />
    </template>
    <template #default="{ closer }">
      <NoteNewForm
        :notebookId="notebookId"
        :initial-folder="initialFolder"
        :title-search-anchor-note="titleSearchAnchorNote ?? undefined"
        :ancestor-folders="ancestorFolders ?? []"
        :initial-title="initialTitle"
        @close-dialog="closer"
      />
    </template>
  </PopButton>
</template>

<script setup lang="ts">
import PopButton from "../../commons/Popups/PopButton.vue"
import type { Folder, Note } from "@generated/doughnut-backend-api"
import NoteNewForm from "../NoteNewForm.vue"

defineProps<{
  notebookId: number
  /** Resolved parent folder for create dialog (sidebar selection or active note folder). */
  initialFolder?: Folder
  titleSearchAnchorNote?: Note | null
  ancestorFolders?: Folder[]
  initialTitle?: string
}>()
</script>
