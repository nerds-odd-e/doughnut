<template>
  <svg :viewBox="`0 0 ${width} ${height}`" class="h-auto w-full" role="img">
    <g v-if="title">
      <text class="rs-trend-axis" :x="padLeft" :y="14" font-weight="600">
        {{ title }}
      </text>
    </g>
    <!-- Y-axis: gridlines + tick labels -->
    <g>
      <line
        v-for="(t, i) in yTicks"
        :key="`ygrid-${i}`"
        class="rs-trend-grid"
        :x1="padLeft"
        :x2="width - 10"
        :y1="t.y"
        :y2="t.y"
      />
      <text
        v-for="(t, i) in yTicks"
        :key="`ylabel-${i}`"
        class="rs-trend-axis"
        :x="padLeft - 4"
        :y="t.y + 3"
        text-anchor="end"
      >
        {{ t.label }}
      </text>
      <line
        class="rs-trend-axis-line"
        :x1="padLeft"
        :x2="padLeft"
        :y1="padTop"
        :y2="height - padBottom"
      />
    </g>
    <!-- X-axis: date tick labels -->
    <g v-if="xLabels">
      <line
        class="rs-trend-axis-line"
        :x1="padLeft"
        :x2="width - 10"
        :y1="height - padBottom"
        :y2="height - padBottom"
      />
      <text
        v-for="(t, i) in xTicks"
        :key="`xlabel-${i}`"
        class="rs-trend-axis"
        :x="t.x"
        :y="height - padBottom + 12"
        text-anchor="middle"
      >
        {{ t.label }}
      </text>
    </g>
    <polyline
      :data-testid="polylineTestid"
      :points="polylinePoints"
      fill="none"
      :style="{ stroke: stroke }"
      stroke-width="2"
    />
    <circle
      v-for="(pt, idx) in insufficientPoints"
      :key="`ins-${idx}`"
      :data-testid="insufficientTestid"
      class="rs-trend-insufficient"
      :cx="pt.x"
      :cy="pt.y"
      r="2"
    />
  </svg>
</template>

<script setup lang="ts">
import { computed } from "vue"

const props = defineProps<{
  values: (number | null)[]
  polylineTestid: string
  insufficientTestid: string
  stroke: string
  yMax?: number
  title?: string
  xLabels?: string[]
  formatY?: (v: number) => string
}>()

const width = 600
const height = 200
const padLeft = 40
const padBottom = 20
const padTop = 10

const plotWidth = width - padLeft - 10
const plotHeight = height - padBottom - padTop

const xFor = (i: number, total: number) =>
  padLeft + (total <= 1 ? 0 : (i / (total - 1)) * plotWidth)

const effectiveYMax = computed(() => {
  if (props.yMax != null) {
    return props.yMax
  }
  let max = 0
  for (const v of props.values) {
    if (v != null && v > max) max = v
  }
  return max === 0 ? 1 : max
})

const yFor = (v: number) =>
  height - padBottom - (v / effectiveYMax.value) * plotHeight

const sufficient = computed(() =>
  props.values.map((v) => v).filter((v) => v != null)
)

const polylinePoints = computed(() => {
  const total = sufficient.value.length
  return sufficient.value
    .map((v, idx) => `${xFor(idx, total)},${yFor(v ?? 0)}`)
    .join(" ")
})

const insufficientPoints = computed(() => {
  const points: { x: number; y: number }[] = []
  const total = props.values.length
  props.values.forEach((v, i) => {
    if (v == null) {
      points.push({ x: xFor(i, total), y: height - padBottom })
    }
  })
  return points
})

const yTicks = computed(() => {
  const fmt = props.formatY ?? ((v: number) => String(Math.round(v)))
  const max = effectiveYMax.value
  const ticks = [0, max / 2, max]
  const seen = new Set<number>()
  return ticks
    .filter((t) => {
      const key = Math.round(t * 1000)
      if (seen.has(key)) return false
      seen.add(key)
      return true
    })
    .map((t) => ({ y: yFor(t), label: fmt(t) }))
})

const formatX = (iso?: string): string => {
  if (!iso) return ""
  const [m = 1, d = 1] = iso.slice(5).split("-").map(Number)
  return `${m}/${d}`
}

const xTicks = computed(() => {
  const labels = props.xLabels ?? []
  const total = props.values.length
  if (total === 0) return []
  const indices = [0, Math.floor((total - 1) / 2), total - 1]
  const seen = new Set<number>()
  return indices
    .filter((i) => {
      if (i < 0 || i >= labels.length) return false
      if (seen.has(i)) return false
      seen.add(i)
      return true
    })
    .map((i) => ({ x: xFor(i, total), label: formatX(labels[i]) }))
})
</script>

<style scoped>
.rs-trend-insufficient {
  fill: color-mix(in oklab, var(--color-base-content) 40%, transparent);
}
.rs-trend-grid {
  stroke: var(--color-base-content);
  stroke-opacity: 0.12;
  stroke-width: 1;
}
.rs-trend-axis-line {
  stroke: var(--color-base-content);
  stroke-opacity: 0.4;
  stroke-width: 1;
}
.rs-trend-axis {
  fill: var(--color-base-content);
  font-size: 11px;
  opacity: 0.7;
}
</style>
