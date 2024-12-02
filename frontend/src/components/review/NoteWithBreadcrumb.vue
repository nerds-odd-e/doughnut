<template>
  <NoteShow
    v-bind="{
      noteId: note.id,
      expandChildren: false,
      storageAccessor,
    }"
  />
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import type { Note } from "@/generated/backend"
import type { StorageAccessor } from "@/store/createNoteStorage"
import NoteShow from "../notes/NoteShow.vue"

defineProps({
  note: {
    type: Object as PropType<Note>,
    required: true,
  },
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
})
</script>
