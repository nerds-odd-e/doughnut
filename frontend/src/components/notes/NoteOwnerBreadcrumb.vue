<template>
<NoteBreadcrumbForOwnOrCircle :ancestors="ancestors" :ownership="ownership">
    <slot />
</NoteBreadcrumbForOwnOrCircle>
</template>

<script setup>
  import NoteBreadcrumbForOwnOrCircle from "./NoteBreadcrumbForOwnOrCircle.vue"
  const props = defineProps({ancestors: Array, ownership: Object})
</script>
