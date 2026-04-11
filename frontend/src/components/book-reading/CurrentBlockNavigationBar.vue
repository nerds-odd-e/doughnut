<template>
  <div
    data-testid="current-block-navigation-bar"
    class="daisy-pointer-events-none daisy-absolute daisy-left-0 daisy-right-0 daisy-bottom-0 daisy-z-20 daisy-px-2 daisy-pb-[max(0.5rem,env(safe-area-inset-bottom))] daisy-pt-2"
  >
    <div
      class="daisy-pointer-events-auto daisy-mx-auto daisy-max-w-3xl daisy-rounded-lg daisy-bg-base-100/95 daisy-border daisy-border-base-300 daisy-shadow-lg daisy-px-3 daisy-py-2 daisy-flex daisy-flex-wrap daisy-items-center daisy-gap-2"
    >
      <p class="daisy-text-sm daisy-min-w-0 daisy-flex-1 daisy-basis-full sm:daisy-basis-auto daisy-m-0 daisy-text-base-content/70">
        Now reading: <span class="daisy-font-medium daisy-text-base-content">{{ currentBlockTitle }}</span>
      </p>
      <div class="daisy-flex daisy-flex-wrap daisy-items-center daisy-gap-2 daisy-shrink-0">
        <button
          type="button"
          data-testid="read-from-here"
          class="daisy-btn daisy-btn-primary daisy-btn-sm"
          @click="emit('readFromHere')"
        >
          Read from here
        </button>
        <button
          type="button"
          data-testid="back-to-selected"
          class="daisy-btn daisy-btn-outline daisy-btn-sm"
          @click="emit('backToSelected')"
        >
          Back to selected
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  currentBlockTitle: string
}>()

const emit = defineEmits<{
  readFromHere: []
  backToSelected: []
}>()
</script>
