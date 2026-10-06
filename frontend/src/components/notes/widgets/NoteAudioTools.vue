<template>
  <section :aria-label="noteMoreOptionsTitles.audio">
    <Waveform
      class="mb-5"
      :audioRecorder="audioRecorder"
      :isRecording="isRecording"
    />
    <p role="status" class="text-center mb-3">{{ status }}</p>
    <div
      v-if="errors"
      class="daisy-alert"
      :class="errors.conversion ? 'daisy-alert-error' : 'daisy-alert-info'"
    >{{ errors.conversion ?? errors }}</div>
    <button
      v-if="!isRecording && errors?.conversion && audioRecorder.hasUnconvertedAudio()"
      class="daisy-btn daisy-btn-sm retry-button"
      @click="stopRecording"
      :disabled="isProcessing"
      title="Retry"
    >Retry</button>
    <div class="button-group">
      <button v-if="!isRecording" class="daisy-btn main-action" @click="startRecording">
        <Mic :size="24" />
        Record
      </button>
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
        <button class="daisy-btn main-action" @click="stopRecording">
          <Square :size="24" />
          Stop
        </button>
      </template>
      <button class="daisy-btn" @click="tryFlushAudio" :disabled="!isRecording || isProcessing" title="Flush Audio">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="24" height="24">
          <path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/>
        </svg>
      </button>
      <button
        class="daisy-btn"
        @click="saveAudioLocally(audioFile as Blob)"
        :disabled="isRecording || !audioFile"
        title="Save Audio Locally"
      >
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="24" height="24">
          <path d="M19 9h-4V3H9v6H5l7 7 7-7zM5 18v2h14v-2H5z"/>
        </svg>
      </button>
      <button
        class="daisy-btn"
        @click="showAdvancedOptions = !showAdvancedOptions"
        title="Advanced Options"
      >
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="24" height="24">
          <path d="M19.14 12.94c.04-.3.06-.61.06-.94 0-.32-.02-.64-.07-.94l2.03-1.58c.18-.14.23-.41.12-.61l-1.92-3.32c-.12-.22-.37-.29-.59-.22l-2.39.96c-.5-.38-1.03-.7-1.62-.94l-.36-2.54c-.04-.24-.24-.41-.48-.41h-3.84c-.24 0-.43.17-.47.41l-.36 2.54c-.59.24-1.13.57-1.62.94l-2.39-.96c-.22-.08-.47 0-.59.22L2.74 8.87c-.12.21-.08.47.12.61l2.03 1.58c-.05.3-.07.62-.07.94s.02.64.07.94l-2.03 1.58c-.18.14-.23.41-.12.61l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c.05.24.24.41.48.41h3.84c.24 0 .44-.17.47-.41l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39.96c.22.08.47 0 .59-.22l1.92-3.32c.12-.22.07-.47-.12-.61l-2.01-1.58zM12 15.6c-1.98 0-3.6-1.62-3.6-3.6s1.62-3.6 3.6-3.6 3.6 1.62 3.6 3.6-1.62 3.6-3.6 3.6z"/>
        </svg>
      </button>
    </div>
    <div v-if="showAdvancedOptions" class="advanced-options">
      <FullScreen>
        <div v-if="errors" class="fullscreen-error">
          {{ Object.values(errors)[0] }}
        </div>
      </FullScreen>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref, type PropType } from "vue"
import { createAudioRecorder } from "../../../models/audio/audioRecorder"
import { createWakeLocker } from "../../../models/wakeLocker"
import { saveAudioLocally } from "@/models/audio/saveAudioLocally"
import type { Note } from "@generated/donut-backend-api"
import Waveform from "./Waveform.vue"
import FullScreen from "@/components/common/FullScreen.vue"
import { noteMoreOptionsTitles } from "./noteMoreOptionsTitles"
import { Mic, Square } from "@lucide/vue"
import { useNoteAudioProcessing } from "@/composables/useNoteAudioProcessing"

const { note } = defineProps({
  note: { type: Object as PropType<Note>, required: true },
})

const audioFile = ref<Blob | undefined>()
const errors = ref<Record<string, string | undefined>>()
const statusByPhase = {
  ready: "Ready to record",
  recording: "Recording. Speak now.",
  stopping: "Turning your speech into text…",
  added: "Added to your note.",
  nothingAdded: "No speech was turned into text.",
} as const
const phase = ref<keyof typeof statusByPhase>("ready")
const isRecording = computed(() => phase.value === "recording")
const status = computed(() => statusByPhase[phase.value])
const wakeLocker = createWakeLocker()
const showAdvancedOptions = ref(false)

const { processAudio, isProcessing, wroteText } = useNoteAudioProcessing(
  note,
  errors
)

const audioRecorder = createAudioRecorder(processAudio)
const audioDevices = audioRecorder.getAudioDevices()
const selectedDevice = audioRecorder.getSelectedDevice()

const onDeviceChange = async (event: Event) => {
  const deviceId = (event.target as HTMLSelectElement).value
  try {
    await audioRecorder.switchAudioDevice(deviceId)
  } catch (error) {
    errors.value = { devices: "Failed to switch audio device" }
  }
}

const startRecording = async () => {
  errors.value = undefined
  wroteText.value = false
  try {
    await wakeLocker.request()
    await audioRecorder.startRecording()
    phase.value = "recording"
  } catch (error) {
    errors.value = { recording: "Failed to start recording" }
    await wakeLocker.release()
  }
}

const stopRecording = async () => {
  phase.value = "stopping"
  try {
    audioFile.value = await audioRecorder.stopRecording()
  } finally {
    phase.value = errors.value?.conversion
      ? "ready"
      : wroteText.value
        ? "added"
        : "nothingAdded"
    await wakeLocker.release()
  }
}

onBeforeUnmount(() => {
  if (isRecording.value) {
    stopRecording()
  }
})

const tryFlushAudio = async () => {
  if (isRecording.value) {
    await audioRecorder.tryFlush()
  }
}
</script>

<style scoped>
.button-group {
  display: flex;
  justify-content: center;
  gap: 20px;
}

.daisy-btn {
  background-color: #4299e1;
  border: none;
  color: white;
  padding: 10px;
  border-radius: 50%;
  cursor: pointer;
  transition: background-color 0.3s ease, transform 0.2s ease;
  flex-shrink: 0;
}

.main-action {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border-radius: 9999px;
  padding: 10px 18px;
}

.daisy-btn:hover:not(:disabled) {
  background-color: #3182ce;
  transform: scale(1.05);
}

.daisy-btn:disabled {
  background-color: #a0aec0;
  cursor: not-allowed;
}

@media (max-width: 480px) {
  .button-group {
    gap: 10px;
  }

  .daisy-btn {
    padding: 8px;
  }
}

.retry-button {
  display: block;
  margin: 8px auto;
  border-radius: 8px;
}

.advanced-options {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.fullscreen-error {
  color: #fc8181;
  font-size: 14px;
  text-align: center;
  max-width: 80%;
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
