<template>
  <NoteInfoComponent
    v-if="noteRecallInfo && note"
    :note="note"
    :note-recall-info="noteRecallInfo"
    @level-changed="$emit('levelChanged', $event)"
    @remember-spelling-changed="$emit('rememberSpellingChanged', $event)"
  />
</template>

<script setup lang="ts">
import type { NoteRecallInfo } from "@generated/backend"
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

const noteRecallInfo = ref<NoteRecallInfo | undefined>(undefined)
const storageAccessor = useStorageAccessor()

const note = computed(
  () =>
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
