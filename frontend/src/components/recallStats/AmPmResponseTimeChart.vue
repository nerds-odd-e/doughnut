<template>
  <div data-testid="am-pm-response-time-chart">
    <svg :width="width" :height="height" role="img" aria-label="Morning vs afternoon response time">
      <g v-for="(bar, idx) in bars" :key="bar.label">
        <rect
          data-testid="am-pm-bar"
          :data-period="bar.label"
          :x="idx * barWidth"
          :y="bar.y"
          :width="barWidth - 4"
          :height="bar.height"
          :class="bar.avgMs == null ? 'rs-ampm-empty' : 'rs-ampm-bar'"
        />
        <text :x="idx * barWidth + barWidth / 2 - 8" :y="height - 4" class="rs-ampm-label">
          {{ bar.label }}
        </text>
      </g>
    </svg>
  </div>
</template>

<script setup lang="ts">
import type { AmPmResponseTime } from "@generated/doughnut-backend-api"
import { computed } from "vue"

const props = defineProps<{ amPm: AmPmResponseTime }>()

const width = 320
const height = 160
const barWidth = width / 4
const plotHeight = height - 24

const maxMs = computed(() => {
  const vals = [
    props.amPm.morningMs,
    props.amPm.afternoonMs,
    props.amPm.eveningMs,
    props.amPm.nightMs,
  ].filter((v) => v != null) as number[]
  return vals.length === 0 ? 1 : Math.max(...vals)
})

const bars = computed(() => {
  const periods: { label: string; ms?: number | null }[] = [
    { label: "Morning", ms: props.amPm.morningMs },
    { label: "Afternoon", ms: props.amPm.afternoonMs },
    { label: "Evening", ms: props.amPm.eveningMs },
    { label: "Night", ms: props.amPm.nightMs },
  ]
  return periods.map((p) => {
    const h = p.ms == null ? 0 : (p.ms / maxMs.value) * plotHeight
    return {
      label: p.label,
      avgMs: p.ms,
      height: h,
      y: height - 24 - h,
    }
  })
})
</script>

<style scoped>
.rs-ampm-bar {
  fill: oklch(0.55 0.2 250);
}
.rs-ampm-empty {
  fill: oklch(0.7 0 0 / 0.25);
}
.rs-ampm-label {
  fill: oklch(0.4 0 0 0.7);
  font-size: 10px;
}
</style>
