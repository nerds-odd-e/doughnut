<template>
  <NoteInfoComponent
    v-if="noteInfo && note"
    :note="note"
    :note-info="noteInfo"
    @level-changed="$emit('levelChanged', $event)"
    @remember-spelling-changed="$emit('rememberSpellingChanged', $event)"
  />
</template>

<script setup lang="ts">
import type { NoteInfo } from "@generated/backend"
import { NoteController } from "@generated/backend/sdk.gen"
import NoteInfoComponent from "./NoteInfoComponent.vue"
import { ref, watch, computed } from "vue"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import { useStorageAccessor } from "@/composables/useStorageAccessor"

const props = defineProps<{
  noteId: number
}>()

const emit = defineEmits<{
  (e: "levelChanged", value: unknown): void
  (e: "rememberSpellingChanged", value: boolean): void
}>()

const noteInfo = ref<NoteInfo | undefined>(undefined)
const storageAccessor = useStorageAccessor()

const note = computed(
  () =>
    storageAccessor.value?.storedApi().getNoteRealmRef(props.noteId).value?.note
)

const fetchData = async () => {
  const { data: noteInfoData, error } = await apiCallWithLoading(() =>
    NoteController.getNoteInfo({
      path: { note: props.noteId },
    })
  )
  if (!error) {
    noteInfo.value = noteInfoData!
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
