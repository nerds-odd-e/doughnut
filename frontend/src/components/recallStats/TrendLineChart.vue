<template>
  <svg :width="width" :height="height" role="img">
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

// When `yMax` is unset, derive it from the data (min 1 so an all-zero/flat series still spans the plot).
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
</script>

<style scoped>
.rs-trend-insufficient {
  fill: color-mix(in oklab, var(--color-base-content) 40%, transparent);
}
</style>
