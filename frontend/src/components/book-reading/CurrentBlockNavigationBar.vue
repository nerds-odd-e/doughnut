<template>
  <div
    data-testid="current-block-navigation-bar"
    class="pointer-events-none absolute left-0 right-0 bottom-0 z-20 px-2 pb-[max(0.5rem,env(safe-area-inset-bottom))] pt-2"
  >
    <div
      class="pointer-events-auto mx-auto max-w-3xl rounded-lg bg-base-100/95 border border-base-300 shadow-lg px-3 py-2 flex flex-wrap items-center gap-2"
    >
      <p class="text-sm min-w-0 flex-1 basis-full sm:basis-auto m-0 text-base-content/70">
        Now reading: <span class="font-medium text-base-content">{{ currentBlockTitle }}</span>
      </p>
      <div class="flex flex-wrap items-center gap-2 shrink-0">
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
