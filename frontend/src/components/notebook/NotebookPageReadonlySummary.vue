<template>
  <div class="daisy-container daisy-mx-auto daisy-py-4 daisy-max-w-6xl">
    <div class="notebook-page-summary daisy-mb-6" data-testid="notebook-page-summary">
      <h1 class="daisy-text-xl daisy-font-semibold daisy-text-base-content">
        {{ notebook.name }}
      </h1>
      <p v-if="notebook.description" class="notebook-page-summary-description">
        {{ notebook.description }}
      </p>
    </div>
    <NotebookAttachedBookSection :notebook-id="notebook.id" />
  </div>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import type { Notebook } from "@generated/doughnut-backend-api"
import NotebookAttachedBookSection from "@/components/notebook/NotebookAttachedBookSection.vue"

defineProps({
  notebook: { type: Object as PropType<Notebook>, required: true },
})
</script>

<style scoped>
.notebook-page-summary {
  background: oklch(var(--b2) / 0.8);
  border-radius: 8px;
  margin-bottom: 1.5rem;
  padding: 1.5rem;
}

.notebook-page-summary-description {
  color: oklch(var(--bc) / 0.6);
  line-height: 1.6;
  margin-top: 0.5rem;
}
</style>
