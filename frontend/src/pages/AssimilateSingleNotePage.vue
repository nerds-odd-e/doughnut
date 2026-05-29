<template>
  <AssimilateSingleNotePageView
    :key="reloadKey"
    :note="noteRealm?.note"
    :ancestor-folders="noteRealm?.ancestorFolders ?? []"
    @reload-needed="onReloadNeeded"
  />
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue"
import type { NoteRealm } from "@generated/doughnut-backend-api"
import { NoteController } from "@generated/doughnut-backend-api/sdk.gen"
import AssimilateSingleNotePageView from "./AssimilateSingleNotePageView.vue"
const { noteId } = defineProps<{
  noteId: number
}>()

const noteRealm = ref<NoteRealm | undefined>(undefined)

const loadNote = async () => {
  const { data: loaded, error } = await NoteController.showNote({
    path: { note: noteId },
  })
  if (!error && loaded) {
    noteRealm.value = loaded
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
