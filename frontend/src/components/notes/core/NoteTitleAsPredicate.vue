<template>
  <h2 role="title">
    <NoteTitleComponent v-bind="{ noteTopology: noteTopology, full: true }" />
  </h2>
  <Breadcrumb
    v-if="noteTopology.targetNoteTopology"
    v-bind="{ noteTopology: noteTopology.targetNoteTopology }"
  />
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import type { NoteTopology } from "@generated/doughnut-backend-api"
import NoteTitleComponent from "./NoteTitleComponent.vue"
import Breadcrumb from "../../toolbars/Breadcrumb.vue"

defineProps({
  noteTopology: { type: Object as PropType<NoteTopology>, required: true },
  readonly: { type: Boolean, default: false },
})
</script>

<style scoped>
h2 {
  font-size: 1.5rem;
  font-weight: 400;
}
</style>
