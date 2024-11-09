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
    <Cards
      v-else
      class="search-result"
      :class="{ 'dropdown-cards': isDropdown }"
      :note-topics="searchResult"
      :columns="isDropdown ? 1 : 3"
    >
      <template #button="{ noteTopic }">
        <slot name="button" :note-topic="noteTopic" />
      </template>
    </Cards>
  </div>
</template>

<script lang="ts">
import type { SearchTerm } from "@/generated/backend"
import { NoteTopic } from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import { debounce } from "mini-debounce"
import { defineComponent } from "vue"
import CheckInput from "../form/CheckInput.vue"
import Cards from "../notes/Cards.vue"

const debounced = debounce((callback) => callback(), 500)

export default defineComponent({
  setup() {
    return useLoadingApi()
  },
  name: "SearchNote",
  props: {
    noteId: Number,
    inputSearchKey: { type: String, required: true },
    isDropdown: { type: Boolean, default: false },
  },
  components: { CheckInput, Cards },
  data() {
    return {
      searchTerm: {
        searchKey: "",
        allMyNotebooksAndSubscriptions: false,
        allMyCircles: false,
      } as SearchTerm,
      oldSearchTerm: {
        searchKey: "",
        allMyNotebooksAndSubscriptions: false,
        allMyCircles: false,
      } as SearchTerm,
      cache: {
        global: {},
        local: {},
      } as {
        global: Record<string, NoteTopic[]>
        local: Record<string, NoteTopic[]>
      },
      recentResult: undefined as NoteTopic[] | undefined,
      timeoutId: null as unknown as ReturnType<typeof setTimeout>,
    }
  },
  watch: {
    searchTerm: {
      handler() {
        if (
          this.searchTerm.allMyCircles &&
          !this.oldSearchTerm.allMyNotebooksAndSubscriptions
        ) {
          this.searchTerm.allMyNotebooksAndSubscriptions = true
        } else if (
          !this.searchTerm.allMyNotebooksAndSubscriptions &&
          this.oldSearchTerm.allMyCircles
        ) {
          this.searchTerm.allMyCircles = false
        }
        if (this.searchTerm.searchKey.trim() !== "") {
          this.search()
        }
        this.oldSearchTerm = { ...this.searchTerm }
      },
      deep: true,
    },
    inputSearchKey() {
      this.searchTerm.searchKey = this.inputSearchKey
    },
  },
  computed: {
    trimmedSearchKey() {
      return this.searchTerm.searchKey.trim()
    },
    cachedSearches() {
      return this.searchTerm.allMyNotebooksAndSubscriptions
        ? this.cache.global
        : this.cache.local
    },
    cachedResult() {
      return this.cachedSearches[this.trimmedSearchKey]
    },
    searchResult() {
      return this.cachedResult ? this.cachedResult : this.recentResult
    },
  },
  methods: {
    async relativeSearch(
      noteId: undefined | Doughnut.ID,
      searchTerm: SearchTerm
    ) {
      if (noteId) {
        return this.managedApi.restNoteController.searchForLinkTargetWithin(
          noteId,
          searchTerm
        )
      }
      return this.managedApi.restNoteController.searchForLinkTarget(searchTerm)
    },

    search() {
      if (
        Object.prototype.hasOwnProperty.call(
          this.cachedSearches,
          "trimmedSearchKey"
        )
      ) {
        return
      }

      this.timeoutId = debounced(async () => {
        const originalTrimmedKey = this.trimmedSearchKey
        const result = await this.relativeSearch(this.noteId, this.searchTerm)
        this.recentResult = result
        this.cachedSearches[originalTrimmedKey] = result
      })
    },
  },
  mounted() {
    if (!this.noteId) {
      this.searchTerm.allMyNotebooksAndSubscriptions = true
    }
    this.searchTerm.searchKey = this.inputSearchKey
  },
  beforeUnmount() {
    clearTimeout(this.timeoutId)
  },
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
</style>
