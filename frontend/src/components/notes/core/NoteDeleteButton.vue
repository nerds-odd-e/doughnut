<template>
  <button title="Delete note" @click="deleteNote">
    <Trash2 class="w-6 h-6" />Delete note
  </button>
</template>

<script setup lang="ts">
import { Trash2 } from "@lucide/vue"
import { useNoteDeleteFlow } from "@/composables/useNoteDeleteFlow"

const props = defineProps({
  noteId: { type: Number, required: true },
  noteTitle: { type: String, required: true },
})

const { deleteNote } = useNoteDeleteFlow(props.noteId, props.noteTitle)
</script>
