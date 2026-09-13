<template>
  <button title="Trash note" @click="trashNote">
    <Trash2 class="w-6 h-6" />Trash note
  </button>
</template>

<script setup lang="ts">
import { Trash2 } from "@lucide/vue"
import { useNoteTrashFlow } from "@/composables/useNoteTrashFlow"

const props = defineProps({
  noteId: { type: Number, required: true },
  noteTitle: { type: String, required: true },
})

const { trashNote } = useNoteTrashFlow(props.noteId, props.noteTitle)
</script>
