<template>
  <PopButton :title="buttonTitle">
    <template #button_face>
      <slot />
    </template>
    <template #default="{ closer }">
      <NoteNewDialog
        :notebook-root-notebook-id="notebookId"
        @close-dialog="closer"
      />
    </template>
  </PopButton>
</template>

<script setup lang="ts">
import PopButton from "../../commons/Popups/PopButton.vue"
import NoteNewDialog from "../NoteNewDialog.vue"

defineProps<{
  notebookId: number
  buttonTitle: string
}>()
</script>
