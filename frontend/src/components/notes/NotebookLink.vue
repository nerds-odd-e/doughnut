<template>
  <router-link
    :to="{ name: 'noteShow', params: { noteId: notebook.headNoteId } }"
    class="daisy-text-decoration-none"
  >
    <BookText class="me-1 w-4 h-4 inline-block" />
    <span>{{ notebook.title }}</span>
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
