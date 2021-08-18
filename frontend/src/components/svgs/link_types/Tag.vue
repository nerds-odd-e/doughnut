<template>
<svg viewBox="0 0 400 400" :x="x" :y="y" :height="!!oheight ? oheight : 250">
    <path d="m169.45719,4.51163l-90.72957,0l-74.22161,74.22161l0,90.72957l224.04881,224.04881l164.95118,-164.96544l-224.04881,-224.03454zm-110.83304,86.07823c-8.83183,-8.83183 -8.83183,-23.14253 0,-31.96009c8.83183,-8.83183 23.14253,-8.83183 31.97436,0c8.8461,8.8461 8.83183,23.12826 0,31.97436c-8.83183,8.83183 -23.14253,8.83183 -31.97436,-0.01427z"
     :fill="fill"/>
</svg>
</template>

<script setup>
import { computed } from "@vue/runtime-core"
import bgcolors from "../../../colors"

  const props = defineProps({ linkRole: String, x: Number, y: Number, oheight: Number})
  const fill = computed(()=> bgcolors(props.linkRole))
</script>