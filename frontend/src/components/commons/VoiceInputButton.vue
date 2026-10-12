<template>
  <button
    type="button"
    :title="name"
    :aria-label="name"
    :aria-pressed="state === 'recording' || undefined"
    :disabled="state === 'busy'"
    @click="emit('click')"
  >
    <canvas
      v-if="state === 'recording' && readLevel"
      ref="levelCanvas"
      class="w-6 h-6"
      width="24"
      height="24"
      aria-hidden="true"
    />
    <LoaderCircle
      v-else-if="state === 'busy'"
      class="w-6 h-6 animate-spin"
      aria-hidden="true"
    />
    <RotateCw
      v-else-if="state === 'keptRecording'"
      class="w-6 h-6"
      aria-hidden="true"
    />
    <Mic v-else class="w-6 h-6" aria-hidden="true" />
  </button>
</template>

<script setup lang="ts">
import { computed, ref, watch } from "vue"
import { LoaderCircle, Mic, RotateCw } from "@lucide/vue"

const { state, readLevel, names } = defineProps<{
  state: "idle" | "busy" | "recording" | "keptRecording"
  /** The live level of the running recording; the waveform is drawn from it. */
  readLevel?: () => number
  names: { start: string; stop: string; retry: string }
}>()

const emit = defineEmits<{ click: [] }>()

const name = computed(() => {
  if (state === "recording") return names.stop
  if (state === "keptRecording") return names.retry
  return names.start
})

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

  const level = Math.min(1, Math.abs(readLevel!()) * 4)
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
</script>
