<template>
  <li
    class="flex flex-col gap-2"
    :data-testid="`resolve-match-row-${matched.id}`"
  >
    <NoteTitleWithLink :note-topology="matched" />
    <div
      v-if="matchRealmRef"
      :data-testid="`resolve-match-path-${matched.id}`"
    >
      <BreadcrumbWithCircle
        :ancestor-folders="matchRealmRef.ancestorFolders ?? []"
        :notebook-realm="matchRealmRef.notebookRealm"
      />
    </div>
    <div v-if="canMutate" class="flex flex-wrap gap-2">
      <button
        type="button"
        class="daisy-btn daisy-btn-secondary daisy-btn-sm"
        :data-testid="`wiki-link-or-relationship-to-matched-note-${matched.id}`"
        :title="addWikiLinkOrRelationshipLabel"
        :aria-label="addWikiLinkOrRelationshipLabel"
        @click="$emit('addWikiLinkOrRelationship')"
      >
        {{ addWikiLinkOrRelationshipLabel }}
      </button>
      <button
        type="button"
        class="daisy-btn daisy-btn-secondary daisy-btn-sm"
        :data-testid="`add-as-overlapped-note-${matched.id}`"
        title="Add as overlapped note"
        aria-label="Add as overlapped note"
        :disabled="addAsOverlappedDisabled"
        @click="$emit('addAsOverlapped')"
      >
        Add as overlapped note
      </button>
    </div>
  </li>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import type { NoteTopology } from "@generated/donut-backend-api"
import NoteTitleWithLink from "@/components/notes/NoteTitleWithLink.vue"
import BreadcrumbWithCircle from "@/components/toolbars/BreadcrumbWithCircle.vue"
import { useStorageAccessor } from "@/composables/useStorageAccessor"

const addWikiLinkOrRelationshipLabel = "Add wiki link or relationship"

const props = defineProps({
  matched: {
    type: Object as PropType<NoteTopology>,
    required: true,
  },
  canMutate: {
    type: Boolean,
    default: false,
  },
  addAsOverlappedDisabled: {
    type: Boolean,
    default: false,
  },
})

defineEmits<{
  (e: "addWikiLinkOrRelationship"): void
  (e: "addAsOverlapped"): void
}>()

const storageAccessor = useStorageAccessor()
const matchRealmRef = storageAccessor.value
  .storedApi()
  .getNoteRealmRefAndLoadWhenNeeded(props.matched.id)
</script>
