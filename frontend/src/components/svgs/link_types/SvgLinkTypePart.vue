<template>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" x="0px" y="0px" viewBox="0 0 800 400">
<Object1 :stripes="false" linkRole="source" x="-250" size="30"/>
<ObjectLeft linkRole="sourceSecondary" x="-330" oheight="300" y="50"/>
<ObjectRight linkRole="source" x="-175" oheight="300" y="50"/>
<Arrow />
<ObjectRight linkRole="target" x="275"/>
</svg>
</template>

<script setup>
import ObjectLeft from "./ObjectLeft.vue"
import ObjectRight from "./ObjectRight.vue"
import Arrow from "./Arrow.vue"
</script>