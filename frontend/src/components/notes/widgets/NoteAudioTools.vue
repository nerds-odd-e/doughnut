<template>
  <section :aria-label="noteMoreOptionsTitles.audio">
    <Waveform
      class="mb-5"
      :audioRecorder="audioRecorder"
      :isRecording="isRecording"
    />
    <div class="button-group">
      <button v-if="phase === 'notConverted'" class="daisy-btn labeled-action" @click="stopRecording">Retry</button>
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
import { useToast } from "@/composables/useToast"

const { note } = defineProps({
  note: { type: Object as PropType<Note>, required: true },
})

const phase = ref<"ready" | "recording" | "stopping" | "notConverted">("ready")
const isRecording = computed(() => phase.value === "recording")
const wakeLocker = createWakeLocker()
const { showErrorToast } = useToast()

const { processAudio, isProcessing, lastConversionFailed } =
  useNoteAudioProcessing(note)

const audioRecorder = createAudioRecorder(processAudio)
const audioDevices = audioRecorder.getAudioDevices()
const selectedDevice = audioRecorder.getSelectedDevice()

const onDeviceChange = async (event: Event) => {
  const deviceId = (event.target as HTMLSelectElement).value
  try {
    await audioRecorder.switchAudioDevice(deviceId)
  } catch {
    showErrorToast("Failed to switch audio device")
  }
}

const startRecording = async () => {
  try {
    await wakeLocker.request()
    await audioRecorder.startRecording()
    phase.value = "recording"
  } catch {
    showErrorToast(
      "Could not use the microphone. Allow microphone access in your browser, then try again."
    )
    await wakeLocker.release()
  }
}

const stopRecording = async () => {
  phase.value = "stopping"
  try {
    await audioRecorder.stopRecording()
  } finally {
    phase.value =
      lastConversionFailed.value && audioRecorder.hasUnconvertedAudio()
        ? "notConverted"
        : "ready"
    await wakeLocker.release()
  }
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
