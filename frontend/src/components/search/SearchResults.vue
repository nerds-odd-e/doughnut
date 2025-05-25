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

    <div v-if="!searchResult || searchResult.length === 0">
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
      </template>
    </Cards>
  </div>
</template>

<script setup lang="ts">
import type { SearchTerm } from "generated/backend"
import { NoteTopology } from "generated/backend"
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
  global: Record<string, NoteTopology[]>
  local: Record<string, NoteTopology[]>
}>({
  global: {},
  local: {},
})

const recentResult = ref<NoteTopology[] | undefined>()
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

const searchResult = computed(() =>
  cachedResult.value ? cachedResult.value : recentResult.value
)

// Methods
const relativeSearch = async (
  noteId: undefined | Doughnut.ID,
  searchTerm: SearchTerm
) => {
  if (noteId) {
    return managedApi.restNoteController.searchForLinkTargetWithin(
      noteId,
      searchTerm
    )
  }
  return managedApi.restNoteController.searchForLinkTarget(searchTerm)
}

const debounced = debounce((callback) => callback(), 500)

const search = () => {
  if (
    Object.prototype.hasOwnProperty.call(
      cachedSearches.value,
      "trimmedSearchKey"
    )
  ) {
    return
  }

  timeoutId.value = debounced(async () => {
    const originalTrimmedKey = trimmedSearchKey.value
    const result = await relativeSearch(props.noteId, searchTerm.value)
    recentResult.value = result
    cachedSearches.value[originalTrimmedKey] = result
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
  }
)

// Lifecycle hooks
onMounted(() => {
  if (!props.noteId) {
    searchTerm.value.allMyNotebooksAndSubscriptions = true
  }
  searchTerm.value.searchKey = props.inputSearchKey
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
