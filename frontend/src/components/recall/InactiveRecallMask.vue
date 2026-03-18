<template>
  <div
    v-if="show"
    data-test="inactive-recall-mask"
    class="daisy-absolute daisy-inset-0 daisy-z-40 daisy-flex daisy-items-center daisy-justify-center daisy-bg-base-100/90 daisy-rounded-lg"
  >
    <div class="daisy-text-center daisy-p-6">
      <p class="daisy-text-lg daisy-font-medium daisy-mb-2">Inactive</p>
      <p class="daisy-text-base-200">Focus to activate</p>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  show: boolean
}>()
</script>
