<template>
  <div class="row">
    <div
      class="col-12 col-sm-6 col-md-6 col-lg-4 col-xl-3"
      v-for="notebook in notebooks"
      :key="notebook.id"
    >
      <NotebookCard :notebook="notebook">
        <template #cardHeader>
          <span class="card-header d-flex flex-row-reverse p-0">
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
