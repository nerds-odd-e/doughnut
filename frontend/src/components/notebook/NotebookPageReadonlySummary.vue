<template>
  <div class="container mx-auto py-4 max-w-6xl">
    <div class="notebook-page-summary mb-6" data-testid="notebook-page-summary">
      <h1 class="text-xl font-semibold text-base-content">
        {{ notebook.name }}
      </h1>
      <p v-if="notebook.description" class="notebook-page-summary-description">
        {{ notebook.description }}
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import type { Notebook } from "@generated/doughnut-backend-api"

defineProps({
  notebook: { type: Object as PropType<Notebook>, required: true },
})
</script>

<style scoped>
.notebook-page-summary {
  background: color-mix(in oklch, var(--color-base-200) 80%, transparent);
  border-radius: 8px;
  margin-bottom: 1.5rem;
  padding: 1.5rem;
}

.notebook-page-summary-description {
  color: color-mix(in oklch, var(--color-base-content) 60%, transparent);
  line-height: 1.6;
  margin-top: 0.5rem;
}
</style>
