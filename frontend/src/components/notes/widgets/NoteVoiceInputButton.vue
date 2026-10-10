<template>
  <button
    type="button"
    :class="
      hasKeptRecording
        ? toolbarKeptRecordingBtnClass
        : toolbarToggleBtnClass(isRecording)
    "
    :title="name"
    :aria-label="name"
    :aria-pressed="isRecording || undefined"
    :disabled="phase === 'stopping'"
    @click="click"
  >
    <canvas
      v-if="isRecording"
      ref="levelCanvas"
      class="w-6 h-6"
      width="24"
      height="24"
      aria-hidden="true"
    />
    <LoaderCircle
      v-else-if="phase === 'stopping'"
      class="w-6 h-6 animate-spin"
      aria-hidden="true"
    />
    <RotateCw
      v-else-if="hasKeptRecording"
      class="w-6 h-6"
      aria-hidden="true"
    />
    <Mic v-else class="w-6 h-6" aria-hidden="true" />
  </button>
</template>

<script setup lang="ts">
import { computed, ref, watch } from "vue"
import type { Note } from "@generated/donut-backend-api"
import { LoaderCircle, Mic, RotateCw } from "@lucide/vue"
import { useNoteVoiceInput } from "@/composables/useNoteVoiceInput"
import { noteVoiceInputTitles } from "./noteMoreOptionsTitles"
import {
  toolbarKeptRecordingBtnClass,
  toolbarToggleBtnClass,
} from "./noteToolbarButtonClasses"

const { note } = defineProps<{ note: Note }>()

const {
  phase,
  isRecording,
  hasKeptRecording,
  audioRecorder,
  wakeLocker,
  processAudio,
  start,
  stop,
  retry,
} = useNoteVoiceInput(note)

const name = computed(() => {
  if (isRecording.value) return noteVoiceInputTitles.stop
  if (hasKeptRecording.value) return noteVoiceInputTitles.retry
  return noteVoiceInputTitles.start
})

const click = () => {
  if (isRecording.value) return stop()
  if (hasKeptRecording.value) return retry()
  return start()
}

const barWidth = 2
const barStep = 3
const framesPerBar = 6
const levelCanvas = ref<HTMLCanvasElement | null>(null)
let levels: number[] = []
let frame = 0
let animationId = 0

const drawLevels = () => {
  const canvas = levelCanvas.value
  const ctx = canvas?.getContext("2d")
  if (!canvas || !ctx) return

  const level = Math.min(1, Math.abs(audioRecorder.getAudioData()) * 4)
  if (frame % framesPerBar === 0) {
    levels = [...levels, level].slice(-Math.floor(canvas.width / barStep))
  }
  frame += 1

  ctx.clearRect(0, 0, canvas.width, canvas.height)
  ctx.fillStyle = getComputedStyle(canvas).color
  const firstX = canvas.width - levels.length * barStep
  levels.forEach((barLevel, index) => {
    const height = Math.max(barWidth, barLevel * canvas.height)
    ctx.fillRect(
      firstX + index * barStep,
      (canvas.height - height) / 2,
      barWidth,
      height
    )
  })

  animationId = requestAnimationFrame(drawLevels)
}

watch(levelCanvas, (canvas) => {
  cancelAnimationFrame(animationId)
  levels = []
  frame = 0
  if (canvas) drawLevels()
})

defineExpose({ start, wakeLocker, processAudio })
</script>
