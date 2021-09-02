<template>
  <svg
    xmlns="http://www.w3.org/2000/svg"
    xmlns:xlink="http://www.w3.org/1999/xlink"
    x="0px"
    y="0px"
    viewBox="0 0 800 400"
  >
    <Object1
      :stripes="false"
      linkRole="source"
      :x="ifInverse === 1 ? -70 : 300"
      y="105"
      size="30"
    />
    <Object1 :stripes="false" linkRole="target" :x="250 * ifInverse" />

    <svg viewBox="0 0 200 100" x="-100" :y="0" :height="400">
      <path
        v-if="ifInverse === 1"
        d="
    M155,40
    c 0 0, 0 -15, -25 -20
    L 105,20 
    c -30 0, -50 0, -50 50
    m -25 -25
    l 25 25
    l 25 -25
    "
        fill="none"
      />

      <path
        v-else
        d="
    M90,30
    c 0 0, 0 -10, 5 -20
    l 50,0 
    c 10 0, 10 0, 10 50
    m -25 -25
    l 25 25
    l 25 -25
    "
        fill="none"
      />
    </svg>
  </svg>
</template>

<script setup>
import Object1 from "./Object1.vue";
const props = defineProps({ ifInverse: { type: Number, default: 1 } });
</script>
