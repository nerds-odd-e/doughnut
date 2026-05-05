<template>
  <li
    role="listitem"
    class="daisy-flex daisy-flex-row daisy-items-start daisy-gap-3 daisy-border-b daisy-border-base-300 daisy-py-2 daisy-px-1 last:daisy-border-b-0 hover:daisy-bg-base-200 daisy-transition-colors"
    :class="{
      'different-notebook-border daisy-border-l-primary': isDifferentNotebook,
    }"
  >
    <div class="daisy-min-w-0 daisy-flex-1">
      <div class="search-result-item-title">
        <NoteTitleWithLink
          v-if="searchHit.hitKind === 'NOTE' && searchHit.noteSearchResult"
          :note-topology="searchHit.noteSearchResult.noteTopology"
        />
        <span
          v-else-if="searchHit.hitKind === 'FOLDER'"
          class="folder-hit-title"
        >{{ searchHit.folderName }}</span>
      </div>
      <div
        v-if="displayNotebookName || displayDistance != null"
        class="search-hit-meta daisy-flex daisy-flex-row daisy-flex-wrap daisy-items-baseline daisy-gap-1"
      >
        <span v-if="displayNotebookName" class="notebook-name-label">{{
          displayNotebookName
        }}</span>
        <span v-if="displayDistance != null">{{ formattedDistance }}</span>
      </div>
    </div>
    <div
      class="daisy-flex daisy-shrink-0 daisy-flex-col daisy-items-end daisy-gap-1 daisy-self-center"
    >
      <div
        v-if="$slots.button"
        class="daisy-flex daisy-flex-row daisy-flex-wrap daisy-justify-end daisy-gap-1"
      >
        <slot name="button" />
      </div>
      <div
        v-if="$slots.folderButton"
        class="daisy-flex daisy-flex-row daisy-flex-wrap daisy-justify-end daisy-gap-1"
      >
        <slot name="folderButton" />
      </div>
    </div>
  </li>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import type { RelationshipLiteralSearchHit } from "@generated/doughnut-backend-api"
import { computed } from "vue"
import NoteTitleWithLink from "../notes/NoteTitleWithLink.vue"

const props = defineProps({
  searchHit: {
    type: Object as PropType<RelationshipLiteralSearchHit>,
    required: true,
  },
  notebookId: { type: Number, default: undefined },
})

const displayNotebookName = computed(() => {
  if (props.searchHit.hitKind === "NOTE" && props.searchHit.noteSearchResult) {
    return props.searchHit.noteSearchResult.notebookName
  }
  if (props.searchHit.hitKind === "FOLDER") {
    return props.searchHit.notebookName
  }
  return undefined
})

const isDifferentNotebook = computed(() => {
  if (props.notebookId === undefined) {
    return false
  }
  const nb =
    props.searchHit.hitKind === "NOTE" && props.searchHit.noteSearchResult
      ? props.searchHit.noteSearchResult.notebookId
      : props.searchHit.notebookId
  return nb !== props.notebookId
})

const displayDistance = computed(() => {
  if (props.searchHit.hitKind === "NOTE" && props.searchHit.noteSearchResult) {
    return props.searchHit.noteSearchResult.distance
  }
  if (props.searchHit.hitKind === "FOLDER") {
    return props.searchHit.distance
  }
  return undefined
})

const formattedDistance = computed(() =>
  displayDistance.value != null ? Number(displayDistance.value).toFixed(3) : ""
)
</script>

<style scoped>
.different-notebook-border {
  border-left-width: 4px;
  border-left-style: solid;
}

.search-hit-meta {
  font-size: 0.65rem;
  opacity: 0.7;
  margin-top: 0.125rem;
}

.folder-hit-title {
  font-weight: 600;
}
</style>
