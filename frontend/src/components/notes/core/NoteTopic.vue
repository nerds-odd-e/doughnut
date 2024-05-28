<template>
  {{ topic }}
</template>

<script setup lang="ts">
defineProps({
  topic: { type: String, required: true },
});
</script>
