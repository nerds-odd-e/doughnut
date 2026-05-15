<template>
  <div :class="{ 'dropdown-style': isDropdown }">
    <div
      v-if="panelVisible"
      :class="displayState.containerClass"
    >
      <div
        v-if="showTitleBar"
        class="result-section-info-row daisy-flex daisy-flex-nowrap daisy-items-center daisy-gap-2 daisy-w-full daisy-min-w-0"
      >
        <button
          v-if="embedSemanticToggle"
          type="button"
          title="Semantic search"
          aria-label="Semantic search"
          data-testid="note-new-form-semantic-search-toggle"
          :class="[
            'daisy-btn daisy-btn-ghost daisy-btn-sm daisy-btn-square daisy-shrink-0',
            semanticSearchEnabled
              ? 'daisy-text-primary'
              : 'daisy-opacity-30',
          ]"
          @click="semanticSearchEnabled = !semanticSearchEnabled"
        >
          <Sparkles class="daisy-w-6 daisy-h-6" />
        </button>
        <span
          v-if="displayState.title"
          class="result-section-info daisy-shrink-0 daisy-text-sm daisy-font-normal daisy-text-base-content/70"
        >
          {{ displayState.title }}
        </span>
        <span
          v-if="isSearchInProgress"
          class="searching-indicator searching-indicator--title-inline daisy-inline-flex daisy-shrink-0 daisy-items-center"
          role="status"
          aria-busy="true"
        >
          <span
            class="daisy-loading daisy-loading-spinner daisy-loading-xs"
          />
        </span>
      </div>
      <div
        v-else-if="isSearchInProgress"
        class="searching-indicator searching-indicator--fallback"
        role="status"
        aria-busy="true"
      >
        <span
          class="daisy-loading daisy-loading-spinner daisy-loading-xs"
        />
      </div>

      <template v-if="displayState.showRecentNotes">
        <SearchDropdownHitList
          v-if="isDropdown"
          :hits="recentNotesAsHits"
        />
        <SearchResultList
          v-else
          class="search-result"
          :search-hits="recentNotesAsHits"
          :notebook-id="notebookId"
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

      <template v-else-if="displayState.showSearchResults && searchResult">
        <SearchDropdownHitList
          v-if="isDropdown"
          :hits="searchResult"
        />
        <SearchResultList
          v-else
          class="search-result"
          :search-hits="searchResult"
          :notebook-id="notebookId"
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
    </div>

    <div
      v-else-if="isSearchInProgress"
      class="searching-indicator searching-indicator--fallback"
      role="status"
      aria-busy="true"
    >
      <span
        class="daisy-loading daisy-loading-spinner daisy-loading-xs"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import type { NoteSearchResult } from "@generated/doughnut-backend-api"
import { Sparkles } from "lucide-vue-next"
import { computed, toRef } from "vue"
import SearchDropdownHitList from "./SearchDropdownHitList.vue"
import SearchResultList from "./SearchResultList.vue"
import { useSearchExecution } from "@/composables/useSearchExecution"

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

const displayState = computed(() =>
  model.getDisplayState({
    trimmedSearchKey: trimmedSearchKey.value,
    isGlobal: isGlobalSearch.value,
    noteId: props.noteId,
    isDropdown: props.isDropdown,
    filteredRecentNotesCount: filteredRecentNotes.value.length,
  })
)

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

const showTitleBar = computed(() => {
  if (hasTitleText.value) return true
  if (props.embedSemanticToggle && blindLoading.value) return true
  return false
})
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

.result-section-info-row {
  padding: 0.5rem 0.5rem 0;
  margin-bottom: 0.5rem;
}

.result-section-info {
  padding: 0;
  line-height: 1.25;
}

.searching-indicator--title-inline {
  margin-left: 0.125rem;
  line-height: 1;
}

.searching-indicator--title-inline .daisy-loading {
  width: 0.75rem;
  height: 0.75rem;
}

.searching-indicator--fallback {
  display: flex;
  justify-content: center;
  padding: 0.25rem 0;
}
</style>
