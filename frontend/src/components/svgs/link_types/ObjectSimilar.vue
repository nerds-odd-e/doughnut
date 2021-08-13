<template>
<svg viewBox="0 0 100 110" :x="x" :y="y || -30" height="400">
      <ellipse stroke-width="10" :ry="36" :rx="26" id="svg_1" cy="50" cx="50" stroke="#000" transform="rotate(15)" :fill="fill" :stroke-dasharray="strokeDashArray"/>
</svg>
</template>

<script setup>
import { computed } from "@vue/runtime-core"
import colors from "./colors"

  const props = defineProps({ linkRole: String, x: Number, y: Number, abstract: Boolean})
  const fill = computed(()=> colors(props.linkRole))
  const strokeDashArray = computed(()=> props.abstract ? '20, 12' : null)
</script>