<template>
  <div :class="{ 'dropdown-style': isDropdown }">
    <div
      v-if="model.isSearchInProgress"
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
      <div v-if="isDropdown" class="dropdown-list">
        <NoteTitleWithLink
          v-for="result in filteredRecentNotes"
          :key="result.noteTopology.id"
          :noteTopology="result.noteTopology"
        />
      </div>
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
      </SearchResultList>
    </div>

    <div v-if="displayState.showEmptyState" :class="displayState.containerClass">
      <div class="result-title" v-if="displayState.title">{{ displayState.title }}</div>
      <em>{{ displayState.emptyMessage }}</em>
    </div>

    <div v-if="displayState.showSearchResults && searchResult" :class="displayState.containerClass">
      <div class="result-title">{{ displayState.title }}</div>
      <div v-if="isDropdown" class="dropdown-list">
        <template
          v-for="hit in searchResult"
          :key="relationshipLiteralSearchHitKey(hit)"
        >
          <NoteTitleWithLink
            v-if="hit.hitKind === 'NOTE' && hit.noteSearchResult"
            :note-topology="hit.noteSearchResult.noteTopology"
          />
          <div
            v-else-if="hit.hitKind === 'FOLDER'"
            class="folder-search-hit daisy-py-1 daisy-px-2"
          >
            <span class="daisy-font-medium">{{ hit.folderName }}</span>
            <span
              v-if="hit.notebookName"
              class="daisy-block daisy-text-xs daisy-opacity-70"
            >{{ hit.notebookName }}</span>
          </div>
          <router-link
            v-else-if="hit.hitKind === 'NOTEBOOK' && hit.notebookId != null"
            :to="{ name: 'notebookPage', params: { notebookId: hit.notebookId } }"
            class="daisy-block daisy-py-1 daisy-px-2 daisy-text-decoration-none"
          >
            <span class="daisy-font-medium">{{ hit.notebookName }}</span>
          </router-link>
        </template>
      </div>
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
      </SearchResultList>
    </div>
  </div>
</template>

<script setup lang="ts">
import type {
  SearchTerm,
  NoteSearchResult,
  RelationshipLiteralSearchHit,
} from "@generated/doughnut-backend-api"
import {
  NoteController,
  SearchController,
} from "@generated/doughnut-backend-api/sdk.gen"
import {} from "@/managedApi/clientSetup"
import { debounce } from "mini-debounce"
import { ref, computed, watch, onBeforeUnmount, shallowRef } from "vue"
import SearchResultList from "./SearchResultList.vue"
import NoteTitleWithLink from "../notes/NoteTitleWithLink.vue"
import { SearchResultsModel } from "@/models/searchResultsModel"
import { relationshipLiteralSearchHitKey } from "@/models/relationshipLiteralSearchHitKey"

const props = defineProps({
  noteId: Number,
  inputSearchKey: { type: String, required: true },
  isDropdown: { type: Boolean, default: false },
  notebookId: { type: Number, default: undefined },
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
}>()

const oldSearchTerm = ref<SearchTerm>({
  searchKey: "",
  allMyNotebooksAndSubscriptions: true,
  allMyCircles: false,
})

const timeoutId = ref<ReturnType<typeof setTimeout>>()
const model = new SearchResultsModel()
/** Bumps when a new debounced search starts so late responses from an older run are ignored. */
const searchGeneration = shallowRef(0)

const trimmedSearchKey = computed(() => props.inputSearchKey.trim())
const isGlobalSearch = computed(
  () => allMyNotebooksAndSubscriptions.value === true
)

const searchResult = computed(() =>
  model.getSearchResult(trimmedSearchKey.value, isGlobalSearch.value)
)

const filteredRecentNotes = computed(() =>
  props.noteId
    ? model.recentNotes.filter((note) => note.noteTopology.id !== props.noteId)
    : model.recentNotes
)

const recentNotesAsHits = computed((): RelationshipLiteralSearchHit[] =>
  filteredRecentNotes.value.map((r) => ({
    hitKind: "NOTE" as const,
    noteSearchResult: r,
  }))
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

const SEARCH_DEBOUNCE_MS = 1000
const debounced = debounce((callback) => callback(), SEARCH_DEBOUNCE_MS)

const fetchRecentNotes = async () => {
  if (
    (isGlobalSearch.value || props.noteId) &&
    model.recentNotes.length === 0
  ) {
    const { data: notes, error } = await NoteController.getRecentNotes({})
    model.recentNotes = error ? [] : notes || []
  }
}

const search = () => {
  const originalTrimmedKey = trimmedSearchKey.value
  model.prepareForNewSearch(originalTrimmedKey, isGlobalSearch.value)

  if (
    !model.hasPreviousResult() &&
    (isGlobalSearch.value || props.noteId) &&
    model.recentNotes.length === 0
  ) {
    fetchRecentNotes()
  }

  if (originalTrimmedKey !== "") {
    model.startSearch()
  }

  timeoutId.value = debounced(async () => {
    const gen = ++searchGeneration.value
    const term: SearchTerm = {
      searchKey: props.inputSearchKey,
      allMyNotebooksAndSubscriptions: allMyNotebooksAndSubscriptions.value,
      allMyCircles: allMyCircles.value,
    }
    const snapshotTrimmed = term.searchKey.trim()
    const snapshotGlobal = term.allMyNotebooksAndSubscriptions === true
    const snapshotNotebookId = props.notebookId

    const literalPromise = props.noteId
      ? SearchController.searchForRelationshipTargetWithin({
          path: { note: props.noteId },
          body: term,
        })
      : SearchController.searchForRelationshipTarget({ body: term })
    const semanticPromise = props.noteId
      ? SearchController.semanticSearchWithin({
          path: { note: props.noteId },
          body: term,
        })
      : SearchController.semanticSearch({ body: term })

    const applyIfCurrent = () =>
      gen === searchGeneration.value &&
      snapshotTrimmed === trimmedSearchKey.value &&
      snapshotGlobal === isGlobalSearch.value

    literalPromise.then((literalRes) => {
      if (!applyIfCurrent()) return
      const literal = literalRes.error ? [] : literalRes.data || []
      model.mergeAndCacheResults({
        trimmedSearchKey: snapshotTrimmed,
        isGlobal: snapshotGlobal,
        literalResults: literal,
        currentNotebookId: snapshotNotebookId,
      })
    })

    semanticPromise.then((semanticRes) => {
      if (!applyIfCurrent()) return
      const semantic = semanticRes.error ? [] : semanticRes.data || []
      model.mergeAndCacheResults({
        trimmedSearchKey: snapshotTrimmed,
        isGlobal: snapshotGlobal,
        semanticResults: semantic,
        currentNotebookId: snapshotNotebookId,
      })
    })

    await Promise.all([literalPromise, semanticPromise])
    if (!applyIfCurrent()) return
    model.completeSearch()
  })
}

watch(
  () =>
    [
      props.inputSearchKey,
      allMyNotebooksAndSubscriptions.value,
      allMyCircles.value,
    ] as const,
  () => {
    if (
      allMyCircles.value &&
      !oldSearchTerm.value.allMyNotebooksAndSubscriptions
    ) {
      allMyNotebooksAndSubscriptions.value = true
    } else if (
      !allMyNotebooksAndSubscriptions.value &&
      oldSearchTerm.value.allMyCircles
    ) {
      allMyCircles.value = false
    }

    if (trimmedSearchKey.value !== "") {
      search()
    } else if (isGlobalSearch.value || props.noteId) {
      fetchRecentNotes()
    }
    oldSearchTerm.value = {
      searchKey: props.inputSearchKey,
      allMyNotebooksAndSubscriptions: allMyNotebooksAndSubscriptions.value,
      allMyCircles: allMyCircles.value,
    }
  },
  { immediate: true }
)

watch(
  () => props.inputSearchKey,
  () => {
    if (props.inputSearchKey.trim() === "") {
      model.clearPreviousResult()
      if (
        (props.noteId && props.isDropdown) ||
        isGlobalSearch.value ||
        props.noteId
      ) {
        model.clearRecentResult()
        fetchRecentNotes()
      }
    }
  }
)

onBeforeUnmount(() => {
  if (timeoutId.value) {
    clearTimeout(timeoutId.value)
  }
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
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  z-index: 1000;
}

.dropdown-list {
  max-height: 200px;
  overflow-y: auto;
  padding: 0.5rem;
}

.dropdown-list :deep(a) {
  display: block;
  padding: 0.25rem 0.5rem;
  color: inherit;
}

.dropdown-list :deep(a:hover) {
  background-color: #f8f9fa;
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
