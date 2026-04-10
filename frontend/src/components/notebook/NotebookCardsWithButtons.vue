<template>
  <div
    v-if="layout === 'list'"
    class="notebook-cards-list daisy-flex daisy-flex-col daisy-gap-2"
  >
    <NotebookListRow
      v-for="notebook in notebooks"
      :key="notebook.id"
      :notebook="notebook"
    >
      <slot :notebook="notebook" />
    </NotebookListRow>
  </div>
  <div
    v-else
    class="daisy-grid daisy-grid-cols-1 sm:daisy-grid-cols-2 md:daisy-grid-cols-2 lg:daisy-grid-cols-3 xl:daisy-grid-cols-4 daisy-gap-4"
  >
    <div
      v-for="notebook in notebooks"
      :key="notebook.id"
      role="card"
      class="daisy-card"
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
import type { Notebook } from "@generated/doughnut-backend-api"
import NotebookCard from "../notebooks/NotebookCard.vue"
import NotebookListRow from "./NotebookListRow.vue"

withDefaults(
  defineProps<{
    notebooks: Notebook[]
    layout?: "grid" | "list"
  }>(),
  { layout: "grid" }
)
</script>

<style scoped>
.daisy-card {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.daisy-card :deep(.notebook-card) {
  flex: 1;
  display: flex;
  flex-direction: column;
  margin-bottom: 0;
}

.daisy-card :deep(.notebook-card > a) {
  flex: 1;
  display: flex;
  flex-direction: column;
}
</style>
