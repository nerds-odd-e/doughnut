<template>
  <div class="speak-title-control flex flex-col gap-1 items-start">
    <p v-if="status" role="status" :class="{ 'text-error': isProblem }">
      {{ status }}
    </p>
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
import { computed, onUnmounted, ref, watch } from "vue"
import { audioChunkToText } from "@/composables/audioChunkToText"
import {
  createAudioRecorder,
  type AudioRecorder,
} from "@/models/audio/audioRecorder"
import type { AudioChunk } from "@/models/audio/audioProcessingScheduler"

const emit = defineEmits<{
  heardSegments: [segments: string[]]
}>()

const titleSpeechBusy = defineModel<boolean>("busy", { default: false })

const statusByPhase = {
  idle: "",
  listening: "Recording. Speak now.",
  converting: "Turning your speech into text…",
  nothingHeard: "No speech was turned into text.",
  micUnavailable:
    "Could not use the microphone. Allow microphone access in your browser, then try again.",
  conversionFailed: "Could not turn your speech into text.",
} as const

const phase = ref<keyof typeof statusByPhase>("idle")
const status = computed(() => statusByPhase[phase.value])
const isProblem = computed(
  () => phase.value === "micUnavailable" || phase.value === "conversionFailed"
)

watch(phase, (p) => {
  titleSpeechBusy.value = p === "listening" || p === "converting"
})

const phaseAfterConversion = {
  heard: "idle",
  nothing: "nothingHeard",
  failed: "conversionFailed",
} as const

let audioRecorder: AudioRecorder | undefined
let conversionOutcome: keyof typeof phaseAfterConversion = "nothing"

const processAudio = async (chunk: AudioChunk): Promise<string | undefined> => {
  try {
    const { segmentTexts, endTimestamp } = await audioChunkToText(chunk)
    if (segmentTexts.length) {
      emit("heardSegments", segmentTexts)
      conversionOutcome = "heard"
    }
    return endTimestamp
  } catch {
    conversionOutcome = "failed"
    return undefined
  }
}

const startListening = async () => {
  audioRecorder = createAudioRecorder(processAudio, {
    convertOnlyAtStop: true,
  })
  try {
    await audioRecorder.startRecording()
    phase.value = "listening"
  } catch {
    phase.value = "micUnavailable"
    audioRecorder = undefined
  }
}

const stopListening = async () => {
  phase.value = "converting"
  conversionOutcome = "nothing"
  try {
    await audioRecorder?.stopRecording()
  } finally {
    phase.value = phaseAfterConversion[conversionOutcome]
    audioRecorder = undefined
  }
}

const onControlClick = () => {
  if (phase.value === "listening") {
    return stopListening()
  }
  return startListening()
}

onUnmounted(() => {
  if (!audioRecorder) return
  const recorder = audioRecorder
  audioRecorder = undefined
  recorder.stopRecording()
})
</script>
