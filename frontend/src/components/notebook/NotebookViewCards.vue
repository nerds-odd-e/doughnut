<template>
  <NotebookCardsWithButtons v-bind="{ notebooks }">
    <template #default="{ notebook }">
      <NotebookButtons v-bind="{ notebook }" class="card-header-btn" />
      <slot class="card-header-btn" />
    </template>
  </NotebookCardsWithButtons>
</template>

<script setup lang="ts">
import { Notebook } from "@/generated/backend"
import { PropType } from "vue"

defineProps({
  notebooks: {
    type: Array as PropType<Notebook[]>,
    required: true,
  },
})
</script>
