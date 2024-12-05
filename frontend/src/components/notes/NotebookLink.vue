<template>
  <router-link
    :to="{ name: 'noteShow', params: { noteId: notebook.headNoteTopic.id } }"
    class="text-decoration-none"
  >
    <span>{{ notebook.headNoteTopic.titleOrPredicate }}</span>
  </router-link>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import type { Notebook } from "@/generated/backend"

defineProps({
  notebook: { type: Object as PropType<Notebook>, required: true },
})
</script>
