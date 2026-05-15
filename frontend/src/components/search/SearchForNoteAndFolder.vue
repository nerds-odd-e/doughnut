<template>
  <div>
    <div class="daisy-flex daisy-items-center daisy-gap-2 daisy-w-full">
      <TextInput
        class="daisy-flex-1 daisy-min-w-0"
        scope-name="searchTerm"
        field="searchKey"
        v-model="inputSearchKey"
        placeholder="Search"
        hide-label
        v-focus
      >
        <template #input_prepend>
          <details
            ref="historyDetailsRef"
            class="search-key-history-details daisy-dropdown daisy-dropdown-start daisy-dropdown-bottom daisy-relative daisy-inline-flex daisy-shrink-0"
            data-testid="search-key-history-dropdown"
            @toggle="onHistoryToggle"
          >
            <summary
              class="daisy-input daisy-input-bordered daisy-flex daisy-w-12 daisy-min-w-12 daisy-max-w-12 daisy-shrink-0 daisy-flex-none daisy-items-center daisy-justify-center daisy-rounded-r-none daisy-px-0 daisy-py-0 list-none daisy-cursor-pointer daisy-bg-base-100"
              title="Search history"
              aria-label="Search history"
              data-testid="search-key-history-trigger"
            >
              <Clock class="daisy-w-5 daisy-h-5" />
            </summary>
            <ul
              tabindex="0"
              class="daisy-dropdown-content daisy-menu daisy-bg-base-100 daisy-rounded-box daisy-min-w-[12rem] daisy-max-w-[20rem] daisy-max-h-60 daisy-overflow-y-auto daisy-p-2 daisy-shadow daisy-z-[1000]"
            >
              <li v-if="historyKeys.length === 0" class="daisy-px-2 daisy-py-1 daisy-text-sm daisy-opacity-60">
                No search history yet
              </li>
              <li
                v-for="(key, index) in historyKeys"
                :key="`${index}-${key}`"
                class="daisy-menu-item daisy-p-0"
              >
                <button
                  type="button"
                  class="daisy-btn daisy-btn-ghost daisy-h-auto daisy-min-h-0 daisy-w-full daisy-justify-start daisy-py-2 daisy-font-normal daisy-text-left daisy-whitespace-normal daisy-break-words"
                  :title="key"
                  :data-testid="`search-key-history-item-${index}`"
                  @click="pickHistoryKey(key)"
                >
                  {{ key }}
                </button>
              </li>
            </ul>
          </details>
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
            ? 'daisy-text-primary'
            : 'daisy-opacity-30',
        ]"
        @click="allMyNotebooksAndSubscriptions = !allMyNotebooksAndSubscriptions"
      >
        <BookOpen class="daisy-w-6 daisy-h-6" />
      </button>
      <button
        type="button"
        title="All My Circles"
        aria-label="All My Circles"
        :class="[
          'daisy-btn daisy-btn-ghost daisy-btn-sm daisy-btn-square',
          allMyCircles ? 'daisy-text-primary' : 'daisy-opacity-30',
        ]"
        @click="allMyCircles = !allMyCircles"
      >
        <Users class="daisy-w-6 daisy-h-6" />
      </button>
      <button
        type="button"
        title="Semantic search"
        aria-label="Semantic search"
        :class="[
          'daisy-btn daisy-btn-ghost daisy-btn-sm daisy-btn-square',
          semanticSearchEnabled ? 'daisy-text-primary' : 'daisy-opacity-30',
        ]"
        @click="semanticSearchEnabled = !semanticSearchEnabled"
      >
        <Sparkles class="daisy-w-6 daisy-h-6" />
      </button>
      <button
        v-if="modalCloser"
        type="button"
        title="Close"
        aria-label="Close"
        class="daisy-btn daisy-btn-ghost daisy-btn-sm daisy-btn-square"
        @click="modalCloser()"
      >
        <X class="daisy-w-6 daisy-h-6" />
      </button>
    </div>
    <SearchResults
      v-bind="{
        noteId,
        inputSearchKey,
        notebookId,
        semanticSearchEnabled,
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
import { BookOpen, Clock, Sparkles, Users, X } from "lucide-vue-next"
import TextInput from "../form/TextInput.vue"
import SearchResults from "./SearchResults.vue"
import type { NoteSearchResult } from "@generated/doughnut-backend-api"
import { readSearchKeyHistory } from "@/utils/searchKeyHistoryCookie"

defineProps<{
  noteId?: number
  notebookId?: number
  /** When set, shows a square close control after the search scope toggles. */
  modalCloser?: () => void
}>()

const emit = defineEmits<{
  (e: "selected", noteSearchResult: NoteSearchResult): void
  (e: "moveUnderFolder", folderId: number): void
  (e: "moveToNotebookRoot", targetNotebookId: number): void
}>()

const inputSearchKey = ref("")
const allMyNotebooksAndSubscriptions = ref(true)
const allMyCircles = ref(false)
const semanticSearchEnabled = ref(false)

const historyDetailsRef = ref<HTMLDetailsElement | null>(null)
const historyKeys = ref<string[]>([])

function onHistoryToggle() {
  if (historyDetailsRef.value?.open) {
    historyKeys.value = readSearchKeyHistory()
  }
}

function pickHistoryKey(key: string) {
  inputSearchKey.value = key
  if (historyDetailsRef.value) {
    historyDetailsRef.value.open = false
  }
}
</script>

<style scoped>
.search-key-history-details > summary::-webkit-details-marker {
  display: none;
}
</style>
