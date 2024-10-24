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
let mediaRecorder: MediaRecorder | null = null
let audioChunks: Blob[] = []

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
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
    mediaRecorder = new MediaRecorder(stream)

    mediaRecorder.ondataavailable = (event) => {
      audioChunks.push(event.data)
    }

    mediaRecorder.onstop = async () => {
      const audioBlob = new Blob(audioChunks, { type: "audio/wav" })
      const fileName = `recorded_audio_${new Date().toISOString()}.wav`
      const file = new File([audioBlob], fileName, { type: "audio/wav" })
      formData.value.uploadAudioFile = file
      audioChunks = []
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

    mediaRecorder.start()
    isRecording.value = true
  } catch (error) {
    console.error("Error starting recording:", error)
    noteFormErrors.value = { recording: "Failed to start recording" }
  }
}

const stopRecording = () => {
  isRecording.value = false
  if (mediaRecorder && mediaRecorder.state !== "inactive") {
    mediaRecorder.stop()

    // Stop all tracks in the media stream
    if (mediaRecorder.stream) {
      mediaRecorder.stream.getTracks().forEach((track) => track.stop())
    }

    // Reset mediaRecorder
    mediaRecorder = null
  }
}
</script>

