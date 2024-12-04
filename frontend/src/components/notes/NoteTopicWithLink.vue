<template>
  <router-link
    :to="{ name: 'noteShow', params: { noteId: noteTopology.id } }"
    class="text-decoration-none"
  >
    <NoteTopicComponent v-if="!iconized" v-bind="{ noteTopology }" />
    <span v-else>🔗</span>
  </router-link>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import { NoteTopology } from "@/generated/backend"
import NoteTopicComponent from "./core/NoteTopicComponent.vue"

defineProps({
  noteTopology: { type: Object as PropType<NoteTopology>, required: true },
  iconized: { type: Boolean, required: false },
})
</script>
