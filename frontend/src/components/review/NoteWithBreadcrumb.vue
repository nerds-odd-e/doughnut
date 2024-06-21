<template>
  <Breadcrumb v-bind="{ noteTopic: note.noteTopic }" />
  <NoteShow
    v-bind="{
      noteId: note.id,
      expandChildren: false,
      readonly: false,
      storageAccessor,
    }"
  />
</template>

<script setup lang="ts">
import { Note } from "@/generated/backend"
import { StorageAccessor } from "@/store/createNoteStorage"
import { PropType } from "vue"

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
