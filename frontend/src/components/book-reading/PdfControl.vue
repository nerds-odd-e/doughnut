<template>
  <div class="daisy-join daisy-join-horizontal items-center">
    <button
      type="button"
      class="daisy-btn daisy-btn-sm daisy-btn-ghost daisy-join-item"
      aria-label="Zoom out"
      data-testid="pdf-zoom-out"
      @click="emit('zoomOut')"
    >
      <ZoomOut class="w-4 h-4" />
    </button>
    <span
      v-if="currentPage != null && pagesTotal != null && pagesTotal > 0"
      data-testid="book-reading-page-indicator"
      class="shrink-0 text-sm text-base-content/70 tabular-nums px-1"
      :aria-label="`Page ${currentPage} of ${pagesTotal}`"
    >
      {{ currentPage }} / {{ pagesTotal }}
    </span>
    <button
      type="button"
      class="daisy-btn daisy-btn-sm daisy-btn-ghost daisy-join-item"
      aria-label="Zoom in"
      data-testid="pdf-zoom-in"
      @click="emit('zoomIn')"
    >
      <ZoomIn class="w-4 h-4" />
    </button>
  </div>
</template>

<script setup lang="ts">
import { ZoomIn, ZoomOut } from "@lucide/vue"

defineProps<{
  currentPage: number | null
  pagesTotal: number | null
}>()

const emit = defineEmits<{
  zoomIn: []
  zoomOut: []
}>()
</script>
