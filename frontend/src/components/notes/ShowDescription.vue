<template>
  <pre :class="`note-description`" style="white-space: pre-wrap">{{
    description
  }}</pre>
</template>

<script setup>
const props = defineProps({ description: String });
</script>
