<template>
  <div :data-testid="`weekday-hour-heatmap-${mode}`">
    <svg
      :viewBox="`0 0 ${leftGutter + 24 * cellSize} ${topGutter + 7 * cellSize + bottomGutter}`"
      class="h-auto w-full"
      role="img"
      :aria-label="`${label}`"
    >
      <text class="rs-hm-caption" :x="leftGutter" :y="11">{{ label }}</text>
      <g :transform="`translate(${leftGutter}, ${topGutter})`">
        <rect
          v-for="cell in cells"
          :key="`${cell.wd}-${cell.hr}`"
          :x="cell.hr * cellSize"
          :y="cell.wd * cellSize"
          :width="cellSize - 2"
          :height="cellSize - 2"
          :class="cellClass(cell.wd, cell.hr)"
          :data-weekday="cell.wd"
          :data-hour="cell.hr"
          :data-count="countAt(cell.wd, cell.hr)"
          :data-answered="mode === 'retention' ? countAt(cell.wd, cell.hr) : undefined"
          :data-correct="mode === 'retention' ? correctAt(cell.wd, cell.hr) : undefined"
          data-testid="heatmap-cell"
        >
          <title>{{ weekdayLabel(cell.wd) }} {{ cell.hr }}:00 — {{ cellTitle(cell.wd, cell.hr) }}</title>
        </rect>
      </g>
      <text
        v-for="wd in weekdayLabelRows"
        :key="`wd-${wd}`"
        class="rs-hm-axis"
        :x="leftGutter - 4"
        :y="topGutter + wd * cellSize + cellSize - 2"
        text-anchor="end"
      >
        {{ weekdayLabel(wd) }}
      </text>
      <text
        v-for="hr in hourTicks"
        :key="`hr-${hr}`"
        class="rs-hm-axis"
        :x="leftGutter + hr * cellSize"
        :y="topGutter + 7 * cellSize + 12"
        text-anchor="middle"
      >
        {{ hr }}
      </text>
    </svg>
  </div>
</template>

<script setup lang="ts">
const props = withDefaults(
  defineProps<{
    mode: "count" | "retention"
    counts: number[][]
    correct?: number[][]
    label?: string
  }>(),
  { label: "" }
)

const cellSize = 16
const leftGutter = 28
const topGutter = 16
const bottomGutter = 16

const cells: { wd: number; hr: number }[] = []
for (let wd = 0; wd < 7; wd++) {
  for (let hr = 0; hr < 24; hr++) {
    cells.push({ wd, hr })
  }
}

const weekdayLabelRows = [0, 2, 4]
const hourTicks = [0, 3, 6, 9, 12, 15, 18, 21]

const weekdayLabel = (wd: number) =>
  ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"][wd] ?? `${wd}`

const countAt = (wd: number, hr: number) => props.counts[wd]?.[hr] ?? 0
const correctAt = (wd: number, hr: number) => props.correct?.[wd]?.[hr] ?? 0

const maxCount = () => {
  let max = 0
  for (const row of props.counts) {
    for (const c of row) {
      if ((c ?? 0) > max) max = c ?? 0
    }
  }
  return max
}

const retentionPct = (wd: number, hr: number) => {
  const answered = countAt(wd, hr)
  if (answered === 0) return 0
  return (correctAt(wd, hr) / answered) * 100
}

const cellClass = (wd: number, hr: number) => {
  if (props.mode === "count") {
    const c = countAt(wd, hr)
    if (c === 0) return "rs-hm-c-empty"
    const max = maxCount()
    if (max === 0) return "rs-hm-c-empty"
    const level = Math.min(4, Math.max(1, Math.ceil((c / max) * 4)))
    return `rs-hm-c${level}`
  }
  const answered = countAt(wd, hr)
  if (answered < 3) return "rs-hm-insufficient"
  const pct = retentionPct(wd, hr)
  const level = Math.min(4, Math.max(1, Math.ceil((pct / 100) * 4)))
  return `rs-hm-r${level}`
}

const cellTitle = (wd: number, hr: number) => {
  if (props.mode === "count") return `${countAt(wd, hr)} reviews`
  return `${correctAt(wd, hr)}/${countAt(wd, hr)} correct (${retentionPct(wd, hr).toFixed(0)}%)`
}
</script>

<style scoped>
.rs-hm-c-empty {
  fill: var(--color-base-300);
}
.rs-hm-c1 {
  fill: color-mix(in oklab, var(--color-base-300), var(--color-success) 40%);
}
.rs-hm-c2 {
  fill: color-mix(in oklab, var(--color-base-300), var(--color-success) 65%);
}
.rs-hm-c3 {
  fill: color-mix(in oklab, var(--color-base-300), var(--color-success) 85%);
}
.rs-hm-c4 {
  fill: var(--color-success);
}
.rs-hm-insufficient {
  fill: color-mix(in oklab, var(--color-base-content) 25%, transparent);
}
.rs-hm-r1 {
  fill: color-mix(in oklab, var(--color-base-300), var(--color-primary) 40%);
}
.rs-hm-r2 {
  fill: color-mix(in oklab, var(--color-base-300), var(--color-primary) 65%);
}
.rs-hm-r3 {
  fill: color-mix(in oklab, var(--color-base-300), var(--color-primary) 85%);
}
.rs-hm-r4 {
  fill: var(--color-primary);
}
.rs-hm-caption {
  fill: var(--color-base-content);
  font-size: 12px;
  font-weight: 600;
}
.rs-hm-axis {
  fill: var(--color-base-content);
  font-size: 11px;
  opacity: 0.7;
}
</style>
