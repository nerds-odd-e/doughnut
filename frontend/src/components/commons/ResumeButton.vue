<template>
  <div class="paused" @click="$emit('resume')">
    <a title="Go back to review">
      <svg :width="svgSize" :height="svgSize" :viewBox="viewBox">
        <circle
          :cx="circleCenter"
          :cy="circleCenter"
          :r="circleRadius"
          stroke="grey"
          :stroke-width="strokeWidth"
          fill="rgba(255, 128, 128, 0.5)"
        />
        <circle
          :cx="circleCenter"
          :cy="circleCenter"
          :r="circleRadius"
          stroke="blue"
          :stroke-width="strokeWidth"
          fill="transparent"
          :stroke-dasharray="`${calculateStrokeDashArray} ${circleCircumference}`"
          stroke-dashoffset="0"
        />
        <text
          :x="circleCenter"
          :y="circleCenter"
          text-anchor="middle"
          dominant-baseline="middle"
          font-size="9"
        >
          Resume
        </text>
      </svg>
    </a>
  </div>
</template>

<script setup lang="ts">
import { computed, toRefs } from "vue"

const svgSize = 50
const viewBox = `0 0 ${svgSize} ${svgSize}`
const circleCenter = svgSize / 2
const circleRadius = 20
const strokeWidth = 4
const circleCircumference = 2 * Math.PI * circleRadius

const props = defineProps({
  finished: { type: Number, required: true },
  toRepeatCount: { type: Number, required: true },
})

const reactiveProps = toRefs(props)

defineEmits(["resume"])

const calculateStrokeDashArray = computed(() => {
  const progress =
    circleCircumference *
    (reactiveProps.finished.value /
      (reactiveProps.finished.value + reactiveProps.toRepeatCount.value))
  return Math.max(progress, 5)
})
</script>

<style lang="scss" scoped>
.paused {
  cursor: pointer;
  position: absolute;
  top: 10em;
  right: 10px;
  z-index: 99999;
}
</style>
