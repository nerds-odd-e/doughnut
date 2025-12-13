<template>
  <NoteInfoComponent
    v-if="noteInfo?.note"
    :note-info="noteInfo"
    @level-changed="$emit('levelChanged', $event)"
    @note-type-updated="$emit('noteTypeUpdated', $event)"
  />
</template>

<script setup lang="ts">
import type { NoteInfo } from "@generated/backend"
import { NoteController } from "@generated/backend/sdk.gen"
import NoteInfoComponent from "./NoteInfoComponent.vue"
import { ref, onMounted } from "vue"
import type { NoteType } from "@/models/noteTypeOptions"

const props = defineProps<{
  noteId: number
}>()

defineEmits<{
  (e: "levelChanged", value: unknown): void
  (e: "noteTypeUpdated", noteType: NoteType): void
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
