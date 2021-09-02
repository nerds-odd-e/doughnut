<template>
  <NotebookCardsWithButtons v-bind="{ notebooks }">
    <template #default="{ notebook }">
      <BazaarNotebookButtons v-bind="{ notebook, user }" />
    </template>
  </NotebookCardsWithButtons>
</template>

<script setup>
import NotebookCardsWithButtons from "../notebook/NotebookCardsWithButtons.vue";
import BazaarNotebookButtons from "./BazaarNotebookButtons.vue";

const props = defineProps({ notebooks: Array, user: Object });
</script>
