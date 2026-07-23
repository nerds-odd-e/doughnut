<template>
  <div data-testid="response-time-trend-chart">
    <TrendLineChart
      :values="values"
      polyline-testid="response-time-polyline"
      insufficient-testid="response-time-insufficient"
      stroke="oklch(0.55 0.2 250)"
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
</script>
