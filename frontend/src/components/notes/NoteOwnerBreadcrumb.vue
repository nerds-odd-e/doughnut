<template>
  <NoteBreadcrumbForOwnOrCircle
    :ancestors="ancestors"
    :circle="!!notebook ? notebook.ownership.circle : nil"
  >
    <slot />
  </NoteBreadcrumbForOwnOrCircle>
</template>

<script setup>
import NoteBreadcrumbForOwnOrCircle from "./NoteBreadcrumbForOwnOrCircle.vue";
const props = defineProps({ ancestors: Array, notebook: Object });
</script>
