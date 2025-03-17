<template>
  <div class="daisy-grid daisy-grid-cols-1 sm:daisy-grid-cols-2 md:daisy-grid-cols-2 lg:daisy-grid-cols-3 xl:daisy-grid-cols-4 daisy-gap-4">
    <div
      v-for="notebook in notebooks"
      :key="notebook.id"
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
import type { Notebook } from "@/generated/backend"
import NotebookCard from "../notebooks/NotebookCard.vue"

defineProps<{
  notebooks: Notebook[]
}>()
</script>
