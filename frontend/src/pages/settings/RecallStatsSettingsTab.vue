<template>
  <div v-if="stats && hasData">
    <RecallStatsTiles :totals="stats.totals ?? {}" />

    <section class="mt-6">
      <h3 class="mt-0 mb-2 text-base font-semibold">Activity</h3>
      <RecallActivityCalendar :calendar="stats.calendar ?? []" />
    </section>

    <section class="mt-6">
      <div class="mb-2 flex items-center justify-between">
        <h3 class="mt-0 text-base font-semibold">Daily trends</h3>
        <div class="daisy-join">
          <button
            v-for="opt in windowOptions"
            :key="opt"
            type="button"
            class="daisy-join-item daisy-btn daisy-btn-sm"
            :class="window === opt ? 'daisy-btn-active' : ''"
            :data-testid="`trend-window-${opt}`"
            @click="window = opt"
          >
            {{ opt === "all" ? "All" : `${opt}d` }}
          </button>
        </div>
      </div>
      <div class="grid md:grid-cols-2 gap-4">
        <ResponseTimeTrendChart :trend="visibleTrend" />
        <RetentionTrendChart :retention-trend="visibleRetentionTrend" />
      </div>
      <p class="mt-1 text-xs opacity-70">
        <span class="rs-legend-dot">●</span> = insufficient data (fewer than 3 answers)
      </p>
    </section>

    <section class="mt-6">
      <h3 class="mt-0 mb-2 text-base font-semibold">By weekday and hour</h3>
      <div class="grid md:grid-cols-2 gap-4">
        <WeekdayHourHeatmap
          mode="count"
          label="Reviews"
          :counts="stats.weekdayHourCounts ?? []"
        />
        <WeekdayHourHeatmap
          mode="retention"
          label="Retention %"
          :counts="stats.weekdayHourCounts ?? []"
          :correct="stats.weekdayHourCorrect ?? []"
        />
      </div>
    </section>

    <section class="mt-6">
      <h3 class="mt-0 mb-2 text-base font-semibold">Response time by time of day</h3>
      <AmPmResponseTimeChart :am-pm="stats.amPm ?? {}" />
    </section>
  </div>
  <div
    v-else-if="stats"
    data-testid="recall-stats-empty"
    class="mt-6 rounded-box bg-base-200 p-6 text-center"
  >
    <p class="text-base font-semibold">No reviews yet</p>
    <p class="mt-1 text-sm opacity-70">
      Once you start reviewing, your recall stats will appear here.
    </p>
  </div>
  <div
    v-else-if="error"
    data-testid="recall-stats-error"
    class="mt-6 rounded-box bg-base-200 p-6 text-center"
  >
    <p class="text-base font-semibold">Couldn't load your stats</p>
    <button
      type="button"
      class="daisy-btn daisy-btn-sm daisy-btn-primary mt-3"
      data-testid="recall-stats-retry"
      @click="load"
    >
      Try Again
    </button>
  </div>
  <ContentLoader v-else />
</template>

<script setup lang="ts">
import RecallStatsTiles from "@/components/recallStats/RecallStatsTiles.vue"
import RecallActivityCalendar from "@/components/recallStats/RecallActivityCalendar.vue"
import ResponseTimeTrendChart from "@/components/recallStats/ResponseTimeTrendChart.vue"
import RetentionTrendChart from "@/components/recallStats/RetentionTrendChart.vue"
import WeekdayHourHeatmap from "@/components/recallStats/WeekdayHourHeatmap.vue"
import AmPmResponseTimeChart from "@/components/recallStats/AmPmResponseTimeChart.vue"
import ContentLoader from "@/components/commons/ContentLoader.vue"
import { UserController } from "@generated/doughnut-backend-api/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import timezoneParam from "@/managedApi/window/timezoneParam"
import type { RecallStatsDto } from "@generated/doughnut-backend-api"
import { computed, onMounted, ref } from "vue"

const stats = ref<RecallStatsDto | undefined>()
const error = ref(false)
const window = ref<number | "all">(90)
const windowOptions: (number | "all")[] = [30, 90, "all"]

const hasData = computed(
  () => (stats.value?.totals?.totalReviewsAllTime ?? 0) > 0
)

const sliceSize = computed(() =>
  window.value === "all" ? 90 : (window.value as number)
)

const visibleTrend = computed(() => {
  const trend = stats.value?.trend ?? []
  return trend.slice(Math.max(0, trend.length - sliceSize.value))
})

const visibleRetentionTrend = computed(() => {
  const trend = stats.value?.retentionTrend ?? []
  return trend.slice(Math.max(0, trend.length - sliceSize.value))
})

const load = async () => {
  error.value = false
  stats.value = undefined
  const { data, error: apiError } = await apiCallWithLoading(() =>
    UserController.getRecallStats({ query: { timezone: timezoneParam() } })
  )
  if (apiError) {
    error.value = true
  } else {
    stats.value = data
  }
}

onMounted(load)
</script>
