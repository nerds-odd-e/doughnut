<template>
  <div
    role="card"
    class="daisy-card daisy-bg-base-100 daisy-shadow-xl hover:daisy-shadow-2xl hover:daisy-bg-base-300 daisy-transition-all"
    :class="{ 'different-notebook-border daisy-border-primary': isDifferentNotebook }"
  >
    <div class="daisy-card-body daisy-p-4">
      <h5 class="daisy-card-title">
        <NoteTitleWithLink
          :note-topology="searchResult.noteTopology"
        />
      </h5>
      <div class="daisy-card-actions daisy-justify-end" v-if="$slots.button">
        <slot name="button" :search-result="searchResult" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import type { NoteSearchResult } from "@generated/backend"
import { computed } from "vue"
import NoteTitleWithLink from "../notes/NoteTitleWithLink.vue"

const props = defineProps({
  searchResult: { type: Object as PropType<NoteSearchResult>, required: true },
  notebookId: { type: Number, default: undefined },
})

const isDifferentNotebook = computed(() => {
  if (props.notebookId === undefined) {
    return false
  }
  return props.searchResult.notebookId !== props.notebookId
})
</script>

<style scoped>
.different-notebook-border {
  border-width: 1px !important;
  border-style: solid !important;
}
</style>

