<template>
  <div>
    <div>Certificate Page {{ props.notebookId }}</div>
  </div>
</template>

<script setup lang="ts">
const props = defineProps({
  notebookId: { type: Number, required: true },
})
</script>