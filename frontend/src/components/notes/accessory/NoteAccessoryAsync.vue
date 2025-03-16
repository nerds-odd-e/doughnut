<template>
  <div class="daisy:text-break">
    <NoteAccessoryDisplay
      v-if="noteAccessory"
      :note-accessory="noteAccessory"
    />
  </div>
</template>

<script setup lang="ts">
import type { NoteAccessory } from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import type { PropType } from "vue"
import { onMounted, ref, watch } from "vue"
import NoteAccessoryDisplay from "./NoteAccessoryDisplay.vue"

const { managedApi } = useLoadingApi()

const { noteId, updatedNoteAccessory } = defineProps({
  noteId: { type: Number, required: true },
  readonly: { type: Boolean, required: true },
  updatedNoteAccessory: {
    type: Object as PropType<NoteAccessory>,
  },
})

const noteAccessory = ref<NoteAccessory | undefined>(undefined)

watch(
  () => updatedNoteAccessory,
  (updatedNoteAccessory) => {
    noteAccessory.value = updatedNoteAccessory
  }
)

const fetchData = async () => {
  noteAccessory.value =
    await managedApi.restNoteController.showNoteAccessory(noteId)
}

onMounted(fetchData)
</script>
