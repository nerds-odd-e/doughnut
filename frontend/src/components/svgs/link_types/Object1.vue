<template>
<svg viewBox="0 0 100 100" :x="x" :y="y || 50" height="300">
      <ellipse stroke-width="10" :ry="size ? size : 45" :rx="size ? size: 45" id="svg_1" cy="50" cx="50" stroke="#000" :fill="fill" :stroke-dasharray="strokeDashArray"/>
      <template v-if="stripes">
      <line y1="40" x1="5" y2="95" x2="60" stroke-width="10" stroke="#000" fill="none"/>
      <line y1="20" x1="20" y2="80" x2="80" stroke-width="10" stroke="#000" fill="none"/>
      <line y1="5" x1="40" y2="60" x2="95" stroke-width="10" stroke="#000" fill="none"/>
      </template>
</svg>
</template>

<script setup>
import { computed } from "@vue/runtime-core"
import colors from "./colors"

  const props = defineProps({ stripes: Boolean, linkRole: String, x: Number, y: Number, abstract: Boolean, size: Number})
  const fill = computed(()=> colors(props.linkRole))
  const strokeDashArray = computed(()=> props.abstract ? '20, 12' : null)
</script>