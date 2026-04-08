<template>
  <div
    data-testid="book-reading-reading-control-panel"
    class="daisy-pointer-events-none daisy-absolute daisy-bottom-0 daisy-left-0 daisy-right-0 daisy-z-20 daisy-px-2 daisy-pb-[max(0.5rem,env(safe-area-inset-bottom))] daisy-pt-2"
  >
    <div
      class="daisy-pointer-events-auto daisy-mx-auto daisy-max-w-3xl daisy-rounded-lg daisy-bg-base-200/95 daisy-border daisy-border-base-300 daisy-shadow-lg daisy-px-3 daisy-py-2 daisy-flex daisy-flex-wrap daisy-items-center daisy-gap-2"
    >
      <p class="daisy-text-sm daisy-min-w-0 daisy-flex-1 daisy-m-0">
        <span class="daisy-font-medium">{{ selectedRangeTitle }}</span>
      </p>
      <button
        type="button"
        data-testid="book-reading-mark-as-read"
        class="daisy-btn daisy-btn-primary daisy-btn-sm daisy-shrink-0"
        @click="emit('markAsRead')"
      >
        Mark as read
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  selectedRangeTitle: string
}>()

const emit = defineEmits<{
  markAsRead: []
}>()
</script>
