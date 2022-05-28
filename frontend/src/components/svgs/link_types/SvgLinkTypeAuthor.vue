<template>
  <svg
    xmlns="http://www.w3.org/2000/svg"
    xmlns:xlink="http://www.w3.org/1999/xlink"
    x="0px"
    y="0px"
    viewBox="0 0 800 400"
  >
    <Object1 link-role="source" :x="-250 * ifInverse" />
    <Lnk />
    <Person link-role="target" :x="250 * ifInverse" />
    <Feather link-role="target" :x="280 * ifInverse" :y="150" />
  </svg>
</template>

<script setup>
import Object1 from "./Object1.vue";
import Person from "./Person.vue";
import Feather from "./Feather.vue";
import Lnk from "./Lnk.vue";

const props = defineProps({ ifInverse: { type: Number, default: 1 } });
</script>
