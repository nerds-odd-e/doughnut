<template>
  <div>
    <TextInput
      scope-name="searchTerm"
      field="searchKey"
      v-model="inputSearchKey"
      placeholder="Search"
      v-focus
    />
    <SearchResults v-bind="{ noteId, inputSearchKey }">
      <template v-if="noteId" #button="{ noteTopology }">
        <div class="daisy-join daisy-join-horizontal">
          <button
            class="daisy-btn daisy-btn-primary daisy-btn-sm daisy-join-item"
            @click.prevent="emit('selected', noteTopology)"
          >
            Link
          </button>
          <button
            class="daisy-btn daisy-btn-secondary daisy-btn-sm daisy-join-item"
            @click.prevent="emit('moveUnder', noteTopology)"
          >
            Move Under
          </button>
        </div>
      </template>
    </SearchResults>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue"
import TextInput from "../form/TextInput.vue"
import SearchResults from "./SearchResults.vue"
import type { NoteTopology } from "generated/backend"

defineProps<{
  noteId?: number
}>()

const emit = defineEmits<{
  (e: "selected", noteTopology: NoteTopology): void
  (e: "moveUnder", noteTopology: NoteTopology): void
}>()

const inputSearchKey = ref("")
</script>
