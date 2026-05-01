<template>
  <AssimilateSingleNotePageView
    :key="reloadKey"
    :note="note"
    @assimilation-done="assimilationDone"
    @reload-needed="onReloadNeeded"
  />
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue"
import type { Note } from "@generated/doughnut-backend-api"
import { NoteController } from "@generated/doughnut-backend-api/sdk.gen"
import AssimilateSingleNotePageView from "./AssimilateSingleNotePageView.vue"
import { useRouter } from "vue-router"
import { noteShowLocation } from "@/routes/noteShowLocation"

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

const assimilationDone = () => {
  const id = note.value?.id
  if (id != null) {
    router.push(noteShowLocation(id))
  }
}

const reloadKey = ref(0)

const onReloadNeeded = async () => {
  await loadNote()
  reloadKey.value += 1
}

onMounted(() => {
  loadNote()
})
</script>
