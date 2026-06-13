<template>
  <NoteInfoComponent
    v-if="noteRecallInfo && displayedNote"
    :note="displayedNote"
    :note-recall-info="noteRecallInfo"
    @level-changed="$emit('levelChanged', $event)"
    @remember-spelling-changed="$emit('rememberSpellingChanged', $event)"
  />
</template>

<script setup lang="ts">
import type { Note, NoteRecallInfo } from "@generated/doughnut-backend-api"
import { NoteController } from "@generated/doughnut-backend-api/sdk.gen"
import NoteInfoComponent from "./NoteInfoComponent.vue"
import { ref, watch, computed } from "vue"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import { useStorageAccessor } from "@/composables/useStorageAccessor"

const props = defineProps<{
  noteId: number
  note?: Note
}>()

const emit = defineEmits<{
  (e: "levelChanged", value: unknown): void
  (e: "rememberSpellingChanged", value: boolean): void
  (e: "noteRecallInfoLoaded", value: NoteRecallInfo): void
}>()

const noteRecallInfo = ref<NoteRecallInfo | undefined>(undefined)
const storageAccessor = useStorageAccessor()

const displayedNote = computed(
  () =>
    props.note ??
    storageAccessor.value?.storedApi().getNoteRealmRef(props.noteId).value?.note
)

const fetchData = async () => {
  const { data, error } = await apiCallWithLoading(() =>
    NoteController.getNoteInfo({
      path: { note: props.noteId },
    })
  )
  if (!error) {
    noteRecallInfo.value = data!
    emit(
      "rememberSpellingChanged",
      data?.recallSetting?.rememberSpelling ?? false
    )
    emit("noteRecallInfoLoaded", data!)
  }
}

watch(
  () => props.noteId,
  () => {
    fetchData()
  },
  { immediate: true }
)

defineExpose({ reload: fetchData, noteRecallInfo })
</script>
