<template>
  <NotebookCardsWithButtons v-bind="{ notebooks }">
    <template #default="{ notebook }">
      <NotebookButtons v-bind="{ notebook, user }" class="card-header-btn" />
      <slot class="card-header-btn" />
    </template>
  </NotebookCardsWithButtons>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import type { Notebook, User } from "@/generated/backend"
import NotebookCardsWithButtons from "./NotebookCardsWithButtons.vue"
import NotebookButtons from "./NotebookButtons.vue"

defineProps({
  notebooks: {
    type: Array as PropType<Notebook[]>,
    required: true,
  },
  user: {
    type: Object as PropType<User>,
    required: false,
  },
})
</script>
