<template>
  <svg
    xmlns="http://www.w3.org/2000/svg"
    xmlns:xlink="http://www.w3.org/1999/xlink"
    x="0px"
    y="0px"
    viewBox="0 0 800 400"
  >
    <ObjectLeft :link-role="ifInverse === 1 ? 'source' : 'target'" :x="-270" />
    <Lnk />
    <ObjectLeft :link-role="ifInverse === 1 ? 'target' : 'source'" :x="220" />
  </svg>
</template>

<script setup>
defineProps({ ifInverse: { type: Number, default: 1 } })
</script>
