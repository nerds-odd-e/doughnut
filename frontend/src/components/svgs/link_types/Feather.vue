<template>
<svg viewBox="0 0 489 489" :x="x" :y="y || 30" height="250" :fill="fill" >
<path d="M454.42,1.682c-13.582-13.573-176.909,57.175-253.594,133.866c-68.213,68.213-103.595,146.031-87.341,185.697
	c-38.402,52.85-65.758,104.646-80.619,167.724c41.236-55.449,61.96-108.331,250.62-315.439
	c-12.301,16.585-69.783,83.155-119.021,172.665c42.354-3.692,102.21-37.042,156.088-90.912
	C397.245,178.591,467.992,15.263,454.42,1.682z"/>
</svg>

</template>

<script setup>
import { computed } from "@vue/runtime-core"
import colors from "./colors"

  const props = defineProps({ linkRole: String, x: Number, y: Number})
  const fill = computed(()=> colors(props.linkRole))
  const strokeDashArray = computed(()=> props.abstract ? '20, 12' : null)
</script>