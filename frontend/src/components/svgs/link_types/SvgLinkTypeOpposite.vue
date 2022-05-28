<template>
  <svg
    xmlns="http://www.w3.org/2000/svg"
    xmlns:xlink="http://www.w3.org/1999/xlink"
    x="0px"
    y="0px"
    viewBox="0 0 800 400"
  >
    <ObjectLeft :linkRole="ifInverse === 1 ? `source` : `target`" :x="-275" />
    <Lnk />
    <ObjectRight :linkRole="ifInverse === 1 ? `target` : `source`" :x="275" />
  </svg>
</template>

<script setup>
import ObjectLeft from "./ObjectLeft.vue";
import ObjectRight from "./ObjectRight.vue";
import Lnk from "./Lnk.vue";

const props = defineProps({ ifInverse: { type: Number, default: 1 } });
</script>
