<template>
  <NotebookCardsWithButtons v-bind="{ notebooks }">
    <template #default="{ notebook }">
      <NotebookButtons v-bind="{ notebook }" class="card-header-btn" />
      <slot class="card-header-btn" />
    </template>
  </NotebookCardsWithButtons>
</template>

<script setup lang="ts">
import { PropType } from "vue"
import { Notebook } from "@/generated/backend"
import NotebookCardsWithButtons from "./NotebookCardsWithButtons.vue"
import NotebookButtons from "./NotebookButtons.vue"

defineProps({
  notebooks: {
    type: Array as PropType<Notebook[]>,
    required: true,
  },
})
</script>
