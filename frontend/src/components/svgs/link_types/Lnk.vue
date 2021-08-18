<template>
<svg viewBox="0 0 100 100" :x="x" :y="100" height="200" >
  <ellipse :ry="15" :rx="20" cy="50" cx="25" fill="none"/>
  <ellipse :ry="15" :rx="20" cy="50" cx="75" fill="none"/>
  <path d="
     M30 50 l40 0"/>
</svg>

</template>

<script setup>
  const props = defineProps({ x: String})
</script>