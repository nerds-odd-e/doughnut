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

    <div v-if="searchResult === undefined">
      <em>Searching ...</em>
    </div>

    <div v-else-if="searchResult.length === 0 && isDropdown" class="dropdown-list">
      <em>Similar notes within the same notebook</em>
    </div>

    <div v-else-if="searchResult.length === 0">
      <em>No matching notes found.</em>
    </div>

    <div v-else-if="isDropdown" class="dropdown-list">
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
</template>

<script setup lang="ts">
import type { SearchTerm } from "@generated/backend"
import type { NoteTopology } from "@generated/backend"
import type { NoteSearchResult } from "@generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import { debounce } from "mini-debounce"
import { ref, computed, watch, onMounted, onBeforeUnmount } from "vue"
import CheckInput from "../form/CheckInput.vue"
import Cards from "../notes/Cards.vue"
import NoteTitleWithLink from "../notes/NoteTitleWithLink.vue"

// Props definition
const props = defineProps({
  noteId: Number,
  inputSearchKey: { type: String, required: true },
  isDropdown: { type: Boolean, default: false },
})

// Emit slot for template
defineSlots<{
  button: (props: { noteTopology: NoteTopology }) => void
}>()

const { managedApi } = useLoadingApi()

// Data properties
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

const cache = ref<{
  global: Record<string, NoteSearchResult[]>
  local: Record<string, NoteSearchResult[]>
}>({
  global: {},
  local: {},
})

const recentResult = ref<NoteSearchResult[] | undefined>()
const timeoutId = ref<ReturnType<typeof setTimeout>>()

// Computed properties
const trimmedSearchKey = computed(() => searchTerm.value.searchKey.trim())

const cachedSearches = computed(() =>
  searchTerm.value.allMyNotebooksAndSubscriptions
    ? cache.value.global
    : cache.value.local
)

const cachedResult = computed(
  () => cachedSearches.value[trimmedSearchKey.value]
)

const searchResult = computed(() => {
  const raw = cachedResult.value ? cachedResult.value : recentResult.value
  if (!raw) return undefined
  // Support both new API (NoteSearchResult) and legacy (NoteTopology) shapes
  return (raw as Array<NoteSearchResult | NoteTopology>).map((r) =>
    "noteTopology" in (r as NoteSearchResult)
      ? (r as NoteSearchResult).noteTopology
      : (r as NoteTopology)
  ) as NoteTopology[]
})

const distanceById = computed<Record<string, number>>(() => {
  const map: Record<string, number> = {}
  const list = cachedResult.value ?? recentResult.value ?? []
  list.forEach((r) => {
    const id = String((r as NoteSearchResult).noteTopology.id as number)
    const d = (r as NoteSearchResult).distance
    if (d != null) map[id] = d
  })
  return map
})

// Methods
const relativeSearch = async (
  noteId: undefined | Doughnut.ID,
  searchTerm: SearchTerm
) => {
  if (noteId) {
    return managedApi.services.searchForLinkTargetWithin({
      note: noteId,
      requestBody: searchTerm,
    })
  }
  return managedApi.services.searchForLinkTarget({ requestBody: searchTerm })
}

const semanticRelativeSearch = async (
  noteId: undefined | Doughnut.ID,
  searchTerm: SearchTerm
) => {
  if (noteId) {
    return managedApi.services.semanticSearchWithin({
      note: noteId,
      requestBody: searchTerm,
    })
  }
  return managedApi.services.semanticSearch({ requestBody: searchTerm })
}

const debounced = debounce((callback) => callback(), 500)

const mergeUniqueAndSortByDistance = (
  existing: NoteSearchResult[],
  incoming: NoteSearchResult[]
): NoteSearchResult[] => {
  const byId = new Map<number, NoteSearchResult>()
  const getId = (r: NoteSearchResult) => r.noteTopology.id as number

  const chooseBetter = (a: NoteSearchResult, b: NoteSearchResult) => {
    const da = a.distance ?? Infinity
    const db = b.distance ?? Infinity
    return db < da ? b : a
  }

  existing.forEach((r) => {
    byId.set(getId(r), r)
  })
  incoming.forEach((r) => {
    const id = getId(r)
    const prev = byId.get(id)
    byId.set(id, prev ? chooseBetter(prev, r) : r)
  })

  return Array.from(byId.values()).sort(
    (a, b) => (a.distance ?? Infinity) - (b.distance ?? Infinity)
  )
}

const search = () => {
  timeoutId.value = debounced(async () => {
    const originalTrimmedKey = trimmedSearchKey.value
    // perform literal and semantic searches in parallel
    const [literalRes, semanticRes] = await Promise.all([
      relativeSearch(props.noteId, searchTerm.value),
      semanticRelativeSearch(props.noteId, searchTerm.value),
    ])
    const combined = [...literalRes, ...semanticRes]
    const existing = cachedSearches.value[originalTrimmedKey] ?? []
    const merged = mergeUniqueAndSortByDistance(existing, combined)
    cachedSearches.value[originalTrimmedKey] = merged
    recentResult.value = cachedSearches.value[originalTrimmedKey]
  })
}

// Watchers
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
    if (searchTerm.value.searchKey.trim() !== "") {
      search()
    }
    oldSearchTerm.value = { ...searchTerm.value }
  },
  { deep: true }
)

watch(
  () => props.inputSearchKey,
  () => {
    searchTerm.value.searchKey = props.inputSearchKey
    // When search key is empty and we have a noteId (dropdown mode in notebook context),
    // set recentResult to empty array to show "Similar notes within the same notebook"
    // instead of "Searching ..."
    if (
      props.inputSearchKey.trim() === "" &&
      props.noteId &&
      props.isDropdown
    ) {
      recentResult.value = []
    }
  }
)

// Lifecycle hooks
onMounted(() => {
  if (!props.noteId) {
    searchTerm.value.allMyNotebooksAndSubscriptions = true
  }
  searchTerm.value.searchKey = props.inputSearchKey
  // When search key is empty and we have a noteId (dropdown mode in notebook context),
  // set recentResult to empty array to show "Similar notes within the same notebook"
  // instead of "Searching ..."
  if (props.inputSearchKey.trim() === "" && props.noteId && props.isDropdown) {
    recentResult.value = []
  }
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
</style>
