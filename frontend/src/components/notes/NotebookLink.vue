<template>
  <router-link
    :to="{ name: 'noteShow', params: { noteId: notebook.headNoteId } }"
    class="daisy-text-decoration-none"
  >
    <SvgNotebook class="me-1" />
    <span>{{ notebook.title }}</span>
  </router-link>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import type { Notebook } from "@generated/backend"
import SvgNotebook from "@/components/svgs/SvgNotebook.vue"

defineProps({
  notebook: { type: Object as PropType<Notebook>, required: true },
})
</script>
