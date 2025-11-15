<template>
  <NoteInfoComponent
    v-if="noteInfo?.note"
    :note-info="noteInfo"
    @level-changed="$emit('levelChanged', $event)"
  />
</template>

<script setup lang="ts">
import type { NoteInfo } from "@generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import NoteInfoComponent from "./NoteInfoComponent.vue"
import { ref, onMounted } from "vue"

const props = defineProps<{
  noteId: number
}>()

defineEmits<{
  (e: "levelChanged", value: unknown): void
}>()

const { managedApi } = useLoadingApi()
const noteInfo = ref<NoteInfo | undefined>(undefined)

const fetchData = () => {
  managedApi.restNoteController.getNoteInfo(props.noteId).then((articles) => {
    noteInfo.value = articles
  })
}

onMounted(() => {
  fetchData()
})
</script>
