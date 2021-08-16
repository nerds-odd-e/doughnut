<template>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" x="0px" y="0px" viewBox="0 0 800 400">
<Object1 :stripes="false" linkRole="source" x="-250"/>
<Arrow />
<ObjectApplication linkRole="target" x="250" y="0"/>
</svg>
</template>

<script setup>
import Object1 from "./Object1.vue"
import ObjectApplication from "./ObjectApplication.vue"
import Arrow from "./Arrow.vue"
</script>