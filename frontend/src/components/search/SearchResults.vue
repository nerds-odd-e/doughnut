<template>
  <div :class="{ 'dropdown-style': isDropdown }">
    <div v-if="!isDropdown">
      <CheckInput
        scope-name="searchTerm"
        field="allMyNotebooksAndSubscriptions"
        v-model="searchTerm.allMyNotebooksAndSubscriptions"
        :disabled="!noteId"
      />
      <CheckInput
        scope-name="searchTerm"
        field="allMyCircles"
        v-model="searchTerm.allMyCircles"
      />
    </div>

    <div v-if="model.isSearchInProgress">
      <em class="searching-indicator">Searching ...</em>
    </div>

    <div v-if="displayState.showRecentNotes" :class="displayState.containerClass">
      <div class="result-title">{{ displayState.title }}</div>
      <div v-if="isDropdown" class="dropdown-list">
        <NoteTitleWithLink
          v-for="note in filteredRecentNotes"
          :key="note.id"
          :noteTopology="note.note.noteTopology"
        />
      </div>
      <Cards
        v-else
        class="search-result"
        :noteTopologies="filteredRecentNotes.map((n) => n.note.noteTopology)"
        :columns="3"
      >
        <template #button="{ noteTopology }">
          <slot name="button" :note-topology="noteTopology" />
        </template>
      </Cards>
    </div>

    <div v-if="displayState.showEmptyState" :class="displayState.containerClass">
      <div class="result-title" v-if="displayState.title">{{ displayState.title }}</div>
      <em>{{ displayState.emptyMessage }}</em>
    </div>

    <div v-if="displayState.showSearchResults && searchResult" :class="displayState.containerClass">
      <div class="result-title">{{ displayState.title }}</div>
      <div v-if="isDropdown" class="dropdown-list">
        <NoteTitleWithLink
          v-for="noteTopology in searchResult"
          :key="noteTopology.id"
          :noteTopology="noteTopology"
        />
      </div>
      <Cards
        v-else
        class="search-result"
        :noteTopologies="searchResult"
        :columns="3"
      >
        <template #button="{ noteTopology }">
          <slot name="button" :note-topology="noteTopology" />
          <small
            v-if="distanceById[String(noteTopology.id)] != null"
            class="similarity-distance"
          >
            {{ Number(distanceById[String(noteTopology.id)]).toFixed(3) }}
          </small>
        </template>
      </Cards>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { SearchTerm, NoteTopology } from "@generated/backend"
import { NoteController, SearchController } from "@generated/backend/sdk.gen"
import {} from "@/managedApi/clientSetup"
import { debounce } from "mini-debounce"
import { ref, computed, watch, onMounted, onBeforeUnmount } from "vue"
import CheckInput from "../form/CheckInput.vue"
import Cards from "../notes/Cards.vue"
import NoteTitleWithLink from "../notes/NoteTitleWithLink.vue"
import { SearchResultsModel } from "@/models/searchResultsModel"

const props = defineProps({
  noteId: Number,
  inputSearchKey: { type: String, required: true },
  isDropdown: { type: Boolean, default: false },
})

defineSlots<{
  button: (props: { noteTopology: NoteTopology }) => void
}>()

const searchTerm = ref<SearchTerm>({
  searchKey: "",
  allMyNotebooksAndSubscriptions: false,
  allMyCircles: false,
})

const oldSearchTerm = ref<SearchTerm>({
  searchKey: "",
  allMyNotebooksAndSubscriptions: false,
  allMyCircles: false,
})

const timeoutId = ref<ReturnType<typeof setTimeout>>()
const model = new SearchResultsModel()

const trimmedSearchKey = computed(() => searchTerm.value.searchKey.trim())
const isGlobalSearch = computed(
  () => searchTerm.value.allMyNotebooksAndSubscriptions === true
)

const searchResult = computed(() =>
  model.getSearchResult(trimmedSearchKey.value, isGlobalSearch.value)
)

const distanceById = computed(() =>
  model.getDistanceById(trimmedSearchKey.value, isGlobalSearch.value)
)

const filteredRecentNotes = computed(() => {
  if (props.noteId) {
    return model.recentNotes.filter((note) => note.id !== props.noteId)
  }
  return model.recentNotes
})

const displayState = computed(() =>
  model.getDisplayState(
    trimmedSearchKey.value,
    isGlobalSearch.value,
    props.noteId,
    props.isDropdown,
    filteredRecentNotes.value.length
  )
)

const performSearch = async (noteId: number | undefined, term: SearchTerm) => {
  const [literalRes, semanticRes] = await Promise.all([
    noteId
      ? SearchController.searchForLinkTargetWithin({
          path: { note: noteId },
          body: term,
        })
      : SearchController.searchForLinkTarget({
          body: term,
        }),
    noteId
      ? SearchController.semanticSearchWithin({
          path: { note: noteId },
          body: term,
        })
      : SearchController.semanticSearch({
          body: term,
        }),
  ])

  return {
    literal: literalRes.error ? [] : literalRes.data || [],
    semantic: semanticRes.error ? [] : semanticRes.data || [],
  }
}

const debounced = debounce((callback) => callback(), 1000)

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
    const { literal, semantic } = await performSearch(
      props.noteId,
      searchTerm.value
    )
    model.mergeAndCacheResults(
      trimmedSearchKey.value,
      isGlobalSearch.value,
      literal,
      semantic
    )
    model.completeSearch()
  })
}

watch(
  () => searchTerm.value,
  () => {
    if (
      searchTerm.value.allMyCircles &&
      !oldSearchTerm.value.allMyNotebooksAndSubscriptions
    ) {
      searchTerm.value.allMyNotebooksAndSubscriptions = true
    } else if (
      !searchTerm.value.allMyNotebooksAndSubscriptions &&
      oldSearchTerm.value.allMyCircles
    ) {
      searchTerm.value.allMyCircles = false
    }

    if (trimmedSearchKey.value !== "") {
      search()
    } else if (isGlobalSearch.value || props.noteId) {
      fetchRecentNotes()
    }
    oldSearchTerm.value = { ...searchTerm.value }
  },
  { deep: true }
)

watch(
  () => props.inputSearchKey,
  () => {
    searchTerm.value.searchKey = props.inputSearchKey
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

onMounted(() => {
  searchTerm.value.allMyNotebooksAndSubscriptions = true
  searchTerm.value.searchKey = props.inputSearchKey
  // Note: fetchRecentNotes() is called by the watch on searchTerm.value
  // when the search key is empty and noteId is set, so we don't need to call it here
})

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

.dropdown-cards {
  max-height: 200px;
  padding: 0.5rem;
}

.dropdown-cards :deep(.card) {
  margin-bottom: 0.5rem;
  cursor: pointer;
}

.dropdown-cards :deep(.card:hover) {
  background-color: #f8f9fa;
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
  display: inline-block;
  animation: pulse 1.5s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}
</style>
