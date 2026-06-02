<template>
  <span data-testid="note-creation-new-button" class="contents">
    <NoteNewButton
      :notebook-id="notebookId"
      :initial-folder="parentFolderForCreation ?? undefined"
      :title-search-anchor-note="anchorNote"
      :ancestor-folders="breadcrumbFolders"
      :initial-title="initialTitle"
    >
      <NotebookPen class="w-6 h-6" />
    </NoteNewButton>
  </span>
</template>

<script setup lang="ts">
import type {
  Folder,
  FolderRealm,
  NoteRealm,
} from "@generated/doughnut-backend-api"
import { NotebookPen } from "@lucide/vue"
import NoteNewButton from "./core/NoteNewButton.vue"
import { useNoteCreationToolbarContext } from "@/composables/useNoteCreationToolbarContext"

const props = withDefaults(
  defineProps<{
    notebookId: number
    activeNoteRealm?: NoteRealm
    activeFolderRealm?: FolderRealm
    breadcrumbFolders?: Folder[]
  }>(),
  { breadcrumbFolders: () => [] }
)

const { initialTitle, parentFolderForCreation, anchorNote } =
  useNoteCreationToolbarContext(() => ({
    activeNoteRealm: props.activeNoteRealm,
    activeFolderRealm: props.activeFolderRealm,
  }))
</script>
