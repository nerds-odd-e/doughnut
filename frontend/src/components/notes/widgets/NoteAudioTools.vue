<template>
  <section :aria-label="noteMoreOptionsTitles.audio">
    <Waveform
      class="mb-5"
      :audioRecorder="audioRecorder"
      :isRecording="isRecording"
    />
    <div v-if="problem" class="flex justify-center items-center gap-2 text-center mb-3">
      <p class="text-error">{{ problem }}</p>
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
  </section>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref, type PropType } from "vue"
import { createAudioRecorder } from "../../../models/audio/audioRecorder"
import { createWakeLocker } from "../../../models/wakeLocker"
import type { Note } from "@generated/donut-backend-api"
import Waveform from "./Waveform.vue"
import { noteMoreOptionsTitles } from "./noteMoreOptionsTitles"
import { Mic, Square } from "@lucide/vue"
import { useNoteAudioProcessing } from "@/composables/useNoteAudioProcessing"

const { note } = defineProps({
  note: { type: Object as PropType<Note>, required: true },
})

const errors = ref<Record<string, string | undefined>>()
const problemByPhase = {
  notConverted:
    "Could not turn your speech into text. Your recording is kept until you close Audio tools.",
  micUnavailable:
    "Could not use the microphone. Allow microphone access in your browser, then try again.",
} as const
const phase = ref<
  "ready" | "recording" | "stopping" | keyof typeof problemByPhase
>("ready")
const isRecording = computed(() => phase.value === "recording")
const problem = computed(() =>
  phase.value in problemByPhase
    ? problemByPhase[phase.value as keyof typeof problemByPhase]
    : undefined
)
const wakeLocker = createWakeLocker()

const { processAudio, isProcessing } = useNoteAudioProcessing(note, errors)

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
    await audioRecorder.stopRecording()
  } finally {
    phase.value =
      errors.value?.conversion && audioRecorder.hasUnconvertedAudio()
        ? "notConverted"
        : "ready"
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
  border-radius: 8px;
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
