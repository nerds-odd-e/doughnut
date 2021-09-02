<template>
  <svg viewBox="0 0 400 400" height="200" y="100" :x="x">
    <path
      :transform="flip ? `scale(-1, 1) translate(-400, 0)` : null"
      id="svg_1"
      d="m182,319l61,-74l-118,0l-118,0l0,-46l0,-46l117,0c64,0 117,-2 117,-5c0,-3 -26,-36 -57,-74l-57,-69l53,0l53,0l80,97l80,97l-80,97l-80,97l-55,0l-55,0l61,-74l0,0z"
      fill="#000000"
    />
  </svg>
</template>

<script setup>
const props = defineProps({ x: String, flip: Boolean });
</script>
