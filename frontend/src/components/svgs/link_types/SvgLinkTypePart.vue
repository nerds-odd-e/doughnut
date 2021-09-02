<template>
  <svg x="0px" y="0px" viewBox="0 0 800 400">
    <ObjectLeft
      linkRole="source"
      :x="ifInverse === 1 ? -330 : 175"
      oheight="300"
      y="50"
    />
    <ObjectRight
      linkRole="source"
      :x="ifInverse === 1 ? -175 : 330"
      oheight="300"
      y="50"
    />
    <Lnk />
    <ObjectRight linkRole="target" :x="ifInverse === 1 ? 275 : -200" />
  </svg>
</template>

<script setup>
import ObjectLeft from "./ObjectLeft.vue";
import ObjectRight from "./ObjectRight.vue";
import Lnk from "./Lnk.vue";
const props = defineProps({ ifInverse: { type: Number, default: 1 } });
</script>
