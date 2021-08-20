<template>
  <pre :class="`note-description`" style="white-space: pre-wrap;">{{description}}</pre>
</template>

<script setup>
  const props = defineProps({description: String})
</script>

<style scoped>
.note-body {
  padding-left: 10px;
  padding-right: 10px;
}
</style>
