<template>
  <div
    class="daisy-stats daisy-bg-base-200 p-4 rounded-lg"
    data-testid="pace-tile"
  >
    <template v-if="hasPace">
      <div class="text-2xl font-bold">{{ paceLabel }}</div>
      <div class="text-xs opacity-70">
        based on {{ pace.sampleSize }} of {{ pace.totalAnsweredToday }}
        answered this morning
      </div>
    </template>
    <template v-else>
      <div class="text-sm opacity-70">
        Not enough recall history yet for a pace comparison
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import type { PaceStats } from "@generated/donut-backend-api"
import { computed } from "vue"

const props = defineProps<{ pace: PaceStats }>()

// Below this magnitude, today's pace reads as "about your usual" rather than slower/faster.
const NEUTRAL_PCT_THRESHOLD = 1

const hasPace = computed(() => (props.pace.sampleSize ?? 0) > 0)

const paceLabel = computed(() => {
  const pct = props.pace.pctVsUsual ?? 0
  if (Math.abs(pct) < NEUTRAL_PCT_THRESHOLD) {
    return "About your usual pace"
  }
  const direction = pct > 0 ? "slower" : "faster"
  return `${Math.abs(pct).toFixed(0)}% ${direction} than usual`
})
</script>
