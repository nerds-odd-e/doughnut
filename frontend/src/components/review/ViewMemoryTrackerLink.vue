<template>
  <router-link
    :to="{
      name: 'memoryTrackerShow',
      params: { memoryTrackerId },
    }"
    class="daisy-link daisy-link-primary daisy-mt-4 daisy-inline-block"
  >
    View Memory Tracker
  </router-link>
</template>

<script setup lang="ts">
defineProps({
  memoryTrackerId: {
    type: Number,
    required: true,
  },
})
</script>

