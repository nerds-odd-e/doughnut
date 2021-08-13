<template>
<svg viewBox="0 0 100 100" :x="x" :y="y ? y : 50" :height="!!oheight ? oheight : 250">
  <rect :width="90" :height="90" y="5" x="5" stroke-width="10" stroke="#777" :fill="fill"/>
</svg>
</template>

<script setup>
import { computed } from "@vue/runtime-core"
import colors from "./colors"

  const props = defineProps({ linkRole: String, x: Number, y: Number, oheight: Number})
  const fill = computed(()=> colors(props.linkRole))
</script>