<template>
  <button class="btn" @click="startRecording" :disabled="isRecording">Record Audio</button>
  <button class="btn" @click="stopRecording" :disabled="!isRecording">Stop Recording</button>
  <NoteUploadAudioForm
    v-if="!!formData"
    v-model="formData"
    :errors="noteFormErrors"
  />
  <input
    value="Convert to SRT"
    class="btn btn-primary"
    @click.once="convertToSRT"
  />
</template>

<script setup lang="ts">
import type { AudioUploadDTO } from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import { ref, type PropType } from "vue"
import NoteUploadAudioForm from "./NoteUploadAudioForm.vue"
import type { StorageAccessor } from "../../../store/createNoteStorage"
import {
  createAudioRecorder,
  startRecording as startAudioRecording,
  stopRecording as stopAudioRecording,
  type AudioRecorder,
} from "../../../models/recording"

const { managedApi } = useLoadingApi()
const { noteId, storageAccessor } = defineProps({
  noteId: { type: Number, required: true },
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
})

const emit = defineEmits(["closeDialog"])

const formData = ref<AudioUploadDTO>({})
const noteFormErrors = ref<Record<string, string | undefined>>({})

const isRecording = ref(false)
const audioRecorder = ref<AudioRecorder>(createAudioRecorder())

const convertToSRT = async () => {
  try {
    const response = await managedApi.restAiAudioController.convertSrt(
      formData.value
    )
    storageAccessor
      .storedApi()
      .updateTextField(noteId, "edit details", response?.textFromAudio)
  } catch (error: unknown) {
    noteFormErrors.value = error as Record<string, string | undefined>
  }
}

const startRecording = async () => {
  try {
    await startAudioRecording(audioRecorder.value)
    isRecording.value = true
  } catch (error) {
    console.error("Error starting recording:", error)
    noteFormErrors.value = { recording: "Failed to start recording" }
  }
}

const stopRecording = async () => {
  isRecording.value = false
  const file = stopAudioRecording(audioRecorder.value)
  formData.value.uploadAudioFile = file

  try {
    const response = await managedApi.restAiAudioController.convertSrt(
      formData.value
    )
    storageAccessor
      .storedApi()
      .updateTextField(noteId, "edit details", response?.textFromAudio)
  } catch (error) {
    noteFormErrors.value = error as Record<string, string | undefined>
  }
}
</script>
