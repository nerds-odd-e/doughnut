<template>
  <div data-testid="retention-trend-chart">
    <svg :width="width" :height="height" role="img" aria-label="Retention trend">
      <polyline
        data-testid="retention-trend-polyline"
        :points="polylinePoints"
        fill="none"
        stroke="oklch(0.6 0.15 145)"
        stroke-width="2"
      />
      <circle
        v-for="(pt, idx) in insufficientPoints"
        :key="`rins-${idx}`"
        data-testid="retention-trend-insufficient"
        class="rs-rt-insufficient"
        :cx="pt.x"
        :cy="pt.y"
        r="2"
      />
    </svg>
  </div>
</template>

<script setup lang="ts">
import type { DayRetention } from "@generated/doughnut-backend-api"
import { computed } from "vue"

const props = defineProps<{ retentionTrend: DayRetention[] }>()

const width = 600
const height = 200
const padLeft = 40
const padBottom = 20
const padTop = 10
const plotWidth = width - padLeft - 10
const plotHeight = height - padBottom - padTop

// Y-axis is fixed 0-100% (retention is a percentage, never derived from data range).
const yFor = (pct: number) => height - padBottom - (pct / 100) * plotHeight

const sufficient = computed(() =>
  props.retentionTrend.filter((d) => d.retentionPct != null)
)

const xFor = (i: number, total: number) =>
  padLeft + (total <= 1 ? 0 : (i / (total - 1)) * plotWidth)

const polylinePoints = computed(() => {
  const total = sufficient.value.length
  return sufficient.value
    .map((d, i) => `${xFor(i, total)},${yFor(d.retentionPct ?? 0)}`)
    .join(" ")
})

const insufficientPoints = computed(() => {
  const points: { x: number; y: number }[] = []
  const total = props.retentionTrend.length
  props.retentionTrend.forEach((d, i) => {
    // Insufficient days (answeredCount < 3 -> retentionPct null) are greyed,
    // never drawn as 0% or 100%.
    if (d.retentionPct == null) {
      points.push({ x: xFor(i, total), y: height - padBottom })
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
