<template>
  <div>
    <div class="flex items-center gap-2 w-full">
      <TextInput
        class="flex-1 min-w-0"
        scope-name="searchTerm"
        field="searchKey"
        v-model="inputSearchKey"
        placeholder="Search"
        hide-label
        v-focus
      >
        <template #input_prepend>
          <AutoCollapseDropdown
            v-slot="{ closeDropdown }"
            class="search-key-history-details daisy-dropdown daisy-dropdown-start daisy-dropdown-bottom relative inline-flex shrink-0"
            data-testid="search-key-history-dropdown"
            @toggle="onHistoryToggle"
          >
            <summary
              tabindex="-1"
              class="daisy-input flex w-12 min-w-12 max-w-12 shrink-0 flex-none items-center justify-center rounded-r-none px-0 py-0 list-none cursor-pointer bg-base-100"
              title="Search history"
              aria-label="Search history"
              data-testid="search-key-history-trigger"
              @focus="(ev: FocusEvent) => (ev.currentTarget as HTMLElement).blur()"
            >
              <Clock class="w-5 h-5" />
            </summary>
            <ul
              tabindex="0"
              class="daisy-dropdown-content daisy-menu bg-base-100 rounded-box min-w-[12rem] max-w-[20rem] max-h-60 overflow-y-auto p-2 shadow z-[1000]"
            >
              <li v-if="historyKeys.length === 0" class="px-2 py-1 text-sm opacity-60">
                No search history yet
              </li>
              <li
                v-for="(key, index) in historyKeys"
                :key="`${index}-${key}`"
                class="daisy-menu-item p-0"
              >
                <button
                  type="button"
                  class="daisy-btn daisy-btn-ghost h-auto min-h-0 w-full justify-start py-2 font-normal text-left whitespace-normal break-words"
                  :title="key"
                  :data-testid="`search-key-history-item-${index}`"
                  @click="pickHistoryKey(key, closeDropdown)"
                >
                  {{ key }}
                </button>
              </li>
            </ul>
          </AutoCollapseDropdown>
        </template>
      </TextInput>
      <button
        type="button"
        title="All My Notebooks And Subscriptions"
        aria-label="All My Notebooks And Subscriptions"
        :disabled="!noteId"
        :class="[
          'daisy-btn daisy-btn-ghost daisy-btn-sm daisy-btn-square',
          allMyNotebooksAndSubscriptions
            ? 'text-primary'
            : 'opacity-30',
        ]"
        @click="allMyNotebooksAndSubscriptions = !allMyNotebooksAndSubscriptions"
      >
        <BookOpen class="w-6 h-6" />
      </button>
      <button
        type="button"
        title="All My Circles"
        aria-label="All My Circles"
        :class="[
          'daisy-btn daisy-btn-ghost daisy-btn-sm daisy-btn-square',
          allMyCircles ? 'text-primary' : 'opacity-30',
        ]"
        @click="allMyCircles = !allMyCircles"
      >
        <Users class="w-6 h-6" />
      </button>
      <button
        type="button"
        title="Semantic search"
        aria-label="Semantic search"
        :class="[
          'daisy-btn daisy-btn-ghost daisy-btn-sm daisy-btn-square',
          semanticSearchEnabled ? 'text-primary' : 'opacity-30',
        ]"
        @click="semanticSearchEnabled = !semanticSearchEnabled"
      >
        <Sparkles class="w-6 h-6" />
      </button>
      <button
        v-if="modalCloser"
        type="button"
        title="Close"
        aria-label="Close"
        class="daisy-btn daisy-btn-ghost daisy-btn-sm daisy-btn-square"
        @click="modalCloser()"
      >
        <X class="w-6 h-6" />
      </button>
    </div>
    <SearchResults
      v-model:semantic-search-enabled="semanticSearchEnabled"
      v-bind="{
        noteId,
        inputSearchKey,
        notebookId,
      }"
      v-model:all-my-notebooks-and-subscriptions="allMyNotebooksAndSubscriptions"
      v-model:all-my-circles="allMyCircles"
    >
      <template v-if="noteId" #button="{ noteSearchResult }">
        <button
          class="daisy-btn daisy-btn-primary daisy-btn-sm"
          @click.prevent="emit('selected', noteSearchResult)"
        >
          Add link
        </button>
      </template>
      <template
        v-if="noteId"
        #folderButton="{ folderId: targetFolderId }"
      >
        <button
          class="daisy-btn daisy-btn-secondary daisy-btn-sm"
          @click.prevent="emit('moveUnderFolder', targetFolderId)"
        >
          Move Under
        </button>
      </template>
      <template
        v-if="noteId"
        #notebookButton="{ notebookId: targetNotebookId }"
      >
        <button
          class="daisy-btn daisy-btn-secondary daisy-btn-sm"
          @click.prevent="emit('moveToNotebookRoot', targetNotebookId)"
        >
          Move to notebook root
        </button>
      </template>
    </SearchResults>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue"
import { BookOpen, Clock, Sparkles, Users, X } from "@lucide/vue"
import TextInput from "../form/TextInput.vue"
import SearchResults from "./SearchResults.vue"
import type { NoteSearchResult } from "@generated/doughnut-backend-api"
import { readSearchKeyHistory } from "@/utils/searchKeyHistoryCookie"
import AutoCollapseDropdown from "@/components/commons/AutoCollapseDropdown.vue"

const { initialSearchKey } = defineProps<{
  noteId?: number
  notebookId?: number
  /** When set, shows a square close control after the search scope toggles. */
  modalCloser?: () => void
  /** Pre-fills the search field (e.g. dead wiki link display text). */
  initialSearchKey?: string
}>()

const emit = defineEmits<{
  (e: "selected", noteSearchResult: NoteSearchResult): void
  (e: "moveUnderFolder", folderId: number): void
  (e: "moveToNotebookRoot", targetNotebookId: number): void
}>()

const inputSearchKey = ref(initialSearchKey ?? "")
const allMyNotebooksAndSubscriptions = ref(true)
const allMyCircles = ref(false)
const semanticSearchEnabled = ref(false)

const historyKeys = ref<string[]>([])

function onHistoryToggle(event: Event) {
  const details = event.target as HTMLDetailsElement
  if (details.open) {
    historyKeys.value = readSearchKeyHistory()
  }
}

function pickHistoryKey(key: string, closeDropdown: () => void) {
  inputSearchKey.value = key
  closeDropdown()
}
</script>

<style scoped>
.search-key-history-details > summary::-webkit-details-marker {
  display: none;
}
</style>
