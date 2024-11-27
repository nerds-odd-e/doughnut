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
      <template #button="{ noteTopic }">
        <div class="btn-group">
          <button
            class="btn btn-primary"
            @click.prevent="emit('selected', noteTopic)"
          >
            Link
          </button>
          <button
            class="btn btn-sm btn-secondary"
            @click.prevent="emit('moveUnder', noteTopic)"
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
import type { NoteTopic } from "@/generated/backend"

defineProps<{
  noteId?: number
}>()

const emit = defineEmits<{
  (e: "selected", noteTopic: NoteTopic): void
  (e: "moveUnder", noteTopic: NoteTopic): void
}>()

const inputSearchKey = ref("")
</script>
