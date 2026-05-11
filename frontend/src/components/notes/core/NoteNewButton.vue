<template>
  <PopButton
    :title="buttonTitle"
    :aria-label="ariaLabel ?? buttonTitle"
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
        :default-title-from-scoped-pattern="defaultTitleFromScopedPattern"
        @close-dialog="closer"
      />
    </template>
  </PopButton>
</template>

<script setup lang="ts">
import PopButton from "../../commons/Popups/PopButton.vue"
import type { Folder, Note } from "@generated/doughnut-backend-api"
import { useDefaultNewNoteTitleFromPattern } from "@/composables/useScopedTitlePatternForNewNote"
import NoteNewForm from "../NoteNewForm.vue"

defineProps<{
  notebookId: number
  buttonTitle: string
  ariaLabel?: string
  /** Resolved parent folder for create dialog (sidebar selection or active note folder). */
  initialFolder?: Folder
  titleSearchAnchorNote?: Note | null
  ancestorFolders?: Folder[]
}>()

const defaultTitleFromScopedPattern = useDefaultNewNoteTitleFromPattern()
</script>
