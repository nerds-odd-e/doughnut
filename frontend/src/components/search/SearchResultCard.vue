<template>
  <div
    role="card"
    class="daisy-card daisy-bg-base-100 daisy-shadow-xl hover:daisy-shadow-2xl hover:daisy-bg-base-300 daisy-transition-all"
    :class="{ 'different-notebook-border daisy-border-primary': isDifferentNotebook }"
  >
    <div class="daisy-card-body daisy-p-4">
      <h5 class="daisy-card-title">
        <NoteTitleWithLink
          v-if="searchHit.hitKind === 'NOTE' && searchHit.noteSearchResult"
          :note-topology="searchHit.noteSearchResult.noteTopology"
        />
        <span
          v-else-if="searchHit.hitKind === 'FOLDER'"
          class="folder-hit-card-title"
        >{{ searchHit.folderName }}</span>
      </h5>
      <div v-if="displayNotebookName" class="notebook-name-label">
        {{ displayNotebookName }}
      </div>
      <small
        v-if="displayDistance != null"
        class="similarity-distance daisy-mt-1 daisy-block daisy-text-right"
      >
        {{ Number(displayDistance).toFixed(3) }}
      </small>
      <div class="daisy-card-actions daisy-justify-end" v-if="$slots.button">
        <slot name="button" />
      </div>
      <div
        class="daisy-card-actions daisy-justify-end"
        v-if="$slots.folderButton"
      >
        <slot name="folderButton" />
      </div>
    </div>
  </div>
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
</script>

<style scoped>
.different-notebook-border {
  border-width: 1px !important;
  border-style: solid !important;
}

.notebook-name-label {
  font-size: 0.65rem;
  opacity: 0.7;
  margin-top: 0.125rem;
}

.folder-hit-card-title {
  font-weight: 600;
}

.similarity-distance {
  font-size: 0.75rem;
  opacity: 0.75;
}
</style>
