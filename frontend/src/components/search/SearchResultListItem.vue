<template>
  <li
    role="listitem"
    :data-testid="searchResultItemTestId"
    class="flex flex-row items-start gap-2 border-b border-base-300 py-2 px-1 last:border-b-0 hover:bg-base-200 transition-colors"
    :class="{
      'different-notebook-border border-l-primary': isDifferentNotebook,
    }"
  >
    <div
      class="search-hit-kind-icon flex w-5 shrink-0 items-start justify-center pt-0.5 text-base-content/50"
      aria-hidden="true"
    >
      <component :is="kindIcon" :size="14" class="block" />
    </div>
    <div class="min-w-0 flex-1">
      <div class="search-result-item-title">
        <NoteTitleWithLink
          v-if="searchHit.hitKind === 'NOTE' && searchHit.noteSearchResult"
          :note-topology="searchHit.noteSearchResult.noteTopology"
        />
        <router-link
          v-else-if="
            searchHit.hitKind === 'FOLDER' &&
            searchHit.folderId != null &&
            searchHit.notebookId != null
          "
          :to="{
            name: 'folderPage',
            params: {
              notebookId: searchHit.notebookId,
              folderId: searchHit.folderId,
            },
          }"
          class="folder-hit-title no-underline"
        >{{ searchHit.folderName }}</router-link>
        <span
          v-else-if="searchHit.hitKind === 'FOLDER'"
          class="folder-hit-title"
        >{{ searchHit.folderName }}</span>
        <router-link
          v-else-if="searchHit.hitKind === 'NOTEBOOK' && searchHit.notebookId != null"
          :to="{ name: 'notebookPage', params: { notebookId: searchHit.notebookId } }"
          class="notebook-hit-title no-underline"
        >{{ searchHit.notebookName }}</router-link>
      </div>
      <div
        v-if="displayNotebookName || displayDistance != null"
        class="search-hit-meta flex flex-row flex-wrap items-baseline gap-1"
      >
        <span v-if="displayNotebookName" class="notebook-name-label">{{
          displayNotebookName
        }}</span>
        <span v-if="displayDistance != null">{{ formattedDistance }}</span>
      </div>
    </div>
    <div
      class="ms-1 flex shrink-0 flex-col items-end gap-1 self-center"
    >
      <div
        v-if="$slots.button"
        class="flex flex-row flex-wrap justify-end gap-1"
      >
        <slot name="button" />
      </div>
      <div
        v-if="$slots.folderButton"
        class="flex flex-row flex-wrap justify-end gap-1"
      >
        <slot name="folderButton" />
      </div>
      <div
        v-if="$slots.notebookButton"
        class="flex flex-row flex-wrap justify-end gap-1"
      >
        <slot name="notebookButton" />
      </div>
    </div>
  </li>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import type { RelationshipLiteralSearchHit } from "@generated/doughnut-backend-api"
import { BookText, FileText, Folder } from "@lucide/vue"
import { computed, type Component } from "vue"
import NoteTitleWithLink from "../notes/NoteTitleWithLink.vue"
import { searchResultItemTestId } from "@/utils/searchDialogKeyboard"

const props = defineProps({
  searchHit: {
    type: Object as PropType<RelationshipLiteralSearchHit>,
    required: true,
  },
  notebookId: { type: Number, default: undefined },
})

const kindIcon = computed((): Component => {
  if (props.searchHit.hitKind === "NOTEBOOK") return BookText
  if (props.searchHit.hitKind === "FOLDER") return Folder
  return FileText
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
  if (
    props.searchHit.hitKind === "FOLDER" ||
    props.searchHit.hitKind === "NOTEBOOK"
  ) {
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

.folder-hit-title,
.notebook-hit-title {
  font-weight: 600;
}

.search-hit-kind-icon {
  line-height: 0;
}
</style>
