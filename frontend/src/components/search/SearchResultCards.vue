<template>
  <div class="daisy-grid daisy-gap-3" :class="{
    'daisy-grid-cols-1': true,
    'md:daisy-grid-cols-3': columns === 3,
    'lg:daisy-grid-cols-3': columns === 3,
  }">
    <div v-for="searchHit in searchHits" :key="searchHitRowKey(searchHit)">
      <SearchResultCard v-bind="{ searchHit, notebookId }">
        <template
          #button
          v-if="$slots.button && searchHit.hitKind === 'NOTE' && searchHit.noteSearchResult"
        >
          <slot name="button" :search-result="searchHit.noteSearchResult" />
        </template>
      </SearchResultCard>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import type { RelationshipLiteralSearchHit } from "@generated/doughnut-backend-api"
import SearchResultCard from "./SearchResultCard.vue"
import { searchHitRowKey } from "./searchHitRowKey"

defineProps({
  searchHits: {
    type: Array as PropType<RelationshipLiteralSearchHit[]>,
    required: true,
  },
  columns: { type: Number, default: 3 },
  notebookId: { type: Number, default: undefined },
})
</script>
