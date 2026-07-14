<template>
  <div :class="{ 'dropdown-style': isDropdown }">
    <div v-if="panelVisible" :class="displayState.containerClass">
      <SearchResultsPanelHeader
        v-if="showTitleBar"
        v-model:semantic-search-enabled="semanticSearchEnabled"
        v-model:list-preference="listPreference"
        :embed-semantic-toggle="embedSemanticToggle"
        :show-list-mode-toggle="showListModeToggle"
        :active-list-mode="activeListMode"
        :title="displayState.title"
        :is-search-in-progress="isSearchInProgress"
      />
      <div
        v-else-if="isSearchInProgress"
        class="searching-indicator searching-indicator--fallback"
        role="status"
        aria-busy="true"
      >
        <span class="daisy-loading daisy-loading-spinner daisy-loading-xs" />
      </div>

      <template v-if="visibleHits">
        <SearchDropdownHitList v-if="isDropdown" :hits="visibleHits" />
        <SearchResultList
          v-else
          class="search-result"
          :search-hits="visibleHits"
          :notebook-id="notebookId"
          @keydown="onListKeydown"
        >
          <template #button="{ searchResult: result }">
            <slot name="button" :note-search-result="result" />
          </template>
          <template
            #folderButton="{
              folderId,
              folderName,
              notebookId: folderNotebookId,
            }"
          >
            <slot
              name="folderButton"
              :folder-id="folderId"
              :folder-name="folderName"
              :notebook-id="folderNotebookId"
            />
          </template>
          <template
            #notebookButton="{
              notebookId: hitNotebookId,
              notebookName,
            }"
          >
            <slot
              name="notebookButton"
              :notebook-id="hitNotebookId"
              :notebook-name="notebookName"
            />
          </template>
        </SearchResultList>
      </template>

      <template v-else-if="displayState.showEmptyState">
        <em>{{ displayState.emptyMessage }}</em>
      </template>
    </div>

    <div
      v-else-if="isSearchInProgress"
      class="searching-indicator searching-indicator--fallback"
      role="status"
      aria-busy="true"
    >
      <span class="daisy-loading daisy-loading-spinner daisy-loading-xs" />
    </div>
  </div>
</template>

<script setup lang="ts">
import type {
  NoteSearchResult,
  RelationshipLiteralSearchHit,
} from "@generated/doughnut-backend-api"
import { computed, toRef } from "vue"
import SearchDropdownHitList from "./SearchDropdownHitList.vue"
import SearchResultsPanelHeader from "./SearchResultsPanelHeader.vue"
import SearchResultList from "./SearchResultList.vue"
import { useSearchExecution } from "@/composables/useSearchExecution"
import { useSearchListPreference } from "@/composables/useSearchListPreference"
import { useStableModalTop } from "@/composables/modalTopAnchor"

useStableModalTop()

const props = defineProps({
  noteId: Number,
  inputSearchKey: { type: String, required: true },
  isDropdown: { type: Boolean, default: false },
  notebookId: { type: Number, default: undefined },
  /**
   * When true, renders the semantic search toggle before the section title (e.g. new note form).
   * Use with v-model:semantic-search-enabled.
   */
  embedSemanticToggle: { type: Boolean, default: false },
})

const semanticSearchEnabled = defineModel<boolean>("semanticSearchEnabled", {
  default: true,
})

const allMyNotebooksAndSubscriptions = defineModel<boolean>(
  "allMyNotebooksAndSubscriptions",
  { default: true }
)
const allMyCircles = defineModel<boolean>("allMyCircles", { default: false })

const emit = defineEmits<{
  keydown: [event: KeyboardEvent]
}>()

function onListKeydown(event: KeyboardEvent) {
  emit("keydown", event)
}

defineSlots<{
  button: (props: { noteSearchResult: NoteSearchResult }) => void
  folderButton: (props: {
    folderId: number
    folderName?: string
    notebookId?: number
  }) => void
  notebookButton: (props: { notebookId: number; notebookName?: string }) => void
}>()

const inputSearchKeyRef = toRef(props, "inputSearchKey")
const noteIdRef = toRef(props, "noteId")
const notebookIdRef = toRef(props, "notebookId")
const semanticSearchEnabledRef = semanticSearchEnabled

const {
  model,
  isSearchInProgress,
  searchResult,
  filteredRecentNotes,
  recentNotesAsHits,
} = useSearchExecution({
  inputSearchKey: inputSearchKeyRef,
  noteId: noteIdRef,
  notebookId: notebookIdRef,
  semanticSearchEnabled: semanticSearchEnabledRef,
  allMyNotebooksAndSubscriptions,
  allMyCircles,
})

const trimmedSearchKey = computed(() => props.inputSearchKey.trim())
const isGlobalSearch = computed(
  () => allMyNotebooksAndSubscriptions.value === true
)

const showListModeToggle = computed(() => !props.embedSemanticToggle)
const { listPreference, effectiveListPreference } = useSearchListPreference({
  enabled: showListModeToggle,
  trimmedSearchKey,
})

const displayState = computed(() =>
  model.getDisplayState({
    trimmedSearchKey: trimmedSearchKey.value,
    isGlobal: isGlobalSearch.value,
    noteId: props.noteId,
    isDropdown: props.isDropdown,
    filteredRecentNotesCount: filteredRecentNotes.value.length,
    listPreference: effectiveListPreference.value,
  })
)

const activeListMode = computed<"matches" | "recent">(() =>
  displayState.value.showRecentNotes ? "recent" : "matches"
)

const visibleHits = computed((): RelationshipLiteralSearchHit[] | null => {
  if (displayState.value.showRecentNotes) return recentNotesAsHits.value
  if (displayState.value.showSearchResults && searchResult.value) {
    return searchResult.value
  }
  return null
})

const hasVisibleResultsSection = computed(
  () =>
    displayState.value.showRecentNotes ||
    displayState.value.showEmptyState ||
    displayState.value.showSearchResults
)

const blindLoading = computed(
  () => isSearchInProgress.value && !hasVisibleResultsSection.value
)

const panelVisible = computed(
  () =>
    hasVisibleResultsSection.value ||
    (props.embedSemanticToggle && blindLoading.value)
)

const hasTitleText = computed(
  () => !!displayState.value.title && hasVisibleResultsSection.value
)

const showTitleBar = computed(
  () => hasTitleText.value || (props.embedSemanticToggle && blindLoading.value)
)
</script>

<style scoped>
.search-result {
  max-height: 300px;
  overflow-y: auto;
}

.dropdown-style {
  position: absolute;
  width: 100%;
  background: white;
  border: 1px solid #ddd;
  border-radius: 0 0 4px 4px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  z-index: 1000;
}

.result-section {
  margin-top: 1rem;
}

.searching-indicator--fallback {
  display: flex;
  justify-content: center;
  padding: 0.25rem 0;
}
</style>
