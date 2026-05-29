<template>
  <span
    v-if="summary"
    data-test="assimilation-progress-summary"
    class="text-xs font-normal text-base-content/60"
    :title="`${summary.assimilated} assimilated / ${summary.planned} planned today / ${summary.total} total`"
  >
    {{ summary.assimilated }}/{{ summary.planned }}/{{ summary.total }}
  </span>
</template>

<script setup lang="ts">
import { computed } from "vue"
import {
  assimilationProgressFromCounts,
  useAssimilationCount,
} from "@/composables/useAssimilationCount"

const { dueCount, assimilatedCountOfTheDay, totalUnassimilatedCount } =
  useAssimilationCount()

const summary = computed(() =>
  assimilationProgressFromCounts(
    assimilatedCountOfTheDay.value,
    dueCount.value,
    totalUnassimilatedCount.value
  )
)
</script>
