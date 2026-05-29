<template>
  <div
    v-if="progress.show"
    data-testid="assimilation-menu-progress"
    class="assimilation-menu-progress"
  >
    <div
      class="assimilation-menu-progress-fill"
      :style="{ width: `${progress.percent}%` }"
    />
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue"
import { useAssimilationCount } from "@/composables/useAssimilationCount"

const { dueCount, assimilatedCountOfTheDay } = useAssimilationCount()

const progress = computed(() => {
  const assimilated = assimilatedCountOfTheDay.value ?? 0
  const due = dueCount.value ?? 0
  const total = assimilated + due
  return {
    show: assimilated > 0 && due > 0,
    percent: total > 0 ? (assimilated * 100) / total : 0,
  }
})
</script>

<style lang="scss" scoped>
.assimilation-menu-progress {
  width: 100%;
  height: 5px;
  margin-top: 4px;
  border-radius: 4px;
  background-color: #6b7280;
  overflow: hidden;
}

.assimilation-menu-progress-fill {
  height: 100%;
  background-color: #66b0ff;
}
</style>
