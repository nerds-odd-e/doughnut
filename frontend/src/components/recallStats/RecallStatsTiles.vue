<template>
  <div class="grid grid-cols-2 md:grid-cols-4 gap-3">
    <div
      class="daisy-stats daisy-stats-primary daisy-bg-success/10 daisy-text-success p-4 rounded-lg md:col-span-2"
      data-testid="retention-pct-tile"
    >
      <div class="text-3xl font-bold">
        {{ formattedRetention }}<span v-if="hasRetention">%</span>
      </div>
      <div class="text-xs opacity-70">
        Retention (last 365d)<span v-if="hasRetention"> · {{ totals.totalReviews365 }} answered</span>
      </div>
    </div>

    <div class="daisy-stats daisy-bg-base-200 p-4 rounded-lg" data-testid="total-reviews-365-tile">
      <div class="text-2xl font-bold">{{ totals.totalReviews365 }}</div>
      <div class="text-xs opacity-70">Reviews (last 365d)</div>
    </div>

    <div class="daisy-stats daisy-bg-base-200 p-4 rounded-lg" data-testid="reviews-today-tile">
      <div class="text-2xl font-bold">{{ totals.reviewsToday }}</div>
      <div class="text-xs opacity-70">Reviews today</div>
    </div>

    <div class="daisy-stats daisy-bg-base-200 p-4 rounded-lg" data-testid="current-streak-tile">
      <div class="text-2xl font-bold">{{ totals.currentStreak }}</div>
      <div class="text-xs opacity-70">Current streak (days)</div>
    </div>

    <div class="daisy-stats daisy-bg-base-200 p-4 rounded-lg" data-testid="longest-streak-tile">
      <div class="text-2xl font-bold">{{ totals.longestStreak }}</div>
      <div class="text-xs opacity-70">Longest streak (days)</div>
    </div>

    <div class="daisy-stats daisy-bg-base-200 p-4 rounded-lg" data-testid="total-reviews-all-time-tile">
      <div class="text-2xl font-bold">{{ totals.totalReviewsAllTime }}</div>
      <div class="text-xs opacity-70">Reviews (all time)</div>
    </div>

    <div class="daisy-stats daisy-bg-base-200 p-4 rounded-lg" data-testid="total-time-spent-tile">
      <div class="text-2xl font-bold">{{ formattedTimeSpent }}</div>
      <div class="text-xs opacity-70">Time spent reviewing</div>
    </div>

    <div
      v-if="hasBestWorst"
      class="daisy-stats daisy-bg-base-200 p-4 rounded-lg md:col-span-2"
      data-testid="best-worst-hours"
    >
      <div class="text-sm">
        Best review hour:
        <span class="font-semibold">{{ formatHour(totals.bestHour) }}</span>
        ({{ formatPct(totals.bestHourRetentionPct) }} retention)
      </div>
      <div class="text-sm">
        Worst review hour:
        <span class="font-semibold">{{ formatHour(totals.worstHour) }}</span>
        ({{ formatPct(totals.worstHourRetentionPct) }} retention)
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { HeadlineStats } from "@generated/doughnut-backend-api"
import { computed } from "vue"

const props = defineProps<{ totals: HeadlineStats }>()

const hasRetention = computed(
  () =>
    props.totals.retentionPct365 != null &&
    props.totals.retentionPct365 !== undefined
)
const formattedRetention = computed(() =>
  hasRetention.value ? (props.totals.retentionPct365 ?? 0).toFixed(1) : "—"
)
const hasBestWorst = computed(
  () => props.totals.bestHour != null && props.totals.worstHour != null
)

const formattedTimeSpent = computed(() => {
  const ms = props.totals.totalTimeSpentMs ?? 0
  const totalMinutes = Math.floor(ms / 60000)
  const hours = Math.floor(totalMinutes / 60)
  const minutes = totalMinutes % 60
  if (hours === 0) return `${minutes}m`
  return `${hours}h ${minutes}m`
})

const formatHour = (h?: number | null) =>
  h == null ? "—" : `${String(h).padStart(2, "0")}:00`
const formatPct = (pct?: number | null) =>
  pct == null ? "—" : `${pct.toFixed(1)}%`
</script>
