<template>
  <div class="dropdown-list">
    <template
      v-for="hit in hits"
      :key="hitKey(hit)"
    >
      <div
        v-if="hit.hitKind === 'NOTE' && hit.noteSearchResult"
        class="dropdown-hit-row flex items-start gap-2 py-1 px-2"
      >
        <div
          class="dropdown-hit-icon-col flex w-5 shrink-0 items-start justify-center mt-0.5"
        >
          <FileText :size="14" class="dropdown-hit-kind-icon" aria-hidden="true" />
        </div>
        <div class="min-w-0 flex-1">
          <NoteTitleWithLink :note-topology="hit.noteSearchResult.noteTopology" />
        </div>
      </div>
      <div
        v-else-if="hit.hitKind === 'FOLDER'"
        class="dropdown-hit-row folder-search-hit flex items-start gap-2 py-1 px-2"
      >
        <div
          class="dropdown-hit-icon-col flex w-5 shrink-0 items-start justify-center mt-0.5"
        >
          <Folder :size="14" class="dropdown-hit-kind-icon" aria-hidden="true" />
        </div>
        <div class="min-w-0 flex-1">
          <router-link
            v-if="hit.folderId != null && hit.notebookId != null"
            :to="{
              name: 'folderPage',
              params: {
                notebookId: hit.notebookId,
                folderId: hit.folderId,
              },
            }"
            class="block font-medium no-underline"
          >{{ hit.folderName }}</router-link>
          <span v-else class="font-medium">{{ hit.folderName }}</span>
          <span
            v-if="hit.notebookName"
            class="block text-xs opacity-70"
          >{{ hit.notebookName }}</span>
        </div>
      </div>
      <div
        v-else-if="hit.hitKind === 'NOTEBOOK' && hit.notebookId != null"
        class="dropdown-hit-row flex items-start gap-2 py-1 px-2"
      >
        <div
          class="dropdown-hit-icon-col flex w-5 shrink-0 items-start justify-center mt-0.5"
        >
          <BookText :size="14" class="dropdown-hit-kind-icon" aria-hidden="true" />
        </div>
        <router-link
          :to="{ name: 'notebookPage', params: { notebookId: hit.notebookId } }"
          class="min-w-0 flex-1 no-underline font-medium"
        >{{ hit.notebookName }}</router-link>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import type { RelationshipLiteralSearchHit } from "@generated/doughnut-backend-api"
import { BookText, FileText, Folder } from "@lucide/vue"
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
