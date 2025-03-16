<template>
  <div class="daisy:grid daisy:grid-cols-1 daisy:sm:grid-cols-2 daisy:md:grid-cols-2 daisy:lg:grid-cols-3 daisy:xl:grid-cols-4 daisy:gap-4">
    <div
      v-for="notebook in notebooks"
      :key="notebook.id"
    >
      <NotebookCard :notebook="notebook">
        <template #cardHeader>
          <span class="daisy:flex daisy:justify-end daisy:p-0">
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
