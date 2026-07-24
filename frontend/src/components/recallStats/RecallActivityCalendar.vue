<template>
  <div data-testid="recall-activity-calendar">
    <svg
      :viewBox="`0 0 ${cellSize * cols + labelWidth} ${cellSize * 7 + labelHeight}`"
      class="h-auto w-full"
      role="img"
      aria-label="Recall activity calendar"
    >
      <g :transform="`translate(${labelWidth}, ${labelHeight})`">
        <rect
          v-for="cell in cells"
          :key="`${cell.col}-${cell.row}`"
          :class="fillClass(cell.count, cell.inRange)"
          :x="cell.col * cellSize"
          :y="cell.row * cellSize"
          :width="cellSize - 2"
          :height="cellSize - 2"
          :data-count="cell.inRange ? cell.count : undefined"
          :data-date="cell.date"
          data-testid="calendar-cell"
        >
          <title v-if="cell.inRange">{{ cell.date }}: {{ cell.count }} reviews</title>
        </rect>
      </g>
      <text
        v-for="r in weekdayLabelRows"
        :key="`wd-${r}`"
        class="rs-cal-axis"
        :x="labelWidth - 4"
        :y="labelHeight + r * cellSize + cellSize - 2"
        text-anchor="end"
      >
        {{ weekdayLabel(r) }}
      </text>
      <text
        v-for="m in monthLabels"
        :key="`mo-${m.col}`"
        class="rs-cal-axis"
        :x="labelWidth + m.col * cellSize"
        :y="labelHeight - 4"
      >
        {{ m.label }}
      </text>
    </svg>
  </div>
</template>

<script setup lang="ts">
import type { DayCount } from "@generated/doughnut-backend-api"
import { computed } from "vue"

const props = defineProps<{ calendar: DayCount[] }>()

const cellSize = 12
const labelWidth = 28
const labelHeight = 16

const WEEKDAY_LABELS = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"]
const MONTH_LABELS = [
  "Jan",
  "Feb",
  "Mar",
  "Apr",
  "May",
  "Jun",
  "Jul",
  "Aug",
  "Sep",
  "Oct",
  "Nov",
  "Dec",
]

const weekdayOf = (date?: string) => {
  if (!date) return 0
  const [y = 1970, m = 1, d = 1] = date.split("-").map(Number)
  return new Date(Date.UTC(y, m - 1, d)).getUTCDay()
}
const epochDayOf = (date?: string) => {
  if (!date) return 0
  const [y = 1970, m = 1, d = 1] = date.split("-").map(Number)
  return Math.round(Date.UTC(y, m - 1, d) / 86_400_000)
}
const isoFromEpochDay = (ed: number) => {
  const dt = new Date(ed * 86_400_000)
  const y = dt.getUTCFullYear()
  const m = String(dt.getUTCMonth() + 1).padStart(2, "0")
  const d = String(dt.getUTCDate()).padStart(2, "0")
  return `${y}-${m}-${d}`
}

const layout = computed(() => {
  const byDay = new Map<number, number>()
  let minDay = Number.POSITIVE_INFINITY
  let maxDay = Number.NEGATIVE_INFINITY
  for (const e of props.calendar) {
    const ed = epochDayOf(e.date)
    byDay.set(ed, e.count ?? 0)
    if (ed < minDay) minDay = ed
    if (ed > maxDay) maxDay = ed
  }
  if (props.calendar.length === 0) {
    return { cols: 0, firstSunday: 0, minDay: 0, maxDay: 0, byDay }
  }
  const first = props.calendar[0]
  const last = props.calendar[props.calendar.length - 1]
  if (!first || !last) {
    return { cols: 0, firstSunday: 0, minDay: 0, maxDay: 0, byDay }
  }
  const firstSunday = minDay - weekdayOf(first.date)
  const lastSunday = maxDay - weekdayOf(last.date)
  return {
    cols: (lastSunday - firstSunday) / 7 + 1,
    firstSunday,
    minDay,
    maxDay,
    byDay,
  }
})

const cols = computed(() => layout.value.cols)

const cells = computed(() => {
  const { cols: c, firstSunday, minDay, maxDay, byDay } = layout.value
  const out: {
    col: number
    row: number
    count: number
    date: string
    inRange: boolean
  }[] = []
  for (let col = 0; col < c; col++) {
    for (let row = 0; row < 7; row++) {
      const ed = firstSunday + col * 7 + row
      const inRange = ed >= minDay && ed <= maxDay
      out.push({
        col,
        row,
        count: byDay.get(ed) ?? 0,
        date: isoFromEpochDay(ed),
        inRange,
      })
    }
  }
  return out
})

const fillClass = (count: number, inRange: boolean) => {
  if (!inRange) return "rs-cal-empty"
  if (count === 0) return "rs-cal-empty"
  let max = 0
  for (const e of props.calendar) {
    if ((e.count ?? 0) > max) max = e.count ?? 0
  }
  if (max === 0) return "rs-cal-empty"
  const level = Math.ceil((count / max) * 4)
  return `rs-cal-l${Math.min(4, Math.max(1, level))}`
}

const weekdayLabelRows = [1, 3, 5]
const weekdayLabel = (r: number) => WEEKDAY_LABELS[r]

const monthLabels = computed(() => {
  const { cols: c, firstSunday } = layout.value
  const labels: { col: number; label: string }[] = []
  let prevMonth = -1
  for (let col = 0; col < c; col++) {
    const sundayEd = firstSunday + col * 7
    const month = new Date(sundayEd * 86_400_000).getUTCMonth()
    if (month !== prevMonth) {
      labels.push({ col, label: MONTH_LABELS[month] ?? "" })
      prevMonth = month
    }
  }
  return labels
})
</script>

<style scoped>
.rs-cal-empty {
  fill: var(--color-base-300);
}
.rs-cal-l1 {
  fill: color-mix(in oklab, var(--color-base-300), var(--color-success) 40%);
}
.rs-cal-l2 {
  fill: color-mix(in oklab, var(--color-base-300), var(--color-success) 65%);
}
.rs-cal-l3 {
  fill: color-mix(in oklab, var(--color-base-300), var(--color-success) 85%);
}
.rs-cal-l4 {
  fill: var(--color-success);
}
.rs-cal-axis {
  fill: var(--color-base-content);
  font-size: 11px;
  opacity: 0.7;
}
</style>
