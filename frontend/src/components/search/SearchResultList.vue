<template>
  <ul
    role="list"
    class="search-result-list daisy-m-0 daisy-list-none daisy-p-0 daisy-flex daisy-flex-col"
  >
    <SearchResultListItem
      v-for="searchHit in searchHits"
      :key="searchHitRowKey(searchHit)"
      v-bind="{ searchHit, notebookId }"
    >
      <template
        #button
        v-if="$slots.button && searchHit.hitKind === 'NOTE' && searchHit.noteSearchResult"
      >
        <slot name="button" :search-result="searchHit.noteSearchResult" />
      </template>
      <template
        #folderButton
        v-if="
          $slots.folderButton &&
          searchHit.hitKind === 'FOLDER' &&
          searchHit.folderId != null
        "
      >
        <slot
          name="folderButton"
          :folder-id="searchHit.folderId"
          :folder-name="searchHit.folderName"
          :notebook-id="searchHit.notebookId"
        />
      </template>
    </SearchResultListItem>
  </ul>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import type { RelationshipLiteralSearchHit } from "@generated/doughnut-backend-api"
import SearchResultListItem from "./SearchResultListItem.vue"
import { searchHitRowKey } from "./searchHitRowKey"

defineProps({
  searchHits: {
    type: Array as PropType<RelationshipLiteralSearchHit[]>,
    required: true,
  },
  notebookId: { type: Number, default: undefined },
})
</script>
