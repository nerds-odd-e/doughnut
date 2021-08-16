<template>
<svg viewBox="-30 -30 630 530" :x="x" :y="y || 30" height="300" :fill="fill" stroke="#000" stroke-width="50">
<path d="M332.64,64.58C313.18,43.57,286,32,256,32c-30.16,0-57.43,11.5-76.8,32.38-19.58,21.11-29.12,49.8-26.88,80.78C156.76,206.28,203.27,256,256,256s99.16-49.71,103.67-110.82C361.94,114.48,352.34,85.85,332.64,64.58Z"/>
<path d="M432,480H80A31,31,0,0,1,55.8,468.87c-6.5-7.77-9.12-18.38-7.18-29.11C57.06,392.94,83.4,353.61,124.8,326c36.78-24.51,83.37-38,131.2-38s94.42,13.5,131.2,38c41.4,27.6,67.74,66.93,76.18,113.75,1.94,10.73-.68,21.34-7.18,29.11A31,31,0,0,1,432,480Z"/>
</svg>

</template>

<script setup>
import { computed } from "@vue/runtime-core"
import colors from "./colors"

  const props = defineProps({ linkRole: String, x: Number, y: Number})
  const fill = computed(()=> colors(props.linkRole))
  const strokeDashArray = computed(()=> props.abstract ? '20, 12' : null)
</script>