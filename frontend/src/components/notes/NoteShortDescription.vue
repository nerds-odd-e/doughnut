<template>
  <p class="note-short-description">{{ shortDescription }}</p>
</template>

<script setup>
const props = defineProps({
  shortDescription: String,
});
</script>
