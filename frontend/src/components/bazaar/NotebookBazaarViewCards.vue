<template>
  <NotebookCardsWithButtons v-bind="{ notebooks }">
    <template #default="{ notebook }">
      <BazaarNotebookButtons v-bind="{ notebook, loggedIn }" />
    </template>
  </NotebookCardsWithButtons>
</template>

<script setup>
import NotebookCardsWithButtons from "../notebook/NotebookCardsWithButtons.vue";
import BazaarNotebookButtons from "./BazaarNotebookButtons.vue";

const props = defineProps({ notebooks: Array, loggedIn: Boolean });
</script>
