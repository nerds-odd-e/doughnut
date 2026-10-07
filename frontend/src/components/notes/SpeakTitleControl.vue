<template>
  <div class="speak-title-control flex flex-col gap-1 items-start">
    <p v-if="status" role="status">{{ status }}</p>
    <button
      type="button"
      class="daisy-btn daisy-btn-sm"
      :disabled="phase === 'converting'"
      @click="onControlClick"
    >
      {{ phase === "listening" ? "Stop" : "Speak the title" }}
    </button>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from "vue"
import { audioChunkToText } from "@/composables/audioChunkToText"
import {
  createAudioRecorder,
  type AudioRecorder,
} from "@/models/audio/audioRecorder"
import type { AudioChunk } from "@/models/audio/audioProcessingScheduler"

const emit = defineEmits<{
  heardSegments: [segments: string[]]
}>()

const statusByPhase = {
  idle: "",
  listening: "Recording. Speak now.",
  converting: "Turning your speech into text…",
} as const

const phase = ref<keyof typeof statusByPhase>("idle")
const status = computed(() => statusByPhase[phase.value])

let audioRecorder: AudioRecorder | undefined

const processAudio = async (chunk: AudioChunk): Promise<string | undefined> => {
  const { segmentTexts, endTimestamp } = await audioChunkToText(chunk)
  if (segmentTexts.length) {
    emit("heardSegments", segmentTexts)
  }
  return endTimestamp
}

const startListening = async () => {
  audioRecorder = createAudioRecorder(processAudio, {
    convertOnlyAtStop: true,
  })
  await audioRecorder.startRecording()
  phase.value = "listening"
}

const stopListening = async () => {
  phase.value = "converting"
  try {
    await audioRecorder?.stopRecording()
  } finally {
    phase.value = "idle"
    audioRecorder = undefined
  }
}

const onControlClick = () => {
  if (phase.value === "listening") {
    return stopListening()
  }
  return startListening()
}
</script>
