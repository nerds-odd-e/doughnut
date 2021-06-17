<template>
<NoteBreadcrumbForOwnOrCircle :ancestors="ancestors" :notebook="notebook">
    <slot />
</NoteBreadcrumbForOwnOrCircle>
</template>

<script setup>
  import NoteBreadcrumbForOwnOrCircle from "./NoteBreadcrumbForOwnOrCircle.vue"
  const props = defineProps({ancestors: Array, notebook: Object})
</script>
