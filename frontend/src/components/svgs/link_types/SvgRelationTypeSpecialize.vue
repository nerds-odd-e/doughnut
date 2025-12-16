<template>
  <svg
    xmlns="http://www.w3.org/2000/svg"
    xmlns:xlink="http://www.w3.org/1999/xlink"
    x="0px"
    y="0px"
    viewBox="0 0 800 400"
  >
    <Object1 :stripes="false" link-role="source" :x="-250 * ifInverse" />
    <Lnk />
    <Object1 :stripes="true" link-role="target" :x="250 * ifInverse" />
  </svg>
</template>

<script setup lang="ts">
defineProps({ ifInverse: { type: Number, default: 1 } })
</script>
