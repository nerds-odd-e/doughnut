<template>
  <svg
    xmlns="http://www.w3.org/2000/svg"
    xmlns:xlink="http://www.w3.org/1999/xlink"
    x="0px"
    y="0px"
    viewBox="0 0 800 400"
  >
    <ObjectLeft linkRole="source" :x="ifInverse === 1 ? -270 : 220" />
    <Lnk />
    <ObjectConfuse linkRole="target" :x="ifInverse === 1 ? 260 : -220" />
  </svg>
</template>

<script setup>
import ObjectLeft from "./ObjectLeft.vue";
import ObjectConfuse from "./ObjectConfuse.vue";
import Lnk from "./Lnk.vue";

const props = defineProps({ ifInverse: { type: Number, default: 1 } });
</script>
