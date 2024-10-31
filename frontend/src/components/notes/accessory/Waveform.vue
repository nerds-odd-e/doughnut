<template>
  <canvas ref="waveformCanvas" class="waveform-canvas"></canvas>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch } from "vue"
import type { AudioRecorder } from "../../../models/audio/recording"

const props = defineProps<{
  audioRecorder: AudioRecorder
  isRecording: boolean
}>()

const waveformCanvas = ref<HTMLCanvasElement | null>(null)
let animationId: number | null = null

function drawWaveform() {
  if (!waveformCanvas.value) return

  const canvas = waveformCanvas.value
  const ctx = canvas.getContext("2d")
  if (!ctx) return

  const audioData = props.audioRecorder.getAudioData()
  const dataLength = audioData.length
  const bufferLength = 3
  const start = dataLength > bufferLength ? dataLength - bufferLength : 0
  const data = audioData.slice(start, dataLength)

  // Shift canvas content to the left
  ctx.drawImage(canvas, -1, 0)

  // Clear the rightmost column
  ctx.fillStyle = "#ffffff"
  ctx.fillRect(canvas.width - 1, 0, 1, canvas.height)

  // Draw new data on the right edge
  const height = canvas.height

  // Compute average of data
  let sum = 0
  for (let i = 0; i < data.length; i++) {
    const channel = data[i]
    if (!channel) continue
    for (let j = 0; j < channel.length; j++) {
      sum += channel[j]!
    }
  }

  const avgSample = sum / data.length

  const y = height - Math.abs(avgSample) * height

  ctx.fillStyle = "#4299e1"
  ctx.fillRect(canvas.width - 1, y, 1, height - y)

  // Schedule next frame
  animationId = requestAnimationFrame(drawWaveform)
}

watch(
  () => props.isRecording,
  (newValue) => {
    if (newValue) {
      if (!animationId) {
        drawWaveform()
      }
    } else if (animationId) {
      cancelAnimationFrame(animationId)
      animationId = null
    }
  }
)

onMounted(() => {
  const canvas = waveformCanvas.value
  if (canvas) {
    // Set canvas dimensions to match its display size
    const rect = canvas.getBoundingClientRect()
    canvas.width = rect.width
    canvas.height = rect.height
  }
})

onUnmounted(() => {
  if (animationId) {
    cancelAnimationFrame(animationId)
    animationId = null
  }
})
</script>

<style scoped>
.waveform-canvas {
  width: 100%;
  height: 50px;
  background-color: #e2e8f0;
  border-radius: 8px;
}
</style>
