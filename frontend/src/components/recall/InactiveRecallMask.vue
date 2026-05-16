<template>
  <div
    v-if="show"
    data-test="inactive-recall-mask"
    class="absolute inset-0 z-40 flex items-center justify-center bg-base-100/90 rounded-lg"
  >
    <div class="text-center p-6">
      <p class="text-lg font-medium mb-2">Inactive</p>
      <p class="text-base-200">Focus to activate</p>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  show: boolean
}>()
</script>
