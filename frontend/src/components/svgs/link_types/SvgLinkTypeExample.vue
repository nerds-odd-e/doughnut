<template>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" x="0px" y="0px" viewBox="0 0 800 400">
<Object1 :stripes="false" linkRole="source" x="-250"/>
<Arrow />
<Object1 :stripes="false" linkRole="target" x="250" y=35 size="48"/>
<svg width="400" height="400" x=330 y=-50 viewBox="0 0 32 32" stroke-width="1%">
        <path d="M25,19V15a2,2,0,0,0-2-2H19a2,2,0,0,0-2,2v6a2,2,0,0,0,2,2h5V21H19V19Zm-6-4h4v2H19Z"/>
</svg>

<svg width="400" height="400" x=440 y=-50 viewBox="0 0 32 32" stroke-width="1%">
        <path d="M19,13a2,2,0,0,0-2,2v6a2,2,0,0,0,2,2h4v2H18v2h5a2,2,0,0,0,2-2V13Zm4,8H19V15h4Z"/>
</svg>
</svg>
</template>

<script setup>
import Tag from "./Tag.vue"
import Object1 from "./Object1.vue"
import Arrow from "./Arrow.vue"
</script>