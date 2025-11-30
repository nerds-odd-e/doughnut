<template>
  <div
    role="card"
    class="daisy-card daisy-bg-base-100 daisy-shadow-xl hover:daisy-shadow-2xl hover:daisy-bg-base-300 daisy-transition-all"
    :class="{ 'different-notebook-border': isDifferentNotebook }"
  >
    <div class="daisy-card-body daisy-p-4">
      <router-link
        :to="{ name: 'noteShow', params: { noteId: searchResult.noteTopology.id } }"
        class="daisy-no-underline"
      >
        <h5 class="daisy-card-title">
          {{ searchResult.noteTopology.titleOrPredicate }}
        </h5>
      </router-link>
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

const props = defineProps({
  searchResult: { type: Object as PropType<NoteSearchResult>, required: true },
  notebookId: { type: Number, default: undefined },
})

const isDifferentNotebook = computed(() => {
  if (props.notebookId === undefined) {
    return false
  }
  return props.searchResult.noteTopology.notebookId !== props.notebookId
})
</script>

<style scoped>
.different-notebook-border {
  border: 2px solid hsl(var(--p) / 0.5);
}
</style>

