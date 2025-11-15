<template>
  <router-link
    :to="{ name: 'noteShow', params: { noteId: noteTopology.id } }"
    class="daisy-text-decoration-none"
  >
    <NoteTitleComponent v-if="!iconized" v-bind="{ noteTopology }" />
    <span v-else>🔗</span>
  </router-link>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import type { NoteTopology } from "@generated/backend"
import NoteTitleComponent from "./core/NoteTitleComponent.vue"

defineProps({
  noteTopology: { type: Object as PropType<NoteTopology>, required: true },
  iconized: { type: Boolean, required: false },
})
</script>
