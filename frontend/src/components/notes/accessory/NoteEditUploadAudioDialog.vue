<template>
  <button class="btn" @click="startRecording" :disabled="isRecording">Record Audio</button>
  <button class="btn" @click="stopRecording" :disabled="!isRecording">Stop Recording</button>
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
import { ref, type PropType } from "vue"
import TextArea from "../../form/TextArea.vue"
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
const convertedSrt = ref<string>("")

const isRecording = ref(false)
let mediaRecorder: MediaRecorder | null = null
let audioChunks: Blob[] = []

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
    storageAccessor.storedApi().updateTextField(noteId, "edit details", "You")
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

    mediaRecorder.onstop = () => {
      const audioBlob = new Blob(audioChunks, { type: "audio/wav" })
      formData.value.uploadAudioFile = audioBlob
      audioChunks = []
    }

    mediaRecorder.start()
    isRecording.value = true
  } catch (error) {
    console.error("Error starting recording:", error)
    noteFormErrors.value = { recording: "Failed to start recording" }
  }
}

const stopRecording = () => {
  if (mediaRecorder && mediaRecorder.state !== "inactive") {
    mediaRecorder.stop()
    isRecording.value = false
  }
}
</script>
