<template>
  <button
    type="button"
    :class="toolbarToggleBtnClass(isRecording)"
    :title="name"
    :aria-label="name"
    :aria-pressed="isRecording || undefined"
    :disabled="phase === 'stopping'"
    @click="isRecording ? stop() : start()"
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
    <Mic v-else class="w-6 h-6" aria-hidden="true" />
  </button>
</template>

<script setup lang="ts">
import { computed, ref, watch } from "vue"
import type { Note } from "@generated/donut-backend-api"
import { LoaderCircle, Mic } from "@lucide/vue"
import { useNoteVoiceInput } from "@/composables/useNoteVoiceInput"
import { noteVoiceInputTitles } from "./noteMoreOptionsTitles"
import { toolbarToggleBtnClass } from "./noteToolbarButtonClasses"

const { note } = defineProps<{ note: Note }>()

const {
  phase,
  isRecording,
  audioRecorder,
  wakeLocker,
  processAudio,
  start,
  stop,
} = useNoteVoiceInput(note)

const name = computed(() =>
  isRecording.value ? noteVoiceInputTitles.stop : noteVoiceInputTitles.start
)

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
