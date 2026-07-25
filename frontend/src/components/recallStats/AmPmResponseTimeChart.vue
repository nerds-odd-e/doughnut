<template>
  <div data-testid="am-pm-response-time-chart">
    <svg
      :viewBox="`0 0 ${width} ${height}`"
      class="h-auto w-full"
      role="img"
      aria-label="Response time by time of day"
    >
      <g v-for="(t, i) in yTicks" :key="`ygrid-${i}`">
        <line
          class="rs-ampm-grid"
          :x1="padLeft"
          :x2="width - 8"
          :y1="t.y"
          :y2="t.y"
        />
        <text class="rs-ampm-axis" :x="padLeft - 4" :y="t.y + 3" text-anchor="end">
          {{ t.label }}
        </text>
      </g>
      <line
        class="rs-ampm-axis-line"
        :x1="padLeft"
        :x2="padLeft"
        :y1="padTop"
        :y2="padTop + plotHeight"
      />
      <g v-for="(bar, idx) in bars" :key="bar.label">
        <rect
          data-testid="am-pm-bar"
          :data-period="bar.label"
          :x="padLeft + idx * barWidth"
          :y="bar.y"
          :width="barWidth - 4"
          :height="bar.height"
          :class="bar.avgMs == null ? 'rs-ampm-empty' : 'rs-ampm-bar'"
        />
        <text
          v-if="bar.avgMs != null"
          class="rs-ampm-value"
          :x="padLeft + idx * barWidth + barWidth / 2"
          :y="bar.y - 4"
          text-anchor="middle"
        >
          {{ formatSeconds(bar.avgMs / 1000) }}
        </text>
        <text
          class="rs-ampm-label"
          :x="padLeft + idx * barWidth + barWidth / 2"
          :y="height - 4"
          text-anchor="middle"
        >
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
const padLeft = 36
const padBottom = 24
const padTop = 12
const plotWidth = width - padLeft - 8
const plotHeight = height - padBottom - padTop
const barWidth = plotWidth / 4

const formatSeconds = (v: number) =>
  v >= 10 ? `${Math.round(v)}s` : `${v.toFixed(1)}s`

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
      y: padTop + plotHeight - h,
    }
  })
})

const yTicks = computed(() => {
  const max = maxMs.value / 1000
  return [
    { v: 0, label: "0s" },
    { v: max, label: formatSeconds(max) },
  ].map((t) => ({
    y: padTop + plotHeight - (t.v / (max || 1)) * plotHeight,
    label: t.label,
  }))
})
</script>

<style scoped>
.rs-ampm-bar {
  fill: var(--color-primary);
}
.rs-ampm-empty {
  fill: color-mix(in oklab, var(--color-base-content) 25%, transparent);
}
.rs-ampm-grid {
  stroke: var(--color-base-content);
  stroke-opacity: 0.12;
  stroke-width: 1;
}
.rs-ampm-axis-line {
  stroke: var(--color-base-content);
  stroke-opacity: 0.4;
  stroke-width: 1;
}
.rs-ampm-axis {
  fill: var(--color-base-content);
  font-size: 11px;
  opacity: 0.7;
}
.rs-ampm-label {
  fill: var(--color-base-content);
  font-size: 12px;
}
.rs-ampm-value {
  fill: var(--color-base-content);
  font-size: 11px;
  opacity: 0.85;
}
</style>
