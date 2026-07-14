<template>
  <div
    class="daisy-join shrink-0"
    role="group"
    aria-label="Search list mode"
  >
    <button
      type="button"
      data-testid="search-list-mode-matches"
      :class="buttonClass('matches')"
      @click="$emit('update:modelValue', 'matches')"
    >
      Matches
    </button>
    <button
      type="button"
      data-testid="search-list-mode-recent"
      :class="buttonClass('recent')"
      @click="$emit('update:modelValue', 'recent')"
    >
      Recent
    </button>
  </div>
</template>

<script setup lang="ts">
import type { SearchListPreference } from "@/models/searchListPreference"

const props = defineProps<{
  modelValue: SearchListPreference
  activeMode: "matches" | "recent"
}>()

defineEmits<{
  "update:modelValue": [value: SearchListPreference]
}>()

function buttonClass(mode: "matches" | "recent") {
  return [
    "daisy-btn daisy-btn-outline daisy-btn-xs daisy-join-item",
    { "bg-primary border-primary": props.activeMode === mode },
  ]
}
</script>
