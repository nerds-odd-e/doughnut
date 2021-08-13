<template>
<svg th:fragment="add" width="400px" height="200px"
xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" x="0px" y="0px" viewBox="0 0 800 400">
<Object1 :stripes="false" linkRole="source" x="-250" :abstract="true"/>
<Arrow />
<Object1 :stripes="false" linkRole="target" x="250"/>
</svg>
</template>

<script setup>
import Object1 from "./Object1.vue"
import Object2 from "./Object2.vue"
import Arrow from "./Arrow.vue"
</script>