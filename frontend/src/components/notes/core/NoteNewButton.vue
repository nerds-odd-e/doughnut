<template>
  <PopButton
    ref="popButtonRef"
    :title="noteMoreOptionsTitles.new"
    :aria-label="noteMoreOptionsTitles.new"
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
import type { FolderTrailSegment, Note } from "@generated/donut-backend-api"
import NoteNewForm from "../NoteNewForm.vue"
import { useKeyboardShortcut } from "@/composables/useKeyboardShortcut"
import { noteMoreOptionsTitles } from "../widgets/noteMoreOptionsTitles"
import { ref } from "vue"

defineProps<{
  notebookId: number
  /** Resolved parent folder for create dialog (sidebar selection or active note folder). */
  initialFolder?: FolderTrailSegment
  titleSearchAnchorNote?: Note | null
  ancestorFolders?: FolderTrailSegment[]
  initialTitle?: string
}>()

const popButtonRef = ref<InstanceType<typeof PopButton> | null>(null)

useKeyboardShortcut("note-new", () => {
  popButtonRef.value?.openDialog()
})

defineExpose({
  openDialog: () => popButtonRef.value?.openDialog(),
})
</script>
