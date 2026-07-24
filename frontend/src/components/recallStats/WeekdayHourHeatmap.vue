<template>
  <div :data-testid="`weekday-hour-heatmap-${mode}`">
    <svg
      :viewBox="`0 0 ${cellSize * 24} ${cellSize * 7}`"
      class="h-auto w-full"
      role="img"
      :aria-label="`${mode} heatmap`"
    >
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
    </svg>
  </div>
</template>

<script setup lang="ts">
const props = defineProps<{
  mode: "count" | "retention"
  counts: number[][]
  correct?: number[][]
}>()

const cellSize = 16

const cells: { wd: number; hr: number }[] = []
for (let wd = 0; wd < 7; wd++) {
  for (let hr = 0; hr < 24; hr++) {
    cells.push({ wd, hr })
  }
}

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
  // retention mode: counts = answered (denominator), correct = correct counts
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
</style>
