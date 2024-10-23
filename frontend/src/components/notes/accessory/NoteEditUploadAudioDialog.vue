<template>
  <button class="btn">Record Audio</button>
  <button class="btn">Stop Recording</button>
  <form @submit.prevent.once="uploadAudio">
    <NoteUploadAudioForm
      v-if="!!formData"
      v-model="formData"
      :errors="noteFormErrors"
    />
    <input type="submit" value="Save" class="btn btn-primary" />
    <input
      value="Convert to SRT"
      class="btn btn-primary"
      @click="convertToSRT"
    />
  </form>
  <TextArea :field="`convertedSrt`" v-model="convertedSrt" :rows="8" />
</template>

<script setup lang="ts">
import type { AudioUploadDTO } from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import { ref } from "vue"
import TextArea from "../../form/TextArea.vue"
import NoteUploadAudioForm from "./NoteUploadAudioForm.vue"

const { managedApi } = useLoadingApi()
const { noteId } = defineProps({
  noteId: { type: Number, required: true },
})

const emit = defineEmits(["closeDialog"])

const formData = ref<AudioUploadDTO>({})
const noteFormErrors = ref<Record<string, string | undefined>>({})
const convertedSrt = ref<string>("")

const uploadAudio = async () => {
  try {
    const na = await managedApi.restNoteController.uploadAudio(
      noteId,
      formData.value
    )
    emit("closeDialog", na)
  } catch (error: unknown) {
    noteFormErrors.value = error as Record<string, string | undefined>
  }
}
const convertToSRT = async () => {
  try {
    const response = await managedApi.restAiAudioController.convertSrt(
      formData.value
    )
    convertedSrt.value = response?.srt
  } catch (error: unknown) {
    noteFormErrors.value = error as Record<string, string | undefined>
  }
}
</script>
