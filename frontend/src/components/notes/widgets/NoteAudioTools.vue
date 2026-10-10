<template>
  <section :aria-label="noteMoreOptionsTitles.audio">
    <Waveform
      class="mb-5"
      :audioRecorder="audioRecorder"
      :isRecording="isRecording"
    />
    <div class="button-group">
      <button v-if="hasKeptRecording" class="daisy-btn labeled-action" @click="retry">Retry</button>
      <button
        v-if="!isRecording"
        class="daisy-btn labeled-action"
        :disabled="phase === 'stopping'"
        @click="start"
      >
        <Mic :size="24" />
        Record
      </button>
      <template v-else>
        <select
          class="device-select"
          :value="selectedDevice"
          @change="switchAudioDevice(($event.target as HTMLSelectElement).value)"
          aria-label="Microphone"
        >
          <option v-for="device in audioDevices" :key="device.deviceId" :value="device.deviceId">
            {{ device.label || `Microphone ${device.deviceId.slice(0, 4)}...` }}
          </option>
        </select>
        <button class="daisy-btn labeled-action" @click="stop">
          <Square :size="24" />
          Stop
        </button>
        <button class="daisy-btn labeled-action" @click="tryFlush" :disabled="isProcessing">Write text now</button>
      </template>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import type { Note } from "@generated/donut-backend-api"
import Waveform from "./Waveform.vue"
import { noteMoreOptionsTitles } from "./noteMoreOptionsTitles"
import { Mic, Square } from "@lucide/vue"
import { useNoteVoiceInput } from "@/composables/useNoteVoiceInput"

const { note } = defineProps({
  note: { type: Object as PropType<Note>, required: true },
})

const {
  phase,
  isRecording,
  hasKeptRecording,
  isProcessing,
  audioRecorder,
  wakeLocker,
  processAudio,
  audioDevices,
  selectedDevice,
  start,
  stop,
  retry,
  tryFlush,
  switchAudioDevice,
} = useNoteVoiceInput(note)

defineExpose({ wakeLocker, processAudio })
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
