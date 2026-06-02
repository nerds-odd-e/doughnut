<template>
  <span data-testid="note-creation-new-button" class="contents">
    <NoteNewButton
      ref="noteNewButtonRef"
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
import { onMounted, onUnmounted, ref } from "vue"
import NoteNewButton from "./core/NoteNewButton.vue"
import { useNoteCreationToolbarContext } from "@/composables/useNoteCreationToolbarContext"
import {
  registerGlobalNoteNewOpener,
  unregisterGlobalNoteNewOpener,
} from "@/utils/globalKeyboardShortcut"

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

const noteNewButtonRef = ref<InstanceType<typeof NoteNewButton> | null>(null)

function openNewNoteDialog() {
  noteNewButtonRef.value?.openDialog()
}

onMounted(() => {
  registerGlobalNoteNewOpener(openNewNoteDialog)
})

onUnmounted(() => {
  unregisterGlobalNoteNewOpener(openNewNoteDialog)
})
</script>
