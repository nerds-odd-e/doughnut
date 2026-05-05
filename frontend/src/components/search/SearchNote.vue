<template>
  <div>
    <TextInput
      scope-name="searchTerm"
      field="searchKey"
      v-model="inputSearchKey"
      placeholder="Search"
      v-focus
    />
    <SearchResults v-bind="{ noteId, inputSearchKey, notebookId }">
      <template v-if="noteId" #button="{ noteTopology }">
        <button
          class="daisy-btn daisy-btn-primary daisy-btn-sm"
          @click.prevent="emit('selected', noteTopology)"
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
import TextInput from "../form/TextInput.vue"
import SearchResults from "./SearchResults.vue"
import type { NoteTopology } from "@generated/doughnut-backend-api"

defineProps<{
  noteId?: number
  notebookId?: number
}>()

const emit = defineEmits<{
  (e: "selected", noteTopology: NoteTopology): void
  (e: "moveUnderFolder", folderId: number): void
}>()

const inputSearchKey = ref("")
</script>
