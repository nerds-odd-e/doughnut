<template>
  <div class="audio-tools-container bg-dark animate-dropdown">
    <div class="waveform-container">
      <Waveform :audioRecorder="audioRecorder" :isRecording="isRecording" />
      <button class="close-btn" @click="closeDialog" title="Close">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="24" height="24">
          <path d="M7.41 15.41L12 10.83l4.59 4.58L18 14l-6-6-6 6z"/>
        </svg>
      </button>
    </div>
    <div class="alert alert-info" v-if="errors">{{ errors }}</div>
    <div class="button-group">
      <template v-if="!isRecording">
        <button class="btn" @click="startRecording" title="Record Audio">
          <SvgAudioInput width="24" height="24" />
        </button>
      </template>
      <template v-else>
        <select
          class="device-select"
          :value="selectedDevice"
          @change="onDeviceChange"
          title="Select Audio Device"
        >
          <option v-for="device in audioDevices" :key="device.deviceId" :value="device.deviceId">
            {{ device.label || `Microphone ${device.deviceId.slice(0, 4)}...` }}
          </option>
        </select>
      </template>
      <button class="btn" @click="flushAudio" :disabled="!isRecording" title="Flush Audio">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="24" height="24">
          <path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/>
        </svg>
      </button>
      <button class="btn" @click="stopRecording" :disabled="!isRecording" title="Stop Recording">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="24" height="24">
          <path d="M6 6h12v12H6z"/>
        </svg>
      </button>
      <button
        class="btn"
        @click="saveAudioLocally"
        :disabled="isRecording || !audioFile"
        title="Save Audio Locally"
      >
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="24" height="24">
          <path d="M19 9h-4V3H9v6H5l7 7 7-7zM5 18v2h14v-2H5z"/>
        </svg>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import useLoadingApi from "@/managedApi/useLoadingApi"
import { onUnmounted, ref, type PropType } from "vue"
import type { StorageAccessor } from "../../../store/createNoteStorage"
import { createAudioRecorder } from "../../../models/audio/recording"
import { createWakeLocker } from "../../../models/wakeLocker"
import type { Note } from "@/generated/backend"
import Waveform from "./Waveform.vue"
import SvgAudioInput from "@/components/svgs/SvgAudioInput.vue"

const { managedApi } = useLoadingApi()
const { note, storageAccessor } = defineProps({
  note: { type: Object as PropType<Note>, required: true },
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
})

const emit = defineEmits(["closeDialog"])

const audioFile = ref<Blob | undefined>()
const errors = ref<Record<string, string | undefined>>()

const isRecording = ref(false)
const wakeLocker = createWakeLocker()

const processAudio = async (file: Blob) => {
  try {
    const response = await managedApi.restAiAudioController.audioToText({
      previousNoteDetails: note.details?.slice(-100) ?? "",
      uploadAudioFile: file,
    })
    await storageAccessor
      .storedApi()
      .appendDetails(note.id, response?.completionMarkdownFromAudio)
  } catch (error) {
    errors.value = error as Record<string, string | undefined>
  }
}

const audioRecorder = createAudioRecorder(processAudio)
const audioDevices = audioRecorder.getAudioDevices()
const selectedDevice = audioRecorder.getSelectedDevice()

const onDeviceChange = async (event: Event) => {
  const deviceId = (event.target as HTMLSelectElement).value
  try {
    await audioRecorder.switchAudioDevice(deviceId)
  } catch (error) {
    console.error("Error switching audio device:", error)
    errors.value = { devices: "Failed to switch audio device" }
  }
}

const startRecording = async () => {
  errors.value = undefined
  try {
    await wakeLocker.request()
    await audioRecorder.startRecording()
    isRecording.value = true
  } catch (error) {
    console.error("Error starting recording:", error)
    errors.value = { recording: "Failed to start recording" }
    await wakeLocker.release()
  }
}

const stopRecording = async () => {
  isRecording.value = false
  try {
    audioFile.value = await audioRecorder.stopRecording()
  } finally {
    await wakeLocker.release()
  }
}

const saveAudioLocally = () => {
  if (audioFile.value) {
    const url = URL.createObjectURL(audioFile.value)
    const a = document.createElement("a")
    a.href = url
    a.download = "recorded_audio.wav"
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
  }
}

const closeDialog = () => {
  if (isRecording.value) {
    stopRecording()
  }
  emit("closeDialog")
}

const flushAudio = async () => {
  if (isRecording.value) {
    await audioRecorder.flush()
  }
}

onUnmounted(() => {
  wakeLocker.release()
})
</script>

<style scoped>
.audio-tools-container {
  position: relative;
  border-radius: 0 0 12px 12px;
  padding: 20px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  animation: dropDown 0.3s ease-out;
  transform-origin: top;
}

@keyframes dropDown {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.waveform-container {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 20px;
}

.waveform-container :deep(.waveform) {
  flex: 1;
}

.close-btn {
  flex-shrink: 0;
  background: none;
  border: none;
  cursor: pointer;
  color: #4a5568;
  transition: color 0.3s ease;
  padding: 8px;
  border-radius: 50%;
  background-color: rgba(255, 255, 255, 0.1);
}

.close-btn:hover {
  color: #2d3748;
  background-color: rgba(255, 255, 255, 0.2);
}

.button-group {
  display: flex;
  justify-content: center;
  gap: 20px;
}

.btn {
  background-color: #4299e1;
  border: none;
  color: white;
  padding: 10px;
  border-radius: 50%;
  cursor: pointer;
  transition: background-color 0.3s ease, transform 0.2s ease;
  flex-shrink: 0;
}

.btn:hover:not(:disabled) {
  background-color: #3182ce;
  transform: scale(1.05);
}

.btn:disabled {
  background-color: #a0aec0;
  cursor: not-allowed;
}

@media (max-width: 480px) {
  .audio-tools-container {
    padding: 15px;
  }

  .button-group {
    gap: 10px;
  }

  .btn {
    padding: 8px;
  }
}

.device-select {
  padding: 8px;
  border-radius: 4px;
  border: 1px solid #4299e1;
  background-color: white;
  color: #2d3748;
  font-size: 14px;
  cursor: pointer;
}

.device-select:focus {
  outline: none;
  border-color: #3182ce;
  box-shadow: 0 0 0 3px rgba(66, 153, 225, 0.5);
}
</style>
