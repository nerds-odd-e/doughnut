<template>
  <ul
    role="list"
    class="daisy-m-0 daisy-list-none daisy-p-0 daisy-flex daisy-flex-col"
  >
    <SearchResultListItem
      v-for="searchHit in searchHits"
      :key="relationshipLiteralSearchHitKey(searchHit)"
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
      <template
        #notebookButton
        v-if="
          $slots.notebookButton &&
          searchHit.hitKind === 'NOTEBOOK' &&
          searchHit.notebookId != null
        "
      >
        <slot
          name="notebookButton"
          :notebook-id="searchHit.notebookId"
          :notebook-name="searchHit.notebookName"
        />
      </template>
    </SearchResultListItem>
  </ul>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import type { RelationshipLiteralSearchHit } from "@generated/doughnut-backend-api"
import SearchResultListItem from "./SearchResultListItem.vue"
import { relationshipLiteralSearchHitKey } from "@/models/relationshipLiteralSearchHitKey"

defineProps({
  searchHits: {
    type: Array as PropType<RelationshipLiteralSearchHit[]>,
    required: true,
  },
  notebookId: { type: Number, default: undefined },
})
</script>
