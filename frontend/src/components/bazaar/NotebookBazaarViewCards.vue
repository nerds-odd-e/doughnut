<template>
  <NotebookCardsWithButtons v-bind="{ notebooks }">
    <template #default="{ notebook }">
      <BazaarNotebookButtons v-bind="{ notebook, loggedIn }" />
    </template>
  </NotebookCardsWithButtons>
</template>

<script setup lang="ts">
import { BazaarNotebook } from "@/generated/backend"
import { PropType } from "vue"

const props = defineProps({
  bazaarNotebooks: {
    type: Array as PropType<BazaarNotebook[]>,
    required: true,
  },
  loggedIn: Boolean,
})

const notebooks = props.bazaarNotebooks.map(
  (bazaarNotebook) => bazaarNotebook.notebook,
)
</script>
