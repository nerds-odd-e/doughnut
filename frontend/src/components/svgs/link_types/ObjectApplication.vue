<template>
<svg viewBox="0 0 100 130" :x="x" :y="y || 50" height="400" :fill="fill">
      <ellipse :ry="40" :rx="40" id="svg_1" cy="85" cx="50"/>
      <ellipse :ry="7" :rx="7" id="svg_1" cy="85" cx="50"/>
      <path d="
      M30,15
      l 30,0
      l 15,-10
      M50,15
      l 0 70
      l -15 15
      l -10 0
      " fill="none"/>

</svg>
</template>

<script setup>
import { computed } from "@vue/runtime-core"
import colors from "./colors"

  const props = defineProps({ linkRole: String, x: Number, y: Number, oheight: Number})
  const fill = computed(()=> colors(props.linkRole))
</script>