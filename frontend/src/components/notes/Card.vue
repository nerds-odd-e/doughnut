<template>
  <div
    role="card"
    class="daisy-card daisy-bg-base-100 daisy-shadow-xl hover:daisy-shadow-2xl hover:daisy-bg-base-300 daisy-transition-all"
    :class="{ 'different-notebook-border': isDifferentNotebook }"
  >
    <slot name="cardHeader" />
      <div class="daisy-card-body daisy-p-4">
    <router-link
      :to="{ name: 'noteShow', params: { noteId: noteTopology.id } }"
      class="daisy-no-underline"
    >
        <h5 class="daisy-card-title">
          <NoteTitleWithLink v-bind="{ noteTopology }" />
        </h5>
        <p v-if="noteTopology.shortDetails" class="daisy-text-base">
          {{ noteTopology.shortDetails }}
        </p>
    </router-link>
    <div class="daisy-card-actions daisy-justify-end" v-if="$slots.button">
      <slot name="button" :note-title="noteTopology" />
    </div>
      </div>
  </div>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import type { NoteTopology } from "@generated/backend"
import NoteTitleWithLink from "./NoteTitleWithLink.vue"
import { computed } from "vue"

const props = defineProps({
  noteTopology: {
    type: Object as PropType<NoteTopology & { notebookId?: number }>,
    required: true,
  },
  notebookId: { type: Number, default: undefined },
})

const isDifferentNotebook = computed(() => {
  if (props.notebookId === undefined || !props.noteTopology.notebookId) {
    return false
  }
  return props.noteTopology.notebookId !== props.notebookId
})
</script>

<style scoped>
.different-notebook-border {
  border: 2px solid hsl(var(--p) / 0.5);
}
</style>
