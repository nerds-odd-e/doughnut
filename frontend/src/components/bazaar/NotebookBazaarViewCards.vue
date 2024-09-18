<template>
  <NotebookCardsWithButtons v-bind="{ notebooks }">
    <template #default="{ notebook }">
      <BazaarNotebookButtons v-bind="{ notebook, loggedIn }" />
    </template>
  </NotebookCardsWithButtons>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import type { BazaarNotebook } from "@/generated/backend"
import NotebookCardsWithButtons from "../notebook/NotebookCardsWithButtons.vue"
import BazaarNotebookButtons from "./BazaarNotebookButtons.vue"

const props = defineProps({
  bazaarNotebooks: {
    type: Array as PropType<BazaarNotebook[]>,
    required: true,
  },
  loggedIn: Boolean,
})

const notebooks = props.bazaarNotebooks.map(
  (bazaarNotebook) => bazaarNotebook.notebook
)
</script>
