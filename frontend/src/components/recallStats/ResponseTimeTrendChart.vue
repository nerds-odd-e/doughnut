<template>
  <div data-testid="response-time-trend-chart">
    <svg :width="width" :height="height" role="img" aria-label="Response time trend">
      <polyline
        data-testid="response-time-polyline"
        :points="polylinePoints"
        fill="none"
        stroke="oklch(0.55 0.2 250)"
        stroke-width="2"
      />
      <circle
        v-for="(pt, idx) in insufficientPoints"
        :key="`ins-${idx}`"
        data-testid="response-time-insufficient"
        class="rs-rt-insufficient"
        :cx="pt.x"
        :cy="pt.y"
        r="2"
      />
    </svg>
  </div>
</template>

<script setup lang="ts">
import type { DayAvgResponseTime } from "@generated/doughnut-backend-api"
import { computed } from "vue"

const props = defineProps<{ trend: DayAvgResponseTime[] }>()

const width = 600
const height = 200
const padLeft = 40
const padBottom = 20
const padTop = 10

const sufficient = computed(() =>
  props.trend.filter((d) => d.avgMs != null && d.avgMs !== undefined)
)

const maxSeconds = computed(() => {
  let max = 0
  for (const d of sufficient.value) {
    const sec = (d.avgMs ?? 0) / 1000
    if (sec > max) max = sec
  }
  return max === 0 ? 1 : max
})

const plotWidth = width - padLeft - 10
const plotHeight = height - padBottom - padTop

const xFor = (i: number, total: number) =>
  padLeft + (total <= 1 ? 0 : (i / (total - 1)) * plotWidth)

const yFor = (ms: number) =>
  height - padBottom - (ms / 1000 / maxSeconds.value) * plotHeight

const polylinePoints = computed(() => {
  const total = sufficient.value.length
  return sufficient.value
    .map((d, i) => `${xFor(i, total)},${yFor(d.avgMs ?? 0)}`)
    .join(" ")
})

const insufficientPoints = computed(() => {
  const points: { x: number; y: number }[] = []
  const total = props.trend.length
  let suffIdx = 0
  props.trend.forEach((d, i) => {
    if (d.avgMs == null) {
      points.push({ x: xFor(i, total), y: height - padBottom })
      suffIdx++
    } else {
      suffIdx++
    }
  })
  return points
})
</script>

<style scoped>
.rs-rt-insufficient {
  fill: oklch(0.7 0 0 / 0.4);
}
</style>
