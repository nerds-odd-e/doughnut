<template>
  <div class="alert alert-info" v-if="errors">{{ errors.recording }}</div>
  <button class="btn" @click="startRecording" :disabled="isRecording">Record Audio</button>
  <button class="btn" @click="stopRecording" :disabled="!isRecording">Stop Recording</button>
  <button
    class="btn"
    @click="saveAudioLocally"
    :disabled="isRecording || !formData.uploadAudioFile"
  >
    Save Audio Locally
  </button>
</template>

<script setup lang="ts">
import type { AudioUploadDTO } from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import { ref, type PropType } from "vue"
import type { StorageAccessor } from "../../../store/createNoteStorage"
import {
  createAudioRecorder,
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
const errors = ref<Record<string, string | undefined>>()

const isRecording = ref(false)
const audioRecorder = ref<AudioRecorder>(createAudioRecorder())

const startRecording = async () => {
  errors.value = undefined
  try {
    await audioRecorder.value.startRecording()
    isRecording.value = true
  } catch (error) {
    console.error("Error starting recording:", error)
    errors.value = { recording: "Failed to start recording" }
  }
}

const stopRecording = async () => {
  isRecording.value = false
  const file = audioRecorder.value.stopRecording()
  formData.value.uploadAudioFile = file

  try {
    const response = await managedApi.restAiAudioController.convertSrt(
      formData.value
    )
    storageAccessor
      .storedApi()
      .updateTextField(noteId, "edit details", response?.textFromAudio)
  } catch (error) {
    errors.value = error as Record<string, string | undefined>
  }
}

const saveAudioLocally = () => {
  if (formData.value.uploadAudioFile) {
    const url = URL.createObjectURL(formData.value.uploadAudioFile)
    const a = document.createElement("a")
    a.href = url
    a.download = "recorded_audio.wav"
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
  }
}
</script>
