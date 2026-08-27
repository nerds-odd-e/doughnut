<template>
  <div
    class="daisy-stats daisy-bg-base-200 p-4 rounded-lg"
    data-testid="accuracy-tile"
  >
    <template v-if="hasAccuracy">
      <div class="text-2xl font-bold">{{ accuracyLabel }}</div>
      <div class="text-xs opacity-70">
        based on {{ accuracy.sampleSize }} answered this morning
      </div>
    </template>
    <template v-else>
      <div class="text-sm opacity-70">
        Not enough recall history yet for an accuracy comparison
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import type { AccuracyStats } from "@generated/donut-backend-api"
import { computed } from "vue"

const props = defineProps<{ accuracy: AccuracyStats }>()

// Below this magnitude, today's standardized residual reads as "about as expected" rather than
// meaningfully better/worse.
const NEUTRAL_RESIDUAL_THRESHOLD = 0.5

const hasAccuracy = computed(() => props.accuracy.standardizedResidual != null)

const accuracyLabel = computed(() => {
  const residual = props.accuracy.standardizedResidual ?? 0
  if (Math.abs(residual) < NEUTRAL_RESIDUAL_THRESHOLD) {
    return "Recalling about as expected"
  }
  const direction = residual > 0 ? "better" : "worse"
  return `Recalling ${direction} than expected`
})
</script>
