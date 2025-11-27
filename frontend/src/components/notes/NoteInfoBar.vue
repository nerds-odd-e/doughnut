<template>
  <NoteInfoComponent
    v-if="noteInfo?.note"
    :note-info="noteInfo"
    @level-changed="$emit('levelChanged', $event)"
  />
</template>

<script setup lang="ts">
import type { NoteInfo } from "@generated/backend"
import { NoteController } from "@generated/backend/sdk.gen"
import {} from "@/managedApi/clientSetup"
import NoteInfoComponent from "./NoteInfoComponent.vue"
import { ref, onMounted } from "vue"

const props = defineProps<{
  noteId: number
}>()

defineEmits<{
  (e: "levelChanged", value: unknown): void
}>()

const noteInfo = ref<NoteInfo | undefined>(undefined)

const fetchData = async () => {
  const { data: noteInfoData, error } = await NoteController.getNoteInfo({
    path: { note: props.noteId },
  })
  if (!error) {
    noteInfo.value = noteInfoData!
  }
}

onMounted(() => {
  fetchData()
})
</script>
