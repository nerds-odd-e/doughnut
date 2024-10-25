<template>
  <div class="audio-tools-container">
    <button class="close-btn" @click="closeDialog" title="Close">
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="24" height="24">
        <path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/>
      </svg>
    </button>
    <div class="alert alert-info" v-if="errors">{{ errors }}</div>
    <canvas ref="waveformCanvas" class="waveform-canvas"></canvas>
    <div class="button-group">
      <button class="btn" @click="startRecording" :disabled="isRecording" title="Record Audio">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="24" height="24">
          <path d="M12 14c1.66 0 3-1.34 3-3V5c0-1.66-1.34-3-3-3S9 3.34 9 5v6c0 1.66 1.34 3 3 3zm-1-9c0-.55.45-1 1-1s1 .45 1 1v6c0 .55-.45 1-1 1s-1-.45-1-1V5zm6 6c0 2.76-2.24 5-5 5s-5-2.24-5-5H5c0 3.53 2.61 6.43 6 6.92V21h2v-3.08c3.39-.49 6-3.39 6-6.92h-2z"/>
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
        :disabled="isRecording || !formData.uploadAudioFile"
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
import type { AudioUploadDTO } from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import { onMounted, onUnmounted, ref, type PropType } from "vue"
import type { StorageAccessor } from "../../../store/createNoteStorage"
import {
  createAudioRecorder,
  type AudioRecorder,
} from "../../../models/recording"
import { createWakeLocker, type WakeLocker } from "../../../models/wakeLocker"

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
const wakeLocker = ref<WakeLocker>(createWakeLocker())

const waveformCanvas = ref<HTMLCanvasElement | null>(null)
let animationId: number | null = null

const startRecording = async () => {
  errors.value = undefined
  try {
    await wakeLocker.value.request() // Request wake lock
    await audioRecorder.value.startRecording()
    isRecording.value = true
    if (!animationId) {
      drawWaveform()
    }
  } catch (error) {
    console.error("Error starting recording:", error)
    errors.value = { recording: "Failed to start recording" }
    await wakeLocker.value.release() // Release wake lock if recording fails
  }
}

const stopRecording = async () => {
  isRecording.value = false
  if (animationId) {
    cancelAnimationFrame(animationId)
    animationId = null
  }
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
  } finally {
    await wakeLocker.value.release() // Release wake lock when recording stops
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

const closeDialog = () => {
  if (isRecording.value) {
    stopRecording()
  }
  emit("closeDialog")
}

function drawWaveform() {
  if (!waveformCanvas.value) return

  const canvas = waveformCanvas.value
  const ctx = canvas.getContext("2d")
  if (!ctx) return

  const audioData = audioRecorder.value.getAudioData()
  const dataLength = audioData.length
  const bufferLength = 3
  const start = dataLength > bufferLength ? dataLength - bufferLength : 0
  const data = audioData.slice(start, dataLength)

  // Shift canvas content to the left
  ctx.drawImage(canvas, -1, 0)

  // Clear the rightmost column
  ctx.fillStyle = "#ffffff"
  ctx.fillRect(canvas.width - 1, 0, 1, canvas.height)

  // Draw new data on the right edge
  const height = canvas.height

  // Compute average of data
  let sum = 0
  for (let i = 0; i < data.length; i++) {
    const channel = data[i]
    if (!channel) continue
    for (let j = 0; j < channel.length; j++) {
      sum += channel[j]!
    }
  }

  const avgSample = sum / data.length

  const y = height - Math.abs(avgSample) * height

  ctx.fillStyle = "#4299e1"
  ctx.fillRect(canvas.width - 1, y, 1, height - y)

  // Schedule next frame
  animationId = requestAnimationFrame(drawWaveform)
}

onMounted(() => {
  const canvas = waveformCanvas.value
  if (canvas) {
    // Set canvas dimensions to match its display size
    const rect = canvas.getBoundingClientRect()
    canvas.width = rect.width
    canvas.height = rect.height
  }
})

onUnmounted(() => {
  if (animationId) {
    cancelAnimationFrame(animationId)
    animationId = null
  }
  wakeLocker.value.release() // Ensure wake lock is released when component is unmounted
})
</script>

<style scoped>
.audio-tools-container {
  position: relative;
  background-color: #f0f4f8;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}

.close-btn {
  position: absolute;
  top: 10px;
  right: 10px;
  background: none;
  border: none;
  cursor: pointer;
  color: #4a5568;
  transition: color 0.3s ease;
}

.close-btn:hover {
  color: #2d3748;
}

.button-group {
  display: flex;
  justify-content: space-around;
  flex-wrap: wrap;
}

.btn {
  background-color: #4299e1;
  border: none;
  color: white;
  padding: 10px;
  border-radius: 50%;
  cursor: pointer;
  transition: background-color 0.3s ease, transform 0.2s ease;
  margin: 5px;
}

.btn:hover:not(:disabled) {
  background-color: #3182ce;
  transform: scale(1.05);
}

.btn:disabled {
  background-color: #a0aec0;
  cursor: not-allowed;
}


.waveform-canvas {
  width: 100%;
  height: 50px;
  background-color: #e2e8f0;
  border-radius: 8px;
  margin-bottom: 20px;
}


@media (max-width: 480px) {
  .audio-tools-container {
    padding: 15px;
  }

  .button-group {
    flex-direction: column;
    align-items: center;
  }

  .btn {
    margin: 10px 0;
  }
}
</style>
