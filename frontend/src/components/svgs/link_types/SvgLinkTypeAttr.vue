<template>
  <svg
    xmlns="http://www.w3.org/2000/svg"
    xmlns:xlink="http://www.w3.org/1999/xlink"
    x="0px"
    y="0px"
    viewBox="0 0 800 400"
  >
    <Object1 :stripes="false" linkRole="target" :x="50 * ifInverse" :size="35" />
    <Object1
      :stripes="false"
      linkRole="source"
      :x="-120 * ifInverse"
      :size="55"
    />
  </svg>
</template>

<script setup>
import Object1 from "./Object1.vue";

const props = defineProps({ ifInverse: { type: Number, default: 1 } });
</script>
