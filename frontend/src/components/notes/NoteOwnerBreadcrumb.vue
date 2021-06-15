<template>
<NoteBreadcrumbForOwnOrCircle :ancestors="untilNote.ancestors" :ownership="untilNote.ownership">
    <slot />
</NoteBreadcrumbForOwnOrCircle>
</template>

<script setup>
  import NoteBreadcrumbForOwnOrCircle from "./NoteBreadcrumbForOwnOrCircle.vue"
  const props = defineProps({untilNote: Object})
</script>
