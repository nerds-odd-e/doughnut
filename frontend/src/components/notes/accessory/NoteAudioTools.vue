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

let audioContext
let mediaStream
let audioInput
let recorder
let audioData: Float32Array[] = []

const startRecording = async () => {
  try {
    audioContext = new AudioContext()
    mediaStream = await navigator.mediaDevices.getUserMedia({ audio: true })
    audioInput = audioContext.createMediaStreamSource(mediaStream)

    // Create a ScriptProcessorNode
    const bufferSize = 4096
    recorder = audioContext.createScriptProcessor(bufferSize, 1, 1)

    recorder.onaudioprocess = (event) => {
      const channelData = event.inputBuffer.getChannelData(0)
      audioData.push(new Float32Array(channelData))
    }

    // Connect the nodes
    audioInput.connect(recorder)
    recorder.connect(audioContext.destination)

    isRecording.value = true
  } catch (error) {
    console.error("Error starting recording:", error)
    noteFormErrors.value = { recording: "Failed to start recording" }
  }
}

const stopRecording = async () => {
  isRecording.value = false

  // Stop the recorder and media stream
  recorder.disconnect()
  audioInput.disconnect()
  mediaStream.getTracks().forEach((track) => track.stop())

  // Encode audio data to WAV
  const wavBlob = encodeWAV(audioData, audioContext.sampleRate)
  const fileName = `recorded_audio_${new Date().toISOString()}.wav`
  const file = new File([wavBlob], fileName, { type: "audio/wav" })

  formData.value.uploadAudioFile = file
  audioData = []

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

const encodeWAV = (samples, sampleRate) => {
  const bufferLength = samples.reduce((acc, sample) => acc + sample.length, 0)
  const buffer = new ArrayBuffer(44 + bufferLength * 2)
  const view = new DataView(buffer)

  // WAV file header
  writeString(view, 0, "RIFF")
  view.setUint32(4, 36 + bufferLength * 2, true)
  writeString(view, 8, "WAVE")
  writeString(view, 12, "fmt ")
  view.setUint32(16, 16, true) // Subchunk1Size (PCM)
  view.setUint16(20, 1, true) // AudioFormat (PCM)
  view.setUint16(22, 1, true) // NumChannels
  view.setUint32(24, sampleRate, true) // SampleRate
  view.setUint32(28, sampleRate * 2, true) // ByteRate
  view.setUint16(32, 2, true) // BlockAlign
  view.setUint16(34, 16, true) // BitsPerSample
  writeString(view, 36, "data")
  view.setUint32(40, bufferLength * 2, true) // Subchunk2Size

  // Write audio samples
  let offset = 44
  samples.forEach((sample) => {
    for (let i = 0; i < sample.length; i++, offset += 2) {
      const s = Math.max(-1, Math.min(1, sample[i]))
      view.setInt16(offset, s < 0 ? s * 0x8000 : s * 0x7fff, true)
    }
  })

  return new Blob([view], { type: "audio/wav" })
}

const writeString = (view, offset, string) => {
  for (let i = 0; i < string.length; i++) {
    view.setUint8(offset + i, string.charCodeAt(i))
  }
}
</script>

