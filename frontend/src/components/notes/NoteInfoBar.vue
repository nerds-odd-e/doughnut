<template>
  <NoteInfoComponent
    v-if="noteInfo?.note"
    :note-info="noteInfo"
    :current-note-details="currentNoteDetails"
    @level-changed="$emit('levelChanged', $event)"
    @note-type-updated="$emit('noteTypeUpdated', $event)"
    @remember-spelling-changed="$emit('rememberSpellingChanged', $event)"
  />
</template>

<script setup lang="ts">
import type { NoteInfo } from "@generated/backend"
import { NoteController } from "@generated/backend/sdk.gen"
import NoteInfoComponent from "./NoteInfoComponent.vue"
import { ref, watch, computed } from "vue"
import type { NoteType } from "@/models/noteTypeOptions"

const props = defineProps<{
  noteId: number
  currentNoteDetails?: string
}>()

const emit = defineEmits<{
  (e: "levelChanged", value: unknown): void
  (e: "noteTypeUpdated", noteType: NoteType): void
  (e: "noteInfoLoaded", noteType: NoteType | undefined): void
  (e: "rememberSpellingChanged", value: boolean): void
}>()

const noteInfo = ref<NoteInfo | undefined>(undefined)

const currentNoteDetails = computed(
  () => props.currentNoteDetails ?? noteInfo.value?.note.note.details
)

const fetchData = async () => {
  const { data: noteInfoData, error } = await NoteController.getNoteInfo({
    path: { note: props.noteId },
  })
  if (!error) {
    noteInfo.value = noteInfoData!
    emit("noteInfoLoaded", noteInfoData?.noteType)
    emit(
      "rememberSpellingChanged",
      noteInfoData?.recallSetting?.rememberSpelling ?? false
    )
  }
}

watch(
  () => props.noteId,
  () => {
    fetchData()
  },
  { immediate: true }
)
</script>
