<template>
  <div class="dropdown-list">
    <template
      v-for="hit in hits"
      :key="hitKey(hit)"
    >
      <div
        v-if="hit.hitKind === 'NOTE' && hit.noteSearchResult"
        class="dropdown-hit-row daisy-flex daisy-items-start daisy-gap-2 daisy-py-1 daisy-px-2"
      >
        <div
          class="dropdown-hit-icon-col daisy-flex daisy-w-5 daisy-shrink-0 daisy-items-start daisy-justify-center daisy-mt-0.5"
        >
          <FileText :size="14" class="dropdown-hit-kind-icon" aria-hidden="true" />
        </div>
        <div class="daisy-min-w-0 daisy-flex-1">
          <NoteTitleWithLink :note-topology="hit.noteSearchResult.noteTopology" />
        </div>
      </div>
      <div
        v-else-if="hit.hitKind === 'FOLDER'"
        class="dropdown-hit-row folder-search-hit daisy-flex daisy-items-start daisy-gap-2 daisy-py-1 daisy-px-2"
      >
        <div
          class="dropdown-hit-icon-col daisy-flex daisy-w-5 daisy-shrink-0 daisy-items-start daisy-justify-center daisy-mt-0.5"
        >
          <Folder :size="14" class="dropdown-hit-kind-icon" aria-hidden="true" />
        </div>
        <div class="daisy-min-w-0 daisy-flex-1">
          <span class="daisy-font-medium">{{ hit.folderName }}</span>
          <span
            v-if="hit.notebookName"
            class="daisy-block daisy-text-xs daisy-opacity-70"
          >{{ hit.notebookName }}</span>
        </div>
      </div>
      <div
        v-else-if="hit.hitKind === 'NOTEBOOK' && hit.notebookId != null"
        class="dropdown-hit-row daisy-flex daisy-items-start daisy-gap-2 daisy-py-1 daisy-px-2"
      >
        <div
          class="dropdown-hit-icon-col daisy-flex daisy-w-5 daisy-shrink-0 daisy-items-start daisy-justify-center daisy-mt-0.5"
        >
          <BookText :size="14" class="dropdown-hit-kind-icon" aria-hidden="true" />
        </div>
        <router-link
          :to="{ name: 'notebookPage', params: { notebookId: hit.notebookId } }"
          class="daisy-min-w-0 daisy-flex-1 daisy-text-decoration-none daisy-font-medium"
        >{{ hit.notebookName }}</router-link>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import type { RelationshipLiteralSearchHit } from "@generated/doughnut-backend-api"
import { BookText, FileText, Folder } from "lucide-vue-next"
import NoteTitleWithLink from "../notes/NoteTitleWithLink.vue"
import { relationshipLiteralSearchHitKey } from "@/models/relationshipLiteralSearchHitKey"

defineProps<{
  hits: RelationshipLiteralSearchHit[]
}>()

function hitKey(hit: RelationshipLiteralSearchHit): string {
  return relationshipLiteralSearchHitKey(hit)
}
</script>

<style scoped>
.dropdown-list {
  max-height: 200px;
  overflow-y: auto;
  padding: 0.5rem;
}

.dropdown-hit-icon-col {
  line-height: 0;
}

.dropdown-hit-kind-icon {
  opacity: 0.5;
  flex-shrink: 0;
}

.dropdown-list :deep(a) {
  display: block;
  color: inherit;
}

.dropdown-hit-row :deep(a) {
  padding: 0;
}

.dropdown-list :deep(a:hover) {
  background-color: #f8f9fa;
}

.dropdown-hit-row:hover {
  background-color: #f8f9fa;
}
</style>
