<template>
  <AssimilateSingleNotePageView
    :note="note"
    @initial-review-done="initialReviewDone"
    @reload-needed="onReloadNeeded"
  />
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue"
import type { Note } from "@generated/backend"
import { NoteController } from "@generated/backend/sdk.gen"
import AssimilateSingleNotePageView from "./AssimilateSingleNotePageView.vue"
import { useRouter } from "vue-router"

const { noteId } = defineProps<{
  noteId: number
}>()

const router = useRouter()
const note = ref<Note | undefined>(undefined)

const loadNote = async () => {
  const { data: noteRealm, error } = await NoteController.showNote({
    path: { note: noteId },
  })
  if (!error && noteRealm) {
    note.value = noteRealm.note
  }
}

const initialReviewDone = () => {
  // Navigate back to the note after assimilation
  router.push({ name: "noteShow", params: { noteId } })
}

const onReloadNeeded = () => {
  loadNote()
}

onMounted(() => {
  loadNote()
})
</script>
