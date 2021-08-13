<template>
<svg viewBox="0 0 100 100" :x="x" :y="y || 50" height="250">
  <rect height="90" width="90" y="5" x="5" stroke-width="10" stroke="#000" :fill="fill"/>
</svg>
</template>

<script setup>
import { computed } from "@vue/runtime-core"

  const props = defineProps({ linkRole: String, x: Number, y: Number})
  const fill = computed(()=> props.linkRole === 'source' ? '#9d3838' : '#3f7f00')
</script>