<template>
  <div data-testid="response-time-trend-chart">
    <TrendLineChart
      :values="values"
      polyline-testid="response-time-polyline"
      insufficient-testid="response-time-insufficient"
      stroke="var(--color-primary)"
      title="Response time (s/day)"
      :x-labels="dates"
      :format-y="formatSeconds"
      aria-label="Response time trend"
    />
  </div>
</template>

<script setup lang="ts">
import type { DayAvgResponseTime } from "@generated/doughnut-backend-api"
import { computed } from "vue"
import TrendLineChart from "./TrendLineChart.vue"

const props = defineProps<{ trend: DayAvgResponseTime[] }>()

// Values are plotted in seconds; yMax is auto-derived from the data.
const values = computed(() =>
  props.trend.map((d) => (d.avgMs == null ? null : d.avgMs / 1000))
)
const dates = computed(() => props.trend.map((d) => d.date ?? ""))
const formatSeconds = (v: number) =>
  v >= 10 ? `${Math.round(v)}s` : `${v.toFixed(1)}s`
</script>
