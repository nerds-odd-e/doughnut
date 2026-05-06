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
      />
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
      v-bind="{ noteId, inputSearchKey, notebookId }"
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
    </SearchResults>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue"
import { BookOpen, Users, X } from "lucide-vue-next"
import TextInput from "../form/TextInput.vue"
import SearchResults from "./SearchResults.vue"
import type { NoteSearchResult } from "@generated/doughnut-backend-api"

defineProps<{
  noteId?: number
  notebookId?: number
  /** When set, shows a square close control after the search scope toggles. */
  modalCloser?: () => void
}>()

const emit = defineEmits<{
  (e: "selected", noteSearchResult: NoteSearchResult): void
  (e: "moveUnderFolder", folderId: number): void
}>()

const inputSearchKey = ref("")
const allMyNotebooksAndSubscriptions = ref(true)
const allMyCircles = ref(false)
</script>
