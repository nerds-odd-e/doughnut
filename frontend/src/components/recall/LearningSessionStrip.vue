<template>
  <div
    :data-test="rowTestId"
    role="status"
    class="flex gap-2 items-start text-base font-normal text-base-content"
  >
    <span class="flex-1 break-words">{{ message }}</span>
    <button
      type="button"
      class="daisy-btn daisy-btn-primary shrink-0"
      :data-test="ctaTestId"
      @click="emit('ctaClick')"
    >
      {{ ctaLabel }}
    </button>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  rowTestId: string
  message: string
  ctaTestId: string
  ctaLabel: string
}>()

const emit = defineEmits<{
  (e: "ctaClick"): void
}>()
</script>
