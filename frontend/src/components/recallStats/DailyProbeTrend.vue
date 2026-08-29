<template>
  <section data-testid="daily-probe-trend" class="mt-6">
    <h3 class="mt-0 mb-2 text-base font-semibold">Daily probe</h3>
    <div class="grid md:grid-cols-3 gap-4">
      <div data-testid="daily-probe-speed-chart">
        <TrendLineChart
          :values="speeds"
          polyline-testid="daily-probe-speed-polyline"
          insufficient-testid="daily-probe-speed-insufficient"
          stroke="var(--color-primary)"
          title="Speed"
          :x-labels="dates"
          :format-y="formatRate"
        />
      </div>
      <div data-testid="daily-probe-lapses-chart">
        <TrendLineChart
          :values="lapses"
          polyline-testid="daily-probe-lapses-polyline"
          insufficient-testid="daily-probe-lapses-insufficient"
          stroke="var(--color-primary)"
          title="Lapses"
          :x-labels="dates"
          :format-y="formatCount"
        />
      </div>
      <div data-testid="daily-probe-variability-chart">
        <TrendLineChart
          :values="variabilities"
          polyline-testid="daily-probe-variability-polyline"
          insufficient-testid="daily-probe-variability-insufficient"
          stroke="var(--color-primary)"
          title="Variability"
          :x-labels="dates"
          :format-y="formatRate"
        />
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { DailyProbeDay } from "@generated/donut-backend-api"
import { computed } from "vue"
import TrendLineChart from "./TrendLineChart.vue"

const props = defineProps<{ points: DailyProbeDay[] }>()

const dates = computed(() => props.points.map((day) => day.date ?? ""))
const speeds = computed(() => props.points.map((day) => day.speed ?? null))
const lapses = computed(() => props.points.map((day) => day.lapses ?? null))
const variabilities = computed(() =>
  props.points.map((day) => day.variability ?? null)
)

const formatRate = (value: number) => value.toFixed(2)
const formatCount = (value: number) => String(Math.round(value))
</script>
