<template>
  <div data-testid="retention-trend-chart">
    <TrendLineChart
      :values="values"
      :y-max="100"
      polyline-testid="retention-trend-polyline"
      insufficient-testid="retention-trend-insufficient"
      stroke="oklch(0.6 0.15 145)"
      aria-label="Retention trend"
    />
  </div>
</template>

<script setup lang="ts">
import type { DayRetention } from "@generated/doughnut-backend-api"
import { computed } from "vue"
import TrendLineChart from "./TrendLineChart.vue"

const props = defineProps<{ retentionTrend: DayRetention[] }>()

// Y-axis is fixed 0–100% (retention is a percentage, never derived from data range).
const values = computed(() =>
  props.retentionTrend.map((d) =>
    d.retentionPct == null ? null : d.retentionPct
  )
)
</script>
