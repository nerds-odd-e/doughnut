<template>
  <section :aria-label="noteMoreOptionsTitles.audio">
    <Waveform
      class="mb-5"
      :audioRecorder="audioRecorder"
      :isRecording="isRecording"
    />
    <div class="flex justify-center items-center gap-2 text-center mb-3">
      <p role="status" :class="{ 'text-error': isProblem }">{{ status }}</p>
      <button v-if="phase === 'notConverted'" class="daisy-btn daisy-btn-sm retry-button" @click="retry">Retry</button>
    </div>
    <div
      v-if="errors && phase !== 'notConverted'"
      class="daisy-alert"
      :class="errors.conversion ? 'daisy-alert-error' : 'daisy-alert-info'"
    >{{ errors.conversion ?? errors }}</div>
    <div class="button-group">
      <button
        v-if="!isRecording"
        class="daisy-btn labeled-action"
        :disabled="phase === 'stopping'"
        @click="startRecording"
      >
        <Mic :size="24" />
        Record
      </button>
      <template v-else>
        <select
          class="device-select"
          :value="selectedDevice"
          @change="onDeviceChange"
          aria-label="Microphone"
        >
          <option v-for="device in audioDevices" :key="device.deviceId" :value="device.deviceId">
            {{ device.label || `Microphone ${device.deviceId.slice(0, 4)}...` }}
          </option>
        </select>
        <button class="daisy-btn labeled-action" @click="stopRecording">
          <Square :size="24" />
          Stop
        </button>
        <button class="daisy-btn labeled-action" @click="audioRecorder.tryFlush()" :disabled="isProcessing">Write text now</button>
      </template>
    </div>
    <div class="secondary-actions">
      <button
        @click="saveAudioLocally(audioFile as Blob)"
        :disabled="isRecording || !audioFile"
      >
        <Download :size="18" />
        Save audio
      </button>
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
import { Download, Mic, Square } from "@lucide/vue"
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
  notConverted:
    "Could not turn your speech into text. Your recording is kept until you close Audio tools.",
  micUnavailable:
    "Could not use the microphone. Allow microphone access in your browser, then try again.",
} as const
const phase = ref<keyof typeof statusByPhase>("ready")
const isRecording = computed(() => phase.value === "recording")
const status = computed(() => statusByPhase[phase.value])
const isProblem = computed(
  () => phase.value === "notConverted" || phase.value === "micUnavailable"
)
const wakeLocker = createWakeLocker()

const { processAudio, isProcessing, startNewRecording, writtenResult } =
  useNoteAudioProcessing(note, errors)

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
  startNewRecording()
  try {
    await wakeLocker.request()
    await audioRecorder.startRecording()
    phase.value = "recording"
  } catch (error) {
    phase.value = "micUnavailable"
    await wakeLocker.release()
  }
}

const stopRecording = async () => {
  phase.value = "stopping"
  try {
    audioFile.value = await audioRecorder.stopRecording()
  } finally {
    const written = await writtenResult()
    if (errors.value?.conversion && audioRecorder.hasUnconvertedAudio()) {
      phase.value = "notConverted"
    } else {
      phase.value = written === "notSaved" ? "ready" : written
    }
    await wakeLocker.release()
  }
}

const retry = () => {
  errors.value = undefined
  return stopRecording()
}

onBeforeUnmount(() => {
  if (isRecording.value) {
    stopRecording()
  }
})
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

.labeled-action {
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

.daisy-btn:disabled,
.secondary-actions :deep(button:disabled) {
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
  border-radius: 8px;
}

.secondary-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-top: 20px;
}

.secondary-actions :deep(button) {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border: none;
  border-radius: 9999px;
  background-color: #2d3748;
  color: white;
  font-size: 14px;
  cursor: pointer;
  transition: background-color 0.3s ease, transform 0.2s ease;
}

.secondary-actions :deep(button:hover:not(:disabled)) {
  background-color: #4a5568;
  transform: scale(1.05);
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
