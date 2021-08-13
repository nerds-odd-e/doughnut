<template>
<svg 
xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" x="0px" y="0px" viewBox="0 0 800 400">
<Tag linkRole="source" x="-200" y="0" :oheight="400"/>
<Arrow />
<Object1 :stripes="false" linkRole="target" x="250"/>
</svg>
</template>

<script setup>
import Tag from "./Tag.vue"
import Object1 from "./Object1.vue"
import Arrow from "./Arrow.vue"
</script>