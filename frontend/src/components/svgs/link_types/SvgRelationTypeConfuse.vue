<template>
  <svg
    xmlns="http://www.w3.org/2000/svg"
    xmlns:xlink="http://www.w3.org/1999/xlink"
    x="0px"
    y="0px"
    viewBox="0 0 800 400"
  >
    <ObjectLeft link-role="source" :x="ifInverse === 1 ? -270 : 220" />
    <Lnk />
    <ObjectConfuse link-role="target" :x="ifInverse === 1 ? 260 : -220" />
  </svg>
</template>

<script setup lang="ts">
defineProps({ ifInverse: { type: Number, default: 1 } })
</script>
