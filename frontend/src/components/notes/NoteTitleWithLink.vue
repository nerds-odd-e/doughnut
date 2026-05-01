<template>
  <router-link
    :to="noteShowLocationFromNoteTopology(noteTopology)"
    class="daisy-text-decoration-none"
  >
    <NoteTitleComponent v-bind="{ noteTopology }" />
  </router-link>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import type { NoteTopology } from "@generated/doughnut-backend-api"
import { noteShowLocationFromNoteTopology } from "@/routes/noteShowLocation"
import NoteTitleComponent from "./core/NoteTitleComponent.vue"

defineProps({
  noteTopology: { type: Object as PropType<NoteTopology>, required: true },
})
</script>
