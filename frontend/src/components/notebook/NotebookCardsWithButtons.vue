<template>
  <div class="daisy-grid daisy-grid-cols-1 sm:daisy-grid-cols-2 md:daisy-grid-cols-2 lg:daisy-grid-cols-3 xl:daisy-grid-cols-4 daisy-gap-4">
    <div
      v-for="notebook in notebooks"
      :key="notebook.id"
      role="card"
      class="daisy-card"
      :class="{ 'subscribed-notebook': isSubscribed }"
      data-cy="notebook-card"
    >
      <NotebookCard :notebook="notebook">
        <template #cardHeader>
          <span class="daisy-flex daisy-justify-end daisy-p-0">
            <slot :notebook="notebook" />
          </span>
        </template>
      </NotebookCard>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { Notebook } from "@generated/backend"
import NotebookCard from "../notebooks/NotebookCard.vue"

defineProps<{
  notebooks: Notebook[]
  isSubscribed?: boolean
}>()
</script>

<style scoped>
.daisy-card {
  height: 200px;
}

.daisy-card.subscribed-notebook .notebook-card {
  background: linear-gradient(to right, oklch(var(--p) / 0.2) 0%, oklch(var(--p) / 0.1) 5%);
  border: 1px solid oklch(var(--p) / 0.4);
}

.daisy-card.subscribed-notebook .notebook-binding {
  background: oklch(var(--p));
  border-right: 1px solid oklch(var(--p) / 0.7);
}
</style>
