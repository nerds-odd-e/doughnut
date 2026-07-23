<template>
  <div data-testid="recall-activity-calendar">
    <svg
      :width="cellSize * 53 + labelWidth"
      :height="cellSize * 7 + labelHeight"
      role="img"
      aria-label="Recall activity calendar"
    >
      <g :transform="`translate(${labelWidth}, ${labelHeight})`">
        <rect
          v-for="(entry, i) in calendar"
          :key="entry.date"
          :class="fillClass(entry.count)"
          :x="(i % 53) * cellSize"
          :y="Math.floor(i / 53) * cellSize"
          :width="cellSize - 2"
          :height="cellSize - 2"
          :data-count="entry.count"
          :data-date="entry.date"
          data-testid="calendar-cell"
        >
          <title>{{ entry.date }}: {{ entry.count }} reviews</title>
        </rect>
      </g>
    </svg>
  </div>
</template>

<script setup lang="ts">
import type { DayCount } from "@generated/doughnut-backend-api"

const props = defineProps<{ calendar: DayCount[] }>()

const cellSize = 12
const labelWidth = 0
const labelHeight = 0

const maxCount = () => {
  let max = 0
  for (const entry of props.calendar) {
    if ((entry.count ?? 0) > max) max = entry.count ?? 0
  }
  return max
}

const fillClass = (count?: number) => {
  const c = count ?? 0
  if (c === 0) return "rs-cal-empty"
  const max = maxCount()
  if (max === 0) return "rs-cal-empty"
  const level = Math.ceil((c / max) * 4)
  return `rs-cal-l${Math.min(4, Math.max(1, level))}`
}
</script>

<style scoped>
.rs-cal-empty {
  fill: #ebedf0;
}
.rs-cal-l1 {
  fill: #9be9a8;
}
.rs-cal-l2 {
  fill: #40c463;
}
.rs-cal-l3 {
  fill: #30a14e;
}
.rs-cal-l4 {
  fill: #216e39;
}
</style>
