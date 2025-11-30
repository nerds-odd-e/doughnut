<template>
  <div class="daisy-grid daisy-gap-3" :class="{
    'daisy-grid-cols-1': true,
    'md:daisy-grid-cols-3': columns === 3,
    'lg:daisy-grid-cols-3': columns === 3,
  }">
    <div v-for="searchResult in searchResults" :key="searchResult.noteTopology.id">
      <SearchResultCard v-bind="{ searchResult: searchResult, notebookId: notebookId }">
        <template #button v-if="$slots.button">
          <slot name="button" :search-result="searchResult" />
        </template>
      </SearchResultCard>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import type { NoteSearchResult } from "@generated/backend"
import SearchResultCard from "./SearchResultCard.vue"

defineProps({
  searchResults: {
    type: Array as PropType<NoteSearchResult[]>,
    required: true,
  },
  columns: { type: Number, default: 3 },
  notebookId: { type: Number, default: undefined },
})
</script>

