<template>
  <div :class="{ 'dropdown-style': isDropdown }">
    <div
      v-if="isSearchInProgress"
      class="searching-indicator"
      role="status"
      aria-busy="true"
    >
      <span
        class="daisy-loading daisy-loading-spinner daisy-loading-sm"
      />
    </div>

    <div v-if="displayState.showRecentNotes" :class="displayState.containerClass">
      <div class="result-title">{{ displayState.title }}</div>
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
    </div>

    <div v-if="displayState.showEmptyState" :class="displayState.containerClass">
      <div v-if="displayState.title" class="result-title">{{ displayState.title }}</div>
      <em>{{ displayState.emptyMessage }}</em>
    </div>

    <div v-if="displayState.showSearchResults && searchResult" :class="displayState.containerClass">
      <div class="result-title">{{ displayState.title }}</div>
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
    </div>
  </div>
</template>

<script setup lang="ts">
import type { NoteSearchResult } from "@generated/doughnut-backend-api"
import { computed, toRef } from "vue"
import SearchDropdownHitList from "./SearchDropdownHitList.vue"
import SearchResultList from "./SearchResultList.vue"
import { useSearchExecution } from "@/composables/useSearchExecution"

const props = defineProps({
  noteId: Number,
  inputSearchKey: { type: String, required: true },
  isDropdown: { type: Boolean, default: false },
  notebookId: { type: Number, default: undefined },
  /** When false, only literal search runs (modal defaults off via SearchForNoteAndFolder). */
  semanticSearchEnabled: { type: Boolean, default: true },
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
const semanticSearchEnabledRef = toRef(props, "semanticSearchEnabled")

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
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  z-index: 1000;
}

.result-section {
  margin-top: 1rem;
}

.result-title {
  font-weight: bold;
  margin-bottom: 0.5rem;
  padding: 0.5rem;
}

.searching-indicator {
  display: flex;
  justify-content: center;
  padding: 0.25rem 0;
}
</style>
