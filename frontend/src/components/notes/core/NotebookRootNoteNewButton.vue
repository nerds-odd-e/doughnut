<template>
  <PopButton :title="buttonTitle" :aria-label="ariaLabel ?? buttonTitle">
    <template #button_face>
      <slot />
    </template>
    <template #default="{ closer }">
      <NoteNewDialog
        :notebook-root-notebook-id="notebookId"
        :target-folder-id="targetFolderId ?? undefined"
        :parent-location-description="parentLocationDescription"
        :title-search-anchor-note="titleSearchAnchorNote ?? undefined"
        @close-dialog="closer"
      />
    </template>
  </PopButton>
</template>

<script setup lang="ts">
import PopButton from "../../commons/Popups/PopButton.vue"
import type { Note } from "@generated/doughnut-backend-api"
import NoteNewDialog from "../NoteNewDialog.vue"

defineProps<{
  notebookId: number
  buttonTitle: string
  ariaLabel?: string
  targetFolderId?: number | null
  parentLocationDescription?: string
  titleSearchAnchorNote?: Note | null
}>()
</script>
