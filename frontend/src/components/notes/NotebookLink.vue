<template>
  <router-link
    :to="{ name: 'notebookPage', params: { notebookId: notebook.id } }"
    class="daisy-text-decoration-none"
  >
    <BookText class="daisy-me-1 daisy-w-4 daisy-h-4 daisy-inline-block" />
    <span>{{ notebook.name }}</span>
  </router-link>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import type { Notebook } from "@generated/doughnut-backend-api"
import { BookText } from "lucide-vue-next"

defineProps({
  notebook: { type: Object as PropType<Notebook>, required: true },
})
</script>
