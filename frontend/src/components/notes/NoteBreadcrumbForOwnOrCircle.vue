<template>
<NoteBreadcrumb :ancestors="ancestors">
  <template v-slot:topLink>
  </template>
  <template v-slot:additional>
    <slot />
  </template>
</NoteBreadcrumb>
</template>

<script setup>
  import NoteBreadcrumb from "./NoteBreadcrumb.vue"
  const props = defineProps({ancestors: Array, notebook: Object})
</script>
