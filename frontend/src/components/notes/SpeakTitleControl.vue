<template>
  <button
    type="button"
    :class="buttonClass"
    :title="name"
    :aria-label="name"
    :aria-pressed="phase === 'listening' || undefined"
    :disabled="phase === 'converting'"
    @click="onControlClick"
  >
    <LoaderCircle
      v-if="phase === 'converting'"
      class="w-6 h-6 animate-spin"
      aria-hidden="true"
    />
    <Mic v-else class="w-6 h-6" aria-hidden="true" />
  </button>
</template>

<script setup lang="ts">
import { computed, onUnmounted, ref, watch } from "vue"
import { LoaderCircle, Mic } from "@lucide/vue"
import { audioChunkToText } from "@/composables/audioChunkToText"
import { useToast } from "@/composables/useToast"
import {
  MICROPHONE_UNAVAILABLE_MESSAGE,
  SPEECH_NOT_CONVERTED_MESSAGE,
} from "@/composables/voiceInputFailureMessages"
import {
  createAudioRecorder,
  type AudioRecorder,
} from "@/models/audio/audioRecorder"
import type { AudioChunk } from "@/models/audio/audioProcessingScheduler"
import { fieldJoinAppendToggleButtonClass } from "@/utils/fieldJoinAppendButtonClass"
import { toolbarToggleBtnClass } from "./widgets/noteToolbarButtonClasses"

const { joinsField } = defineProps<{
  /** Styled as a button appended to a field control in a `daisy-join`. */
  joinsField?: boolean
}>()

const emit = defineEmits<{
  heardSegments: [segments: string[]]
}>()

const titleSpeechBusy = defineModel<boolean>("busy", { default: false })

const { showErrorToast } = useToast()

const phase = ref<"idle" | "listening" | "converting">("idle")

const name = computed(() =>
  phase.value === "listening" ? "Stop speaking the title" : "Speak the title"
)

const buttonClass = computed(() => {
  const listening = phase.value === "listening"
  return joinsField
    ? `${fieldJoinAppendToggleButtonClass(listening)} rounded-none`
    : toolbarToggleBtnClass(listening)
})

watch(phase, (p) => {
  titleSpeechBusy.value = p !== "idle"
})

let audioRecorder: AudioRecorder | undefined

const processAudio = async (chunk: AudioChunk): Promise<string | undefined> => {
  try {
    const { segmentTexts, endTimestamp } = await audioChunkToText(chunk)
    if (segmentTexts.length) emit("heardSegments", segmentTexts)
    return endTimestamp
  } catch {
    showErrorToast(SPEECH_NOT_CONVERTED_MESSAGE)
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
    audioRecorder = undefined
    showErrorToast(MICROPHONE_UNAVAILABLE_MESSAGE)
  }
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

onUnmounted(() => {
  if (!audioRecorder) return
  const recorder = audioRecorder
  audioRecorder = undefined
  recorder.stopRecording()
})
</script>
