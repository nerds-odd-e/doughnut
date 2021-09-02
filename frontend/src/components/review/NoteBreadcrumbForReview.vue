<template>
  <NoteBreadcrumb :ancestors="ancestors">
    <template v-slot:additional> </template>
  </NoteBreadcrumb>
</template>

<script setup>
import NoteBreadcrumb from "../notes/NoteBreadcrumb.vue";
const props = defineProps({ ancestors: Array });
</script>
