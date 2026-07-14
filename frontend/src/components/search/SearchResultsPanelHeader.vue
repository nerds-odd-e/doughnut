<template>
  <div
    class="result-section-info-row flex flex-nowrap items-center gap-2 w-full min-w-0"
  >
    <button
      v-if="embedSemanticToggle"
      type="button"
      title="Semantic search"
      aria-label="Semantic search"
      data-testid="note-new-form-semantic-search-toggle"
      :class="[
        'daisy-btn daisy-btn-ghost daisy-btn-sm daisy-btn-square shrink-0',
        semanticSearchEnabled ? 'text-primary' : 'opacity-30',
      ]"
      @click="semanticSearchEnabled = !semanticSearchEnabled"
    >
      <Sparkles class="w-6 h-6" />
    </button>
    <SearchListModeToggle
      v-if="showListModeToggle"
      v-model="listPreference"
      :active-mode="activeListMode"
    />
    <span
      v-if="title"
      class="result-section-info shrink-0 text-sm font-normal text-base-content/70"
    >
      {{ title }}
    </span>
    <span
      v-if="isSearchInProgress"
      class="searching-indicator searching-indicator--title-inline inline-flex shrink-0 items-center"
      role="status"
      aria-busy="true"
    >
      <span class="daisy-loading daisy-loading-spinner daisy-loading-xs" />
    </span>
  </div>
</template>

<script setup lang="ts">
import { Sparkles } from "@lucide/vue"
import SearchListModeToggle from "./SearchListModeToggle.vue"
import type { SearchListPreference } from "@/models/searchListPreference"

defineProps<{
  embedSemanticToggle: boolean
  showListModeToggle: boolean
  activeListMode: "matches" | "recent"
  title: string | null
  isSearchInProgress: boolean
}>()

const semanticSearchEnabled = defineModel<boolean>("semanticSearchEnabled", {
  required: true,
})
const listPreference = defineModel<SearchListPreference>("listPreference", {
  required: true,
})
</script>

<style scoped>
.result-section-info-row {
  padding: 0.5rem 0.5rem 0;
  margin-bottom: 0.5rem;
}

.result-section-info {
  padding: 0;
  line-height: 1.25;
}

.searching-indicator--title-inline {
  margin-left: 0.125rem;
  line-height: 1;
}

.searching-indicator--title-inline .daisy-loading {
  width: 0.75rem;
  height: 0.75rem;
}
</style>
